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
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class OrbitalAnimation extends Animation {

  @Override
  public void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                    List<ItemStack> allRewards, Runnable onComplete) {
    int total = obtained.size();
    if (total == 0) {
      if (onComplete != null) onComplete.run();
      return;
    }

    CobbleUtils.server.executeSync(() -> {
      OrbitalController controller = new OrbitalController(
        player.getServerWorld(), player.getX(), player.getY() + 0.5, player.getZ(),
        obtained, player, onComplete
      );
      player.getServerWorld().spawnEntity(controller);
    });
  }

  public static class OrbitalController extends CustomArmorStandEntity {
    private final ServerPlayerEntity player;
    private final List<ItemStack> rewards;
    private final Runnable onDestroy;
    private boolean completed = false;
    private final double radius = 1.5;

    private final List<CustomItemDisplayEntity> displays = new ArrayList<>();
    private final List<Double> initialAngles = new ArrayList<>();

    public OrbitalController(World world, double x, double y, double z,
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
        double targetX = player.getX() + radius * Math.cos(angle);
        double targetZ = player.getZ() + radius * Math.sin(angle);
        Vec3d itemPos = new Vec3d(targetX, y, targetZ);

        float yaw = AnimationUtils.getYawToFacePlayer(player, itemPos);
        CustomItemDisplayEntity display = AnimationUtils.spawnItemDisplay(
          sw, itemPos, reward.copy(), new Vector3f(1.0f, 1.0f, 1.0f), yaw, 0
        );

        displays.add(display);
        initialAngles.add(angle);
      }
    }

    @Override public void complete() {
      if (!completed) {
        completed = true;
        for (CustomItemDisplayEntity display : displays) {
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

      if (ticks < 60) {
        double targetY = player.getY() + 0.5;

        for (int i = 0; i < displays.size(); i++) {
          CustomItemDisplayEntity display = displays.get(i);
          if (display == null) continue;

          double angle = initialAngles.get(i) + Math.toRadians(ticks * 12.0);
          double targetX = player.getX() + radius * Math.cos(angle);
          double targetZ = player.getZ() + radius * Math.sin(angle);
          Vec3d targetPos = new Vec3d(targetX, targetY, targetZ);

          float yaw = AnimationUtils.getYawToFacePlayer(player, targetPos);
          float spinPitch = ticks * 6.0f;
          Quaternionf rotation = new Quaternionf().rotationYXZ((float) Math.toRadians(-yaw), (float) Math.toRadians(spinPitch), 0);

          AnimationUtils.updateDisplayTransformation(
            display, targetPos, rotation, new Vector3f(1.0f, 1.0f, 1.0f), 2
          );

          if (ticks % 2 == 0) {
            sw.spawnParticles(
              ParticleTypes.END_ROD,
              targetPos.x, targetPos.y + 0.5, targetPos.z,
              1, 0.0, 0.0, 0.0, 0.0
            );
          }
        }

        if (ticks % 8 == 0) {
          player.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, player.getSoundCategory(), 0.3f, 1.2f);
        }
      } else {
        this.kill();
        complete();
      }

      setTicks(ticks + 1);
    }
  }
}
