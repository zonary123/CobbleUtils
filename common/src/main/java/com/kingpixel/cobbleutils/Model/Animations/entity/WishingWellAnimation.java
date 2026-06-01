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

public class WishingWellAnimation extends Animation {

  @Override
  public void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                    List<ItemStack> allRewards, Runnable onComplete) {
    int total = obtained.size();
    if (total == 0) {
      if (onComplete != null) onComplete.run();
      return;
    }

    Vec3d direction = player.getRotationVec(1.0f).normalize();
    Vec3d center = AnimationUtils.getPosition(player, position).add(direction.x * 2.5, 0.0, direction.z * 2.5);

    CobbleUtils.server.executeSync(() -> {
      FountainEntity fountain = new FountainEntity(
        player.getServerWorld(),
        center.x, center.y, center.z,
        obtained, player, onComplete, center.y
      );
      player.getServerWorld().spawnEntity(fountain);
    });
  }

  public static class EjectedReward {
    public final DisplayEntity.ItemDisplayEntity display;
    public double x;
    public double y;
    public double z;
    public final double velX;
    public double velY;
    public final double velZ;
    public final double groundY;
    public boolean landed = false;
    public int ticks = 0;
    public final float itemYaw;

    public EjectedReward(DisplayEntity.ItemDisplayEntity display, double x, double y, double z,
                         double velX, double velY, double velZ, double groundY, float itemYaw) {
      this.display = display;
      this.x = x;
      this.y = y;
      this.z = z;
      this.velX = velX;
      this.velY = velY;
      this.velZ = velZ;
      this.groundY = groundY;
      this.itemYaw = itemYaw;
    }
  }

  public static class FountainEntity extends CustomArmorStandEntity {
    private final ServerPlayerEntity player;
    private final Vec3d basePos;
    private final List<ItemStack> rewards;
    private final Runnable onDestroy;
    private final double groundY;
    private boolean completed = false;
    private int nextRewardIndex = 0;
    private final List<EjectedReward> rewardEntities = new ArrayList<>();

    public FountainEntity(World world, double x, double y, double z, List<ItemStack> rewards, ServerPlayerEntity player, Runnable onDestroy, double groundY) {
      super(world, x, y, z);
      this.player = player;
      this.basePos = new Vec3d(x, y, z);
      this.rewards = rewards;
      this.onDestroy = onDestroy;
      this.groundY = groundY;

      setNoGravity(true);
      setInvisible(true);
      setInvulnerable(true);
    }

    private void complete() {
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

      if (ticks < 15) {
        double buildProgress = ticks / 15.0;
        int splashCount = (int) (3 + buildProgress * 10);
        sw.spawnParticles(
          ParticleTypes.SPLASH,
          basePos.x, basePos.y + 0.1, basePos.z,
          splashCount, 0.3 * buildProgress, 0.05, 0.3 * buildProgress, 0.0
        );
        sw.spawnParticles(
          ParticleTypes.DRIPPING_WATER,
          basePos.x, basePos.y + 0.5 * buildProgress, basePos.z,
          2, 0.2, 0.1, 0.2, 0.0
        );

        if (ticks == 5) {
          player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_SPLASH, player.getSoundCategory(), 0.6f, 1.3f);
        }
        if (ticks == 10) {
          player.playSoundToPlayer(SoundEvents.BLOCK_WATER_AMBIENT, player.getSoundCategory(), 0.8f, 1.0f);
        }
      }

      if (ticks >= 15 && ticks < 120) {
        double fountainHeight = 1.5 + Math.sin(ticks * 0.15) * 0.3;
        sw.spawnParticles(
          ParticleTypes.SPLASH,
          basePos.x, basePos.y + fountainHeight, basePos.z,
          6, 0.15, 0.1, 0.15, 0.05
        );
        sw.spawnParticles(
          ParticleTypes.FALLING_WATER,
          basePos.x, basePos.y + fountainHeight - 0.3, basePos.z,
          3, 0.25, 0.2, 0.25, 0.0
        );
        sw.spawnParticles(
          ParticleTypes.BUBBLE,
          basePos.x, basePos.y + 0.2, basePos.z,
          2, 0.2, 0.05, 0.2, 0.0
        );

        if (ticks % 20 == 0) {
          for (int ring = 0; ring < 8; ring++) {
            double ringAngle = Math.toRadians((360.0 / 8.0) * ring + ticks * 3);
            double rx = basePos.x + Math.cos(ringAngle) * 0.6;
            double rz = basePos.z + Math.sin(ringAngle) * 0.6;
            sw.spawnParticles(
              ParticleTypes.DRIPPING_WATER,
              rx, basePos.y + 0.3, rz,
              1, 0.0, 0.0, 0.0, 0.0
            );
          }
        }

        if (ticks % 15 == 0) {
          player.playSoundToPlayer(SoundEvents.BLOCK_WATER_AMBIENT, player.getSoundCategory(), 0.4f, 1.2f);
        }
      }

      if (ticks >= 20 && nextRewardIndex < rewards.size()) {
        int interval = Math.max(3, 40 / rewards.size());
        if ((ticks - 20) % interval == 0) {
          ItemStack reward = rewards.get(nextRewardIndex);
          nextRewardIndex++;

          if (reward != null) {
            player.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, player.getSoundCategory(), 0.5f, 1.0f + nextRewardIndex * 0.05f);

            sw.spawnParticles(
              ParticleTypes.SPLASH,
              basePos.x, basePos.y + 0.5, basePos.z,
              12, 0.2, 0.1, 0.2, 0.1
            );

            double angle = (nextRewardIndex * 2.0 * Math.PI / rewards.size()) + (java.util.concurrent.ThreadLocalRandom.current().nextDouble() - 0.5) * 0.5;
            double speed = 0.10 + java.util.concurrent.ThreadLocalRandom.current().nextDouble() * 0.08;
            double velY = 0.28 + java.util.concurrent.ThreadLocalRandom.current().nextDouble() * 0.12;
            double velX = Math.cos(angle) * speed;
            double velZ = Math.sin(angle) * speed;

            float itemYaw = player.getYaw() + 180f;
            DisplayEntity.ItemDisplayEntity display = AnimationUtils.spawnItemDisplay(
              sw, new Vec3d(basePos.x, basePos.y + 0.5, basePos.z), reward.copy(), new Vector3f(1.2f, 1.2f, 1.2f), itemYaw, 0
            );

            EjectedReward rewardEntity = new EjectedReward(
              display, basePos.x, basePos.y + 0.5, basePos.z, velX, velY, velZ, groundY, itemYaw
            );
            rewardEntities.add(rewardEntity);
          }
        }
      }

      // Update reward entities centrally
      for (int i = rewardEntities.size() - 1; i >= 0; i--) {
        EjectedReward reward = rewardEntities.get(i);
        reward.ticks++;

        if (!reward.landed) {
          reward.x += reward.velX;
          reward.z += reward.velZ;
          reward.velY -= 0.012;
          reward.y += reward.velY;

          if (reward.y <= reward.groundY) {
            reward.y = reward.groundY;
            reward.landed = true;
          }

          float spinPitch = reward.ticks * 6.0f;
          Quaternionf rotation = new Quaternionf().rotationYXZ((float) Math.toRadians(-reward.itemYaw), (float) Math.toRadians(spinPitch), 0);
          AnimationUtils.updateDisplayTransformation(
            reward.display, new Vec3d(reward.x, reward.y, reward.z),
            rotation, new Vector3f(1.2f, 1.2f, 1.2f), 2
          );

          if (reward.ticks % 3 == 0) {
            sw.spawnParticles(
              ParticleTypes.SPLASH,
              reward.x, reward.y + 0.5, reward.z,
              2, 0.05, 0.05, 0.05, 0.0
            );
          }
        } else {
          float spinPitch = reward.ticks * 2.0f;
          Quaternionf rotation = new Quaternionf().rotationYXZ((float) Math.toRadians(-reward.itemYaw), (float) Math.toRadians(spinPitch), 0);
          AnimationUtils.updateDisplayTransformation(
            reward.display, new Vec3d(reward.x, reward.y, reward.z),
            rotation, new Vector3f(1.2f, 1.2f, 1.2f), 2
          );
        }

        if (reward.ticks >= 80) {
          reward.display.discard();
          rewardEntities.remove(i);
        }
      }

      if (ticks >= 130) {
        this.kill();
        complete();
      }

      setTicks(ticks + 1);
    }
  }
}
