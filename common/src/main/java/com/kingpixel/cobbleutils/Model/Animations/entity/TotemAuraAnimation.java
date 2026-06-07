package com.kingpixel.cobbleutils.Model.Animations.entity;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.Animations.core.Animation;
import com.kingpixel.cobbleutils.Model.Animations.core.AnimationUtils;
import com.kingpixel.cobbleutils.Model.Animations.core.CustomArmorStandEntity;
import com.kingpixel.cobbleutils.Model.Animations.core.CustomItemDisplayEntity;
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

public class TotemAuraAnimation extends Animation {

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
      TotemEntity totem = new TotemEntity(
        player.getServerWorld(),
        center.x, center.y, center.z,
        obtained, player, onComplete, center.y
      );
      player.getServerWorld().spawnEntity(totem);
    });
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

  public static class TotemEntity extends CustomArmorStandEntity {
    private final ServerPlayerEntity player;
    private final Vec3d basePos;
    private final List<ItemStack> rewards;
    private final Runnable onDestroy;
    private final double groundY;
    private boolean completed = false;

    private CustomItemDisplayEntity totemDisplay;
    private final List<EjectedReward> rewardEntities = new ArrayList<>();

    public TotemEntity(World world, double x, double y, double z, List<ItemStack> rewards, ServerPlayerEntity player, Runnable onDestroy, double groundY) {
      super(world, x, y, z);
      this.player = player;
      this.basePos = new Vec3d(x, y, z);
      this.rewards = rewards;
      this.onDestroy = onDestroy;
      this.groundY = groundY;

      setNoGravity(true);
      setInvisible(true);
      setInvulnerable(true);

      ServerWorld sw = (ServerWorld) world;
      // Spawn a beautiful Totem of Undying display entity
      float yaw = player.getYaw() + 180f;
      totemDisplay = AnimationUtils.spawnItemDisplay(
        sw, new Vec3d(x, y + 0.5, z), new ItemStack(Items.TOTEM_OF_UNDYING), new Vector3f(1.8f, 1.8f, 1.8f), yaw, 0
      );
    }

    @Override public void complete() {
      if (!completed) {
        completed = true;
        if (totemDisplay != null) {
          totemDisplay.discard();
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
        double bobY = basePos.y + 0.5 + Math.sin(ticks * 0.25) * 0.15;
        float itemYaw = player.getYaw() + 180f + ticks * 4.0f; // Spin the totem gently
        Quaternionf rotation = new Quaternionf().rotationYXZ((float) Math.toRadians(-itemYaw), 0, 0);
        AnimationUtils.updateDisplayTransformation(
          totemDisplay, new Vec3d(basePos.x, bobY, basePos.z),
          rotation, new Vector3f(1.8f, 1.8f, 1.8f), 2
        );

        if (ticks % 3 == 0) {
          sw.spawnParticles(
            ParticleTypes.WITCH,
            basePos.x, bobY + 0.5, basePos.z,
            2, 0.1, 0.1, 0.1, 0.0
          );
        }
        if (ticks % 8 == 0) {
          player.playSoundToPlayer(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, player.getSoundCategory(), 0.5f, 1.2f);
        }
      } else if (ticks == 40) {
        if (totemDisplay != null) {
          totemDisplay.discard();
          totemDisplay = null;
        }

        player.playSoundToPlayer(SoundEvents.ITEM_TOTEM_USE, player.getSoundCategory(), 0.9f, 1.0f);
        sw.spawnParticles(
          ParticleTypes.TOTEM_OF_UNDYING,
          basePos.x, basePos.y + 0.8, basePos.z,
          40, 0.4, 0.4, 0.4, 0.2
        );

        int size = rewards.size();
        for (int i = 0; i < size; i++) {
          ItemStack reward = rewards.get(i);
          if (reward == null) continue;

          double angle = (i * 2.0 * Math.PI / size) + (java.util.concurrent.ThreadLocalRandom.current().nextDouble() - 0.5) * 0.4;
          double speed = 0.18 + java.util.concurrent.ThreadLocalRandom.current().nextDouble() * 0.10;
          double velY = 0.20 + java.util.concurrent.ThreadLocalRandom.current().nextDouble() * 0.12;
          double velX = Math.cos(angle) * speed;
          double velZ = Math.sin(angle) * speed;

          float itemYaw = player.getYaw() + 180f;
          CustomItemDisplayEntity display = AnimationUtils.spawnItemDisplay(
            sw, new Vec3d(basePos.x, basePos.y + 0.8, basePos.z), reward.copy(), new Vector3f(1.2f, 1.2f, 1.2f), itemYaw, 0
          );

          EjectedReward rewardEntity = new EjectedReward(
            display, basePos.x, basePos.y + 0.8, basePos.z, velX, velY, velZ, groundY, itemYaw
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
            reward.velY -= 0.014;
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
                ParticleTypes.TOTEM_OF_UNDYING,
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
