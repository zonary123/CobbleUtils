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

public class CycloneAnimation extends Animation {

  @Override
  public void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                    List<ItemStack> allRewards, Runnable onComplete) {
    int total = obtained.size();
    if (total == 0) {
      if (onComplete != null) onComplete.run();
      return;
    }

    Vec3d basePos = player.getPos();

    CobbleUtils.server.executeSync(() -> {
      CycloneController controller = new CycloneController(
        player.getServerWorld(), basePos.x, basePos.y, basePos.z,
        obtained, player, onComplete
      );
      player.getServerWorld().spawnEntity(controller);
    });
  }

  public static class CycloneController extends CustomArmorStandEntity {
    private final ServerPlayerEntity player;
    private final List<ItemStack> rewards;
    private final Runnable onDestroy;
    private boolean completed = false;
    private final Vec3d basePos;

    private final List<CustomItemDisplayEntity> displays = new ArrayList<>();
    private final List<Double> initialAngles = new ArrayList<>();

    public CycloneController(World world, double x, double y, double z,
                             List<ItemStack> rewards, ServerPlayerEntity player, Runnable onDestroy) {
      super(world, x, y, z);
      this.player = player;
      this.rewards = rewards;
      this.onDestroy = onDestroy;
      this.basePos = new Vec3d(x, y, z);

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

        CustomItemDisplayEntity display = AnimationUtils.spawnItemDisplay(
          sw, basePos, reward.copy(), new Vector3f(1.0f, 1.0f, 1.0f), yaw, 0
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

      if (ticks < 70) {
        double progress = ticks / 70.0;
        double radius = Math.max(0.2, 2.5 * (1.0 - progress));

        for (int i = 0; i < displays.size(); i++) {
          CustomItemDisplayEntity display = displays.get(i);
          if (display == null) continue;

          double angle = initialAngles.get(i) + Math.toRadians(ticks * 18.0);
          double targetY = basePos.y + (progress * 2.5);

          double targetX = player.getX() + radius * Math.cos(angle);
          double targetZ = player.getZ() + radius * Math.sin(angle);
          Vec3d targetPos = new Vec3d(targetX, targetY, targetZ);

          float yaw = AnimationUtils.getYawToFacePlayer(player, targetPos);
          // Add a beautiful 3D spin on multiple axes
          float spinPitch = ticks * 6.0f;
          Quaternionf rotation = new Quaternionf().rotationYXZ((float) Math.toRadians(-yaw), (float) Math.toRadians(spinPitch), 0);

          AnimationUtils.updateDisplayTransformation(
            display, targetPos, rotation, new Vector3f(1.0f, 1.0f, 1.0f), 2
          );

          if (ticks % 2 == 0) {
            sw.spawnParticles(
              ParticleTypes.CLOUD,
              targetX, targetY, targetZ,
              1, 0.05, 0.05, 0.05, 0.0
            );
          }
        }

        if (ticks % 8 == 0) {
          player.playSoundToPlayer(SoundEvents.ENTITY_PHANTOM_BITE, player.getSoundCategory(), 0.3f, 1.6f);
        }
      } else {
        this.kill();
        complete();
      }

      setTicks(ticks + 1);
    }
  }
}
