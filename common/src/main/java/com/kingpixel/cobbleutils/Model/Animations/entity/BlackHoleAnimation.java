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

public class BlackHoleAnimation extends Animation {

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
    BlackHoleCoreEntity core = new BlackHoleCoreEntity(
      player.getServerWorld(),
      center.x, center.y + 1.2, center.z,
      obtained, player, onComplete, center.y
    );
    CobbleUtils.server.executeSync(() -> player.getServerWorld().spawnEntity(core));
  }

  public static class OrbitingReward {
    public final CustomItemDisplayEntity display;
    public final double startAngle;
    public final float itemYaw;

    public OrbitingReward(CustomItemDisplayEntity display, double startAngle, float itemYaw) {
      this.display = display;
      this.startAngle = startAngle;
      this.itemYaw = itemYaw;
    }
  }

  public static class LandingReward {
    public final CustomItemDisplayEntity display;
    public double x;
    public double y;
    public double z;
    public final double velX;
    public double velY;
    public final double velZ;
    public final double groundY;
    public boolean landedOnGround = false;
    public final float itemYaw;
    public int ticks = 0;

    public LandingReward(CustomItemDisplayEntity display, double x, double y, double z,
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

  public static class BlackHoleCoreEntity extends CustomArmorStandEntity {
    private final ServerPlayerEntity player;
    private final List<ItemStack> rewards;
    private final Runnable onDestroy;
    private final double groundY;
    private boolean completed = false;
    private boolean collapsed = false;
    private final List<OrbitingReward> orbitingRewards = new ArrayList<>();
    private final List<LandingReward> scatteredRewards = new ArrayList<>();

    public BlackHoleCoreEntity(World world, double x, double y, double z, List<ItemStack> rewards, ServerPlayerEntity player, Runnable onDestroy, double groundY) {
      super(world, x, y, z);
      this.player = player;
      this.rewards = rewards;
      this.onDestroy = onDestroy;
      this.groundY = groundY;

      setNoGravity(true);
      setInvisible(true);
      setInvulnerable(true);

      ServerWorld sw = (ServerWorld) world;
      int size = rewards.size();
      float itemYaw = player.getYaw() + 180;
      for (int i = 0; i < size; i++) {
        double angle = i * 2.0 * Math.PI / size;
        double rx = x + Math.cos(angle) * 2.5;
        double rz = z + Math.sin(angle) * 2.5;

        CustomItemDisplayEntity display = AnimationUtils.spawnItemDisplay(
          sw, new Vec3d(rx, y, rz), rewards.get(i).copy(), new Vector3f(1.0f, 1.0f, 1.0f), itemYaw, 0
        );
        orbitingRewards.add(new OrbitingReward(display, angle, itemYaw));
      }
    }

    @Override public void complete() {
      if (!completed) {
        completed = true;
        for (OrbitingReward reward : orbitingRewards) {
          if (reward.display != null) {
            reward.display.discard();
          }
        }
        for (LandingReward reward : scatteredRewards) {
          if (reward.display != null) {
            reward.display.discard();
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

      if (ticks < 45) {
        sw.spawnParticles(
          ParticleTypes.PORTAL,
          getX(), getY(), getZ(),
          20, 0.4, 0.4, 0.4, 0.15
        );
        sw.spawnParticles(
          ParticleTypes.DRAGON_BREATH,
          getX(), getY(), getZ(),
          8, 0.3, 0.3, 0.3, 0.05
        );

        if (ticks % 3 == 0) {
          sw.spawnParticles(
            ParticleTypes.SQUID_INK,
            getX(), getY(), getZ(),
            12, 0.2, 0.2, 0.2, 0.02
          );
        }

        double progress = ticks / 45.0;
        double radius = 2.5 * (1.0 - progress);

        for (int i = 0; i < orbitingRewards.size(); i++) {
          OrbitingReward reward = orbitingRewards.get(i);
          if (reward.display == null) continue;

          double angle = reward.startAngle + Math.toRadians(ticks * 8.0);
          double ix = getX() + radius * Math.cos(angle);
          double iz = getZ() + radius * Math.sin(angle);
          double iy = getY();

          float spinYaw = reward.itemYaw + ticks * 12.0f;
          float spinPitch = ticks * 6.0f;
          Quaternionf rotation = new Quaternionf().rotationYXZ((float) Math.toRadians(-spinYaw), (float) Math.toRadians(spinPitch), 0);
          AnimationUtils.updateDisplayTransformation(
            reward.display, new Vec3d(ix, iy, iz),
            rotation, new Vector3f(1.0f - (float) progress, 1.0f - (float) progress, 1.0f - (float) progress), 1
          );
        }

        if (ticks % 6 == 0) {
          player.playSoundToPlayer(SoundEvents.BLOCK_BEACON_AMBIENT, player.getSoundCategory(), 0.7f, 0.5f);
        }
      } else if (ticks == 45) {
        for (OrbitingReward reward : orbitingRewards) {
          if (reward.display != null) {
            reward.display.discard();
          }
        }
        player.playSoundToPlayer(SoundEvents.BLOCK_PORTAL_TRAVEL, player.getSoundCategory(), 0.8f, 0.5f);
      } else if (ticks < 50) {
        double pulseRadius = 0.5 + Math.sin((ticks - 45) * 1.5) * 0.4;
        sw.spawnParticles(
          ParticleTypes.REVERSE_PORTAL,
          getX(), getY(), getZ(),
          30, pulseRadius, pulseRadius, pulseRadius, 0.2
        );
      } else if (ticks == 50 && !collapsed) {
        collapsed = true;
        player.playSoundToPlayer(SoundEvents.ENTITY_GENERIC_EXPLODE.value(), player.getSoundCategory(), 1.2f, 0.7f);
        player.playSoundToPlayer(SoundEvents.ENTITY_WITHER_DEATH, player.getSoundCategory(), 1.0f, 1.4f);

        sw.spawnParticles(
          ParticleTypes.SONIC_BOOM,
          getX(), getY(), getZ(),
          2, 0.1, 0.1, 0.1, 0.0
        );
        sw.spawnParticles(
          ParticleTypes.EXPLOSION_EMITTER,
          getX(), getY(), getZ(),
          3, 0.5, 0.5, 0.5, 0.1
        );
        sw.spawnParticles(
          ParticleTypes.FLASH,
          getX(), getY(), getZ(),
          5, 0.1, 0.1, 0.1, 0.0
        );

        for (OrbitingReward reward : orbitingRewards) {
          if (reward.display != null) {
            reward.display.discard();
          }
        }

        int size = rewards.size();
        for (int i = 0; i < size; i++) {
          ItemStack reward = rewards.get(i);
          if (reward == null) continue;

          double angle = (i * 2.0 * Math.PI / size) + (java.util.concurrent.ThreadLocalRandom.current().nextDouble() - 0.5) * 0.4;
          double speed = 0.14 + java.util.concurrent.ThreadLocalRandom.current().nextDouble() * 0.1;
          double velY = 0.18 + java.util.concurrent.ThreadLocalRandom.current().nextDouble() * 0.12;
          double velX = Math.cos(angle) * speed;
          double velZ = Math.sin(angle) * speed;

          float landingYaw = (float) Math.toDegrees(angle) + 180f;
          CustomItemDisplayEntity display = AnimationUtils.spawnItemDisplay(
            sw, new Vec3d(getX(), getY(), getZ()), reward.copy(), new Vector3f(1.0f, 1.0f, 1.0f), landingYaw, 0
          );

          LandingReward landingReward = new LandingReward(
            display, getX(), getY(), getZ(), velX, velY, velZ, groundY, landingYaw
          );
          scatteredRewards.add(landingReward);
        }
      } else {
        // Update scattered/landing rewards centrally
        for (int i = scatteredRewards.size() - 1; i >= 0; i--) {
          LandingReward reward = scatteredRewards.get(i);
          reward.ticks++;

          if (!reward.landedOnGround) {
            reward.x += reward.velX;
            reward.z += reward.velZ;
            reward.velY -= 0.016;
            reward.y += reward.velY;

            if (reward.y <= reward.groundY) {
              reward.y = reward.groundY;
              reward.landedOnGround = true;
            }

            float spinPitch = reward.ticks * 8.0f;
            Quaternionf rotation = new Quaternionf().rotationYXZ((float) Math.toRadians(-reward.itemYaw), (float) Math.toRadians(spinPitch), 0);
            AnimationUtils.updateDisplayTransformation(
              reward.display, new Vec3d(reward.x, reward.y, reward.z),
              rotation, new Vector3f(1.0f, 1.0f, 1.0f), 2
            );

            if (reward.ticks % 2 == 0) {
              sw.spawnParticles(
                ParticleTypes.DRAGON_BREATH,
                reward.x, reward.y + 0.5, reward.z,
                1, 0.05, 0.05, 0.05, 0.0
              );
            }
          } else {
            float spinPitch = reward.ticks * 2.0f;
            Quaternionf rotation = new Quaternionf().rotationYXZ((float) Math.toRadians(-reward.itemYaw), (float) Math.toRadians(spinPitch), 0);
            AnimationUtils.updateDisplayTransformation(
              reward.display, new Vec3d(reward.x, reward.y, reward.z),
              rotation, new Vector3f(1.0f, 1.0f, 1.0f), 2
            );
          }

          if (reward.ticks >= 80) {
            reward.display.discard();
            scatteredRewards.remove(i);
          }
        }

        if (ticks >= 150) {
          this.kill();
          complete();
        }
      }

      setTicks(ticks + 1);
    }
  }
}
