package com.kingpixel.cobbleutils.Model.Animations.entity;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.Animations.core.Animation;
import com.kingpixel.cobbleutils.Model.Animations.core.AnimationUtils;
import com.kingpixel.cobbleutils.Model.Animations.core.CustomArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class VortexAnimation extends Animation {

  @Override
  public void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                    List<ItemStack> allRewards, Runnable onComplete) {
    int totalRewards = obtained.size();
    if (totalRewards == 0) {
      if (onComplete != null) onComplete.run();
      return;
    }

    CobbleUtils.server.executeSync(() -> {
      VortexController controller = new VortexController(
        player.getServerWorld(), player.getX(), player.getY(), player.getZ(),
        obtained, player, onComplete
      );
      player.getServerWorld().spawnEntity(controller);
    });
  }

  public static class VortexController extends CustomArmorStandEntity {
    private final ServerPlayerEntity player;
    private final List<ItemStack> rewards;
    private final Runnable onDestroy;
    private boolean completed = false;

    private final List<DisplayEntity.ItemDisplayEntity> displays = new ArrayList<>();
    private final List<Double> initialAngles = new ArrayList<>();

    public VortexController(World world, double x, double y, double z,
                            List<ItemStack> rewards, ServerPlayerEntity player, Runnable onDestroy) {
      super(world, x, y, z);
      this.player = player;
      this.rewards = rewards;
      this.onDestroy = onDestroy;

      setNoGravity(true);
      setInvisible(true);
      setInvulnerable(true);

      ServerWorld sw = (ServerWorld) world;
      int size = rewards.size();
      for (int i = 0; i < size; i++) {
        ItemStack reward = rewards.get(i);
        if (reward == null) continue;

        double angle = Math.toRadians((360.0 / size) * i);
        float yaw = player.getYaw() + 180f;

        DisplayEntity.ItemDisplayEntity display = AnimationUtils.spawnItemDisplay(
          sw, player.getPos(), reward.copy(), new Vector3f(1.0f, 1.0f, 1.0f), yaw, 0
        );

        displays.add(display);
        initialAngles.add(angle);
      }
    }

    private void complete() {
      if (!completed) {
        completed = true;
        for (DisplayEntity.ItemDisplayEntity display : displays) {
          if (display != null) display.discard();
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
      Vec3d playerPos = player.getPos();

      if (ticks < 80) {
        double progress = (double) ticks / 80.0;
        double radius = Math.max(0.1, 4.0 * (1.0 - progress));

        for (int i = 0; i < displays.size(); i++) {
          DisplayEntity.ItemDisplayEntity display = displays.get(i);
          if (display == null) continue;

          double angle = initialAngles.get(i) + Math.toRadians(ticks * (6.0 + progress * 24.0));
          double targetX = playerPos.x + radius * Math.cos(angle);
          double targetY = playerPos.y + 0.3 + (progress * 1.2);
          double targetZ = playerPos.z + radius * Math.sin(angle);
          Vec3d targetPos = new Vec3d(targetX, targetY, targetZ);

          float yaw = AnimationUtils.getYawToFacePlayer(player, targetPos);
          float spinPitch = ticks * 6.0f;
          Quaternionf rotation = new Quaternionf().rotationYXZ((float) Math.toRadians(-yaw), (float) Math.toRadians(spinPitch), 0);

          AnimationUtils.updateDisplayTransformation(
            display, targetPos, rotation, new Vector3f(1.0f, 1.0f, 1.0f), 2
          );

          sw.spawnParticles(
            ParticleTypes.PORTAL,
            targetX, targetY, targetZ,
            2, 0.05, 0.05, 0.05, 0.0
          );
        }
      } else {
        player.playSoundToPlayer(SoundEvents.ENTITY_ENDERMAN_TELEPORT, player.getSoundCategory(), 0.5f, 1.4f);
        this.kill();
        complete();
      }

      setTicks(ticks + 1);
    }
  }
}
