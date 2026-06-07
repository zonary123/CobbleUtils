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

public class SupernovaAnimation extends Animation {

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
    SupernovaCoreEntity core = new SupernovaCoreEntity(player.getServerWorld(), center.x, center.y + 1.2, center.z, obtained, player, onComplete, center.y);
    CobbleUtils.server.executeSync(() -> player.getServerWorld().spawnEntity(core));
  }

  public static class OrbitingReward {
    public final CustomItemDisplayEntity display;
    public final float itemYaw;

    public OrbitingReward(CustomItemDisplayEntity display, float itemYaw) {
      this.display = display;
      this.itemYaw = itemYaw;
    }
  }

  public static class EjectedReward {
    public final CustomItemDisplayEntity display;
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

    public EjectedReward(CustomItemDisplayEntity display, double x, double y, double z,
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

  public static class SupernovaCoreEntity extends CustomArmorStandEntity {
    private final ServerPlayerEntity player;
    private final List<ItemStack> rewards;
    private final Runnable onDestroy;
    private final double groundY;
    private boolean completed = false;
    private boolean blasted = false;
    private final List<OrbitingReward> coreRewardEntities = new ArrayList<>();
    private final List<EjectedReward> rewardEntities = new ArrayList<>();

    public SupernovaCoreEntity(World world, double x, double y, double z, List<ItemStack> rewards, ServerPlayerEntity player, Runnable onDestroy, double groundY) {
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
      float itemYaw = player.getYaw() + 180f;
      for (int i = 0; i < size; i++) {
        ItemStack reward = rewards.get(i);
        if (reward == null) continue;

        CustomItemDisplayEntity display = AnimationUtils.spawnItemDisplay(
          sw, new Vec3d(x, y, z), reward.copy(), new Vector3f(1.0f, 1.0f, 1.0f), itemYaw, 0
        );
        coreRewardEntities.add(new OrbitingReward(display, itemYaw));
      }
    }

    @Override public void complete() {
      if (!completed) {
        completed = true;
        for (OrbitingReward ent : coreRewardEntities) {
          if (ent.display != null) {
            ent.display.discard();
          }
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

      if (ticks < 40) {
        double orbitRadius = 0.15 + 0.35 * (1.0 - (ticks / 40.0));
        int size = coreRewardEntities.size();
        for (int i = 0; i < size; i++) {
          OrbitingReward rewardEnt = coreRewardEntities.get(i);
          if (rewardEnt.display == null) continue;
          double angle = (i * 2.0 * Math.PI / size) + Math.toRadians(ticks * 15.0);
          double rx = getX() + orbitRadius * Math.cos(angle);
          double rz = getZ() + orbitRadius * Math.sin(angle);
          double ry = getY() + Math.sin(ticks * 0.2 + i) * 0.05;

          float spinYaw = rewardEnt.itemYaw + ticks * 12.0f;
          float spinPitch = ticks * 6.0f;
          Quaternionf rotation = new Quaternionf().rotationYXZ((float) Math.toRadians(-spinYaw), (float) Math.toRadians(spinPitch), 0);
          AnimationUtils.updateDisplayTransformation(
            rewardEnt.display, new Vec3d(rx, ry, rz),
            rotation, new Vector3f(1.0f, 1.0f, 1.0f), 1
          );
        }

        double radius = 3.0 * (1.0 - (ticks / 40.0));
        for (int i = 0; i < 8; i++) {
          double angle = Math.toRadians((360.0 / 8.0) * i + ticks * 10);
          double ox = radius * Math.cos(angle);
          double oz = radius * Math.sin(angle);
          sw.spawnParticles(
            ParticleTypes.PORTAL,
            getX() + ox, getY(), getZ() + oz,
            1, 0.0, 0.0, 0.0, 0.0
          );
        }
        if (ticks % 4 == 0) {
          player.playSoundToPlayer(SoundEvents.BLOCK_PORTAL_AMBIENT, player.getSoundCategory(), 0.5f, 1.5f);
        }
      } else if (ticks == 40 && !blasted) {
        blasted = true;
        player.playSoundToPlayer(SoundEvents.ENTITY_GENERIC_EXPLODE.value(), player.getSoundCategory(), 1.2f, 0.8f);
        player.playSoundToPlayer(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, player.getSoundCategory(), 0.8f, 1.0f);

        for (OrbitingReward rewardEnt : coreRewardEntities) {
          if (rewardEnt.display != null) {
            rewardEnt.display.discard();
          }
        }
        coreRewardEntities.clear();

        sw.spawnParticles(
          ParticleTypes.FLASH,
          getX(), getY(), getZ(),
          5, 0.1, 0.1, 0.1, 0.0
        );
        sw.spawnParticles(
          ParticleTypes.EXPLOSION_EMITTER,
          getX(), getY(), getZ(),
          3, 0.5, 0.5, 0.5, 0.1
        );

        for (int i = 0; i < 30; i++) {
          double angle = Math.toRadians((360.0 / 30.0) * i);
          double ox = Math.cos(angle);
          double oz = Math.sin(angle);
          sw.spawnParticles(
            ParticleTypes.FLAME,
            getX(), getY(), getZ(),
            2, ox * 0.4, 0.0, oz * 0.4, 0.1
          );
        }

        int size = rewards.size();
        for (int i = 0; i < size; i++) {
          ItemStack reward = rewards.get(i);
          if (reward == null) continue;

          double angle = (i * 2.0 * Math.PI / size) + (java.util.concurrent.ThreadLocalRandom.current().nextDouble() - 0.5) * 0.4;
          double speed = 0.22 + java.util.concurrent.ThreadLocalRandom.current().nextDouble() * 0.12;
          double velY = 0.24 + java.util.concurrent.ThreadLocalRandom.current().nextDouble() * 0.15;
          double velX = Math.cos(angle) * speed;
          double velZ = Math.sin(angle) * speed;

          float itemYaw = player.getYaw() + 180f;
          CustomItemDisplayEntity display = AnimationUtils.spawnItemDisplay(
            sw, new Vec3d(getX(), getY(), getZ()), reward.copy(), new Vector3f(1.2f, 1.2f, 1.2f), itemYaw, 0
          );

          EjectedReward rewardEntity = new EjectedReward(
            display, getX(), getY(), getZ(), velX, velY, velZ, groundY, itemYaw
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
            reward.velY -= 0.016;
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
                ParticleTypes.FLAME,
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

        if (ticks >= 90) {
          this.kill();
          complete();
        }
      }

      setTicks(ticks + 1);
    }
  }
}
