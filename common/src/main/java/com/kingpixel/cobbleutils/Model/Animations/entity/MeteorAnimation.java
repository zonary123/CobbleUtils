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

public class MeteorAnimation extends Animation {

  @Override
  public void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                    List<ItemStack> allRewards, Runnable onComplete) {
    int total = obtained.size();
    if (total == 0) {
      if (onComplete != null) onComplete.run();
      return;
    }

    Vec3d center = AnimationUtils.getPosition(player, position);
    CobbleUtils.server.executeSync(() -> {
      MeteorShowerController controller = new MeteorShowerController(
        player.getServerWorld(),
        center.x, center.y, center.z,
        obtained, player,
        onComplete
      );
      player.getServerWorld().spawnEntity(controller);
    });
  }

  public static class MeteorProjectile {
    public final DisplayEntity.ItemDisplayEntity display;
    public final Vec3d startPos;
    public final Vec3d targetPos;
    public final int duration = 25;
    public final List<ItemStack> itemsToLaunch;
    public final double sizeScale;
    public int ticks = 0;
    public final float yaw;

    public MeteorProjectile(DisplayEntity.ItemDisplayEntity display, Vec3d startPos, Vec3d targetPos,
                            List<ItemStack> itemsToLaunch, double sizeScale, float yaw) {
      this.display = display;
      this.startPos = startPos;
      this.targetPos = targetPos;
      this.itemsToLaunch = itemsToLaunch;
      this.sizeScale = sizeScale;
      this.yaw = yaw;
    }
  }

  public static class EjectedReward {
    public final DisplayEntity.ItemDisplayEntity display;
    public double x;
    public double y;
    public double z;
    public double velX;
    public double velY;
    public double velZ;
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

  public static class MeteorShowerController extends CustomArmorStandEntity {
    private final ServerPlayerEntity player;
    private final List<ItemStack> rewards;
    private final Runnable onDestroy;
    private boolean completed = false;

    private final List<MeteorProjectile> activeMeteors = new ArrayList<>();
    private final List<EjectedReward> rewardEntities = new ArrayList<>();

    public MeteorShowerController(World world, double x, double y, double z, List<ItemStack> rewards, ServerPlayerEntity player, Runnable onDestroy) {
      super(world, x, y, z);
      this.player = player;
      this.rewards = rewards;
      this.onDestroy = onDestroy;

      setNoGravity(true);
      setInvisible(true);
      setInvulnerable(true);
    }

    private void complete() {
      if (!completed) {
        completed = true;
        for (MeteorProjectile met : activeMeteors) {
          if (met.display != null) {
            met.display.discard();
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

      // Spawn meteors at ticks 0, 10, 20
      if (ticks == 0) {
        spawnMeteor(1, 0.35);
      } else if (ticks == 10) {
        spawnMeteor(2, 0.35);
      } else if (ticks == 20) {
        spawnMeteor(3, 0.30);
      }

      // Update meteors centrally
      for (int i = activeMeteors.size() - 1; i >= 0; i--) {
        MeteorProjectile meteor = activeMeteors.get(i);
        meteor.ticks++;

        if (meteor.ticks < meteor.duration) {
          double progress = (double) meteor.ticks / meteor.duration;
          double cx = meteor.startPos.x + (meteor.targetPos.x - meteor.startPos.x) * progress;
          double cy = meteor.startPos.y + (meteor.targetPos.y - 1.8 - meteor.startPos.y) * progress;
          double cz = meteor.startPos.z + (meteor.targetPos.z - meteor.startPos.z) * progress;
          Vec3d currentPos = new Vec3d(cx, cy, cz);

          // Rotate the Magma Block display in 3D to roll like a meteor!
          float rollAngle = meteor.ticks * 15.0f;
          Quaternionf rotation = new Quaternionf().rotationYXZ((float) Math.toRadians(-meteor.yaw), (float) Math.toRadians(rollAngle), (float) Math.toRadians(rollAngle));
          float scale = (float) meteor.sizeScale * 2.8f;

          AnimationUtils.updateDisplayTransformation(
            meteor.display, currentPos, rotation, new Vector3f(scale, scale, scale), 2
          );

          int particleCount = (int) (18 * meteor.sizeScale);
          sw.spawnParticles(
            ParticleTypes.FLAME,
            cx, cy + 1.8, cz,
            particleCount, 0.25 * scale, 0.25 * scale, 0.25 * scale, 0.1
          );
          sw.spawnParticles(
            ParticleTypes.LARGE_SMOKE,
            cx, cy + 1.8, cz,
            (int) (12 * meteor.sizeScale), 0.35 * scale, 0.35 * scale, 0.35 * scale, 0.05
          );
          sw.spawnParticles(
            ParticleTypes.LAVA,
            cx, cy + 1.8, cz,
            (int) (6 * meteor.sizeScale), 0.2 * scale, 0.2 * scale, 0.2 * scale, 0.0
          );
        } else {
          // Explode!
          player.playSoundToPlayer(SoundEvents.ENTITY_GENERIC_EXPLODE.value(), player.getSoundCategory(), 1.4f, 0.5f);
          player.playSoundToPlayer(SoundEvents.BLOCK_FIRE_AMBIENT, player.getSoundCategory(), 1.0f, 1.0f);

          sw.spawnParticles(
            ParticleTypes.EXPLOSION_EMITTER,
            meteor.targetPos.x, meteor.targetPos.y + 0.5, meteor.targetPos.z,
            (int) (3 * meteor.sizeScale), 0.6 * meteor.sizeScale, 0.6 * meteor.sizeScale, 0.6 * meteor.sizeScale, 0.1
          );
          sw.spawnParticles(
            ParticleTypes.FLASH,
            meteor.targetPos.x, meteor.targetPos.y + 0.5, meteor.targetPos.z,
            (int) (2 * meteor.sizeScale), 0.3 * meteor.sizeScale, 0.3 * meteor.sizeScale, 0.3 * meteor.sizeScale, 0.0
          );

          // Eject items
          int size = meteor.itemsToLaunch.size();
          for (int j = 0; j < size; j++) {
            ItemStack reward = meteor.itemsToLaunch.get(j);
            if (reward == null) continue;

            double angle = (j * 2.0 * Math.PI / size) + (java.util.concurrent.ThreadLocalRandom.current().nextDouble() - 0.5) * 0.5;
            double speed = 0.22 + java.util.concurrent.ThreadLocalRandom.current().nextDouble() * 0.14;
            double velY = 0.26 + java.util.concurrent.ThreadLocalRandom.current().nextDouble() * 0.18;
            double velX = Math.cos(angle) * speed;
            double velZ = Math.sin(angle) * speed;

            float itemYaw = player.getYaw() + 180f;
            DisplayEntity.ItemDisplayEntity display = AnimationUtils.spawnItemDisplay(
              sw, meteor.targetPos, reward.copy(), new Vector3f(1.2f, 1.2f, 1.2f), itemYaw, 0
            );

            EjectedReward rewardEntity = new EjectedReward(
              display, meteor.targetPos.x, meteor.targetPos.y, meteor.targetPos.z,
              velX, velY, velZ, meteor.targetPos.y - 0.5, itemYaw
            );
            rewardEntities.add(rewardEntity);
          }

          meteor.display.discard();
          activeMeteors.remove(i);
        }
      }

      // Update ejected reward entities centrally
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
              ParticleTypes.SMALL_FLAME,
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

      if (ticks >= 85 && activeMeteors.isEmpty() && rewardEntities.isEmpty()) {
        this.kill();
        complete();
      }

      setTicks(ticks + 1);
    }

    private void spawnMeteor(int index, double percentage) {
      int totalSize = rewards.size();
      int startIndex = 0;
      int endIndex = 0;

      if (index == 1) {
        startIndex = 0;
        endIndex = Math.max(1, (int) (totalSize * percentage));
      } else if (index == 2) {
        startIndex = Math.max(1, (int) (totalSize * 0.35));
        endIndex = Math.max(startIndex + 1, (int) (totalSize * 0.70));
      } else {
        startIndex = Math.max(1, (int) (totalSize * 0.70));
        endIndex = totalSize;
      }

      if (startIndex >= totalSize && index > 1) {
        return;
      }

      List<ItemStack> meteorItems = new ArrayList<>();
      for (int i = startIndex; i < Math.min(endIndex, totalSize); i++) {
        ItemStack item = rewards.get(i);
        if (item != null) meteorItems.add(item);
      }

      if (meteorItems.isEmpty() && index > 1) {
        return;
      }
      if (meteorItems.isEmpty() && !rewards.isEmpty()) {
        meteorItems.add(rewards.get(0));
      }

      Vec3d direction = player.getRotationVec(1.0f).normalize();
      double distance = 6.0 + index * 1.5;
      double angleOffset = (index - 2) * 35.0;
      double rad = Math.toRadians(angleOffset);

      double rx = direction.x * Math.cos(rad) - direction.z * Math.sin(rad);
      double rz = direction.x * Math.sin(rad) + direction.z * Math.cos(rad);

      Vec3d targetPos = player.getPos().add(rx * distance, 0.0, rz * distance);

      double sx = targetPos.x - 8.0 + (index * 2.0);
      double sy = targetPos.y + 18.0 + (index * 2.0);
      double sz = targetPos.z - 8.0 - (index * 1.0);
      double scale = 1.0 + (index * 0.35);

      ServerWorld sw = (ServerWorld) this.getWorld();
      float itemYaw = player.getYaw() + 180f;

      // Spawn meteor as beautiful rolling magma block display entity
      DisplayEntity.ItemDisplayEntity display = AnimationUtils.spawnItemDisplay(
        sw, new Vec3d(sx, sy, sz), new ItemStack(Items.MAGMA_BLOCK), new Vector3f((float) scale, (float) scale, (float) scale), itemYaw, 0
      );

      // Play fiery launch sound once upon spawning
      player.playSoundToPlayer(SoundEvents.ENTITY_GHAST_SHOOT, player.getSoundCategory(), 1.0f, 0.5f);

      MeteorProjectile projectile = new MeteorProjectile(
        display, new Vec3d(sx, sy, sz), targetPos, meteorItems, scale, itemYaw
      );
      activeMeteors.add(projectile);
    }
  }
}
