package com.kingpixel.cobbleutils.Model.Animations.entity;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.Animations.core.Animation;
import com.kingpixel.cobbleutils.Model.Animations.core.AnimationUtils;
import com.kingpixel.cobbleutils.Model.Animations.core.CustomArmorStandEntity;
import com.kingpixel.cobbleutils.Model.Animations.core.CustomItemDisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ShowerAnimation extends Animation {

  @Override
  public void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                    List<ItemStack> allRewards, Runnable onComplete) {
    int totalRewards = obtained.size();
    if (totalRewards == 0) {
      if (onComplete != null) onComplete.run();
      return;
    }

    Vec3d centerPosition = AnimationUtils.getPosition(player, position);

    CobbleUtils.server.executeSync(() -> {
      ShowerController controller = new ShowerController(
        player.getServerWorld(), centerPosition.x, centerPosition.y, centerPosition.z,
        obtained, player, onComplete
      );
      player.getServerWorld().spawnEntity(controller);
    });
  }

  public static class EjectedReward {
    public final CustomItemDisplayEntity display;
    public double x;
    public double y;
    public double z;
    public double velX;
    public double velY;
    public double velZ;
    public final float itemYaw;

    public EjectedReward(CustomItemDisplayEntity display, double x, double y, double z,
                         double velX, double velY, double velZ, float itemYaw) {
      this.display = display;
      this.x = x;
      this.y = y;
      this.z = z;
      this.velX = velX;
      this.velY = velY;
      this.velZ = velZ;
      this.itemYaw = itemYaw;
    }
  }

  public static class ShowerController extends CustomArmorStandEntity {
    private final ServerPlayerEntity player;
    private final List<ItemStack> rewards;
    private final Runnable onDestroy;
    private boolean completed = false;

    private final List<EjectedReward> rewardEntities = new ArrayList<>();

    public ShowerController(World world, double x, double y, double z,
                            List<ItemStack> rewards, ServerPlayerEntity player, Runnable onDestroy) {
      super(world, x, y, z);
      this.player = player;
      this.rewards = rewards;
      this.onDestroy = onDestroy;

      setNoGravity(true);
      setInvisible(true);
      setInvulnerable(true);

      ServerWorld sw = (ServerWorld) world;
      ThreadLocalRandom random = ThreadLocalRandom.current();
      int size = rewards.size();

      for (int i = 0; i < size; i++) {
        ItemStack reward = rewards.get(i);
        if (reward == null) continue;

        double angle = random.nextDouble() * 2 * Math.PI;
        double horizontalSpeed = 0.12 + random.nextDouble() * 0.12;
        double velX = horizontalSpeed * Math.cos(angle);
        double velY = 0.25 + random.nextDouble() * 0.18;
        double velZ = horizontalSpeed * Math.sin(angle);

        float itemYaw = player.getYaw() + 180f;
        CustomItemDisplayEntity display = AnimationUtils.spawnItemDisplay(
          sw, new Vec3d(x, y, z), reward.copy(), new Vector3f(1.0f, 1.0f, 1.0f), itemYaw, 0
        );

        EjectedReward rewardEntity = new EjectedReward(display, x, y, z, velX, velY, velZ, itemYaw);
        rewardEntities.add(rewardEntity);
      }
    }

    @Override public void complete() {
      if (!completed) {
        completed = true;
        for (EjectedReward ent : rewardEntities) {
          if (ent.display != null) {
            ent.display.discard();
          }
        }
        if (onDestroy != null) onDestroy.run();
      }
    }

    @Override
    public void tick() {
      super.tick();

      if (this.player == null || this.player.isRemoved() || !this.isAlive()) {
        this.kill();
        complete();
        return;
      }

      int ticks = getTicks();
      ServerWorld sw = (ServerWorld) this.getWorld();

      if (ticks < 80) {
        for (int i = 0; i < rewardEntities.size(); i++) {
          EjectedReward reward = rewardEntities.get(i);
          if (reward.display == null) continue;

          reward.x += reward.velX;
          reward.y += reward.velY;
          reward.z += reward.velZ;

          reward.velY -= 0.012;
          reward.velX *= 0.97;
          reward.velZ *= 0.97;

          Vec3d targetPos = new Vec3d(reward.x, reward.y, reward.z);
          float yaw = AnimationUtils.getYawToFacePlayer(player, targetPos);
          float spinPitch = ticks * 6.0f;
          Quaternionf rotation = new Quaternionf().rotationYXZ((float) Math.toRadians(-yaw), (float) Math.toRadians(spinPitch), 0);

          AnimationUtils.updateDisplayTransformation(
            reward.display, targetPos, rotation, new Vector3f(1.0f, 1.0f, 1.0f), 2
          );

          sw.spawnParticles(
            ParticleTypes.HAPPY_VILLAGER,
            reward.x, reward.y + 0.5, reward.z,
            1, 0.05, 0.05, 0.05, 0.01
          );
        }
      } else {
        this.kill();
        complete();
      }

      setTicks(ticks + 1);
    }
  }
}
