package com.kingpixel.cobbleutils.Model.Animations.entity;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.Animations.core.Animation;
import com.kingpixel.cobbleutils.Model.Animations.core.AnimationUtils;
import com.kingpixel.cobbleutils.Model.Animations.core.CustomArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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

public class FireworksAnimation extends Animation {

  @Override
  public void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                    List<ItemStack> allRewards, Runnable onComplete) {
    int total = obtained.size();
    if (total == 0) {
      if (onComplete != null) onComplete.run();
      return;
    }

    Vec3d direction = player.getRotationVec(1.0f).normalize();
    Vec3d startPos = player.getPos().add(direction.x * 2.5, 0.0, direction.z * 2.5);

    CobbleUtils.server.executeSync(() -> {
      RocketEntity rocket = new RocketEntity(
        player.getServerWorld(),
        startPos.x, startPos.y, startPos.z,
        obtained, player, onComplete
      );
      player.getServerWorld().spawnEntity(rocket);
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

  public static class RocketEntity extends CustomArmorStandEntity {
    private final ServerPlayerEntity player;
    private final Vec3d basePos;
    private final List<ItemStack> rewards;
    private final Runnable onDestroy;
    private boolean completed = false;
    private boolean landed = false;

    private DisplayEntity.ItemDisplayEntity rocketDisplay;
    private final List<EjectedReward> rewardEntities = new ArrayList<>();

    public RocketEntity(World world, double x, double y, double z, List<ItemStack> rewards, ServerPlayerEntity player, Runnable onDestroy) {
      super(world, x, y, z);
      this.player = player;
      this.basePos = new Vec3d(x, y, z);
      this.rewards = rewards;
      this.onDestroy = onDestroy;

      setNoGravity(true);
      setInvisible(true);
      setInvulnerable(true);

      ServerWorld sw = (ServerWorld) world;
      float yaw = player.getYaw() + 180f;
      // Spawn a rocket display entity
      rocketDisplay = AnimationUtils.spawnItemDisplay(
        sw, new Vec3d(x, y + 0.5, z), new ItemStack(Items.FIREWORK_ROCKET), new Vector3f(1.2f, 1.2f, 1.2f), yaw, 0
      );
    }

    private void complete() {
      if (!completed) {
        completed = true;
        if (rocketDisplay != null) {
          rocketDisplay.discard();
        }
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

      if (ticks < 35) {
        double currentY = basePos.y + (ticks * 0.12);
        
        // Spin the rocket as it ascends
        float spinYaw = player.getYaw() + 180f + ticks * 25.0f;
        Quaternionf rotation = new Quaternionf().rotationY((float) Math.toRadians(-spinYaw));
        
        AnimationUtils.updateDisplayTransformation(
          rocketDisplay, new Vec3d(basePos.x, currentY + 0.5, basePos.z),
          rotation, new Vector3f(1.2f, 1.2f, 1.2f), 2
        );

        if (ticks % 2 == 0) {
          sw.spawnParticles(
            ParticleTypes.SMALL_FLAME,
            basePos.x, currentY + 0.5, basePos.z,
            1, 0.02, 0.02, 0.02, 0.0
          );
          player.playSoundToPlayer(SoundEvents.ENTITY_FIREWORK_ROCKET_SHOOT, player.getSoundCategory(), 0.5f, 1.2f);
        }
      } else if (ticks == 35 && !landed) {
        landed = true;
        if (rocketDisplay != null) {
          rocketDisplay.discard();
          rocketDisplay = null;
        }

        player.playSoundToPlayer(SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, player.getSoundCategory(), 1.2f, 1.0f);
        player.playSoundToPlayer(SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, player.getSoundCategory(), 1.0f, 1.0f);

        double burstY = basePos.y + (35 * 0.12);
        sw.spawnParticles(
          ParticleTypes.FIREWORK,
          basePos.x, burstY + 0.5, basePos.z,
          40, 0.4, 0.4, 0.4, 0.15
        );
        sw.spawnParticles(
          ParticleTypes.GLOW,
          basePos.x, burstY + 0.5, basePos.z,
          10, 0.3, 0.3, 0.3, 0.05
        );

        int size = rewards.size();
        for (int i = 0; i < size; i++) {
          ItemStack reward = rewards.get(i);
          if (reward == null) continue;

          double angle = (i * 2.0 * Math.PI / size) + (java.util.concurrent.ThreadLocalRandom.current().nextDouble() - 0.5) * 0.4;
          double speed = 0.16 + java.util.concurrent.ThreadLocalRandom.current().nextDouble() * 0.12;
          double velY = 0.18 + java.util.concurrent.ThreadLocalRandom.current().nextDouble() * 0.10;
          double velX = Math.cos(angle) * speed;
          double velZ = Math.sin(angle) * speed;

          float itemYaw = player.getYaw() + 180f;
          DisplayEntity.ItemDisplayEntity display = AnimationUtils.spawnItemDisplay(
            sw, new Vec3d(basePos.x, burstY + 0.5, basePos.z), reward.copy(), new Vector3f(1.2f, 1.2f, 1.2f), itemYaw, 0
          );

          EjectedReward rewardEntity = new EjectedReward(
            display, basePos.x, burstY + 0.5, basePos.z, velX, velY, velZ, basePos.y - 0.5, itemYaw
          );
          rewardEntities.add(rewardEntity);
        }
      } else {
        // Update reward entities centrally
        for (int i = rewardEntities.size() - 1; i >= 0; i--) {
          EjectedReward reward = rewardEntities.get(i);
          reward.ticks++;

          if (!reward.landed) {
            reward.x += reward.velX;
            reward.z += reward.velZ;
            reward.velY -= 0.008;
            reward.y += reward.velY;

            if (reward.y <= reward.groundY) {
              reward.y = reward.groundY;
              reward.landed = true;
            }

            float spinPitch = reward.ticks * 8.0f;
            Quaternionf rotation = new Quaternionf().rotationYXZ((float) Math.toRadians(-reward.itemYaw), (float) Math.toRadians(spinPitch), 0);
            AnimationUtils.updateDisplayTransformation(
              reward.display, new Vec3d(reward.x, reward.y, reward.z),
              rotation, new Vector3f(1.2f, 1.2f, 1.2f), 2
            );

            if (reward.ticks % 2 == 0) {
              sw.spawnParticles(
                ParticleTypes.CHERRY_LEAVES,
                reward.x, reward.y + 0.5, reward.z,
                1, 0.05, 0.05, 0.05, 0.0
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

        if (ticks >= 120) {
          this.kill();
          complete();
        }
      }

      setTicks(ticks + 1);
    }
  }
}
