package com.kingpixel.cobbleutils.Model.Animations.entity;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.Animations.core.Animation;
import com.kingpixel.cobbleutils.Model.Animations.core.AnimationUtils;
import com.kingpixel.cobbleutils.Model.Animations.core.CustomArmorStandEntity;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class GiftAnimation extends Animation {

  @Override
  public void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                    List<ItemStack> allRewards, Runnable onComplete) {
    if (obtained.isEmpty()) {
      if (onComplete != null) onComplete.run();
      return;
    }

    Vec3d center = AnimationUtils.getPosition(player, position);

    CobbleUtils.server.executeSync(() -> {
      GiftBoxController controller = new GiftBoxController(
        player.getServerWorld(), center.x, center.y, center.z,
        obtained, player, onComplete
      );
      player.getServerWorld().spawnEntity(controller);
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

  public static class GiftBoxController extends CustomArmorStandEntity {
    private final ServerPlayerEntity player;
    private final Vec3d staticChestPos;
    private final List<ItemStack> rewards;
    private final Runnable onDestroy;
    private boolean completed = false;

    private DisplayEntity.BlockDisplayEntity blockDisplay;
    private final List<EjectedReward> activeRewards = new ArrayList<>();
    private int currentRewardIndex = 0;
    private final int spawnIntervalTicks = 8;

    private final float chestFacingYaw;
    private float currentVisualShake = 0f;

    private final float scaleFactor = 2.5f;
    private final double displayHeightOffset = 0.2;
    private final double chestTopY;

    public GiftBoxController(World world, double x, double y, double z,
                             List<ItemStack> rewards, ServerPlayerEntity player, Runnable onDestroy) {
      super(world, x, y, z);
      this.player = player;
      this.rewards = rewards;
      this.onDestroy = onDestroy;

      this.setNoGravity(true);
      this.setInvisible(true);
      this.setInvulnerable(true);

      Vec3d direction = player.getRotationVec(1.0f).normalize();
      this.staticChestPos = new Vec3d(x, y, z).add(direction.x * 2.5, 0.0, direction.z * 2.5);

      this.chestFacingYaw = AnimationUtils.getYawToFacePlayer(player, staticChestPos) + 180f;
      this.chestTopY = staticChestPos.y + displayHeightOffset + scaleFactor;

      this.refreshPositionAndAngles(staticChestPos.x, staticChestPos.y, staticChestPos.z, chestFacingYaw, 0);

      ServerWorld sw = (ServerWorld) world;
      this.blockDisplay = new DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, sw);
      this.blockDisplay.setBlockState(Blocks.CHEST.getDefaultState());

      this.blockDisplay.refreshPositionAndAngles(staticChestPos.x, staticChestPos.y, staticChestPos.z, chestFacingYaw, 0f);
      buildDisplayTransformation(0f);

      sw.spawnEntity(this.blockDisplay);
    }

    private void buildDisplayTransformation(float shakeYaw) {
      if (blockDisplay == null) return;

      float angleRad = (float) Math.toRadians(-shakeYaw);
      Quaternionf rotationQuaternion = new Quaternionf().rotationY(angleRad);
      Vector3f scaleVector = new Vector3f(scaleFactor, scaleFactor, scaleFactor);

      // Rotate local pivot center of the chest base to keep it perfectly centered at staticChestPos
      Vector3f localCenter = rotationQuaternion.transform(new Vector3f(scaleFactor / 2f, 0f, scaleFactor / 2f));
      Vector3f translationVector = new Vector3f(-localCenter.x, (float) displayHeightOffset, -localCenter.z);

      blockDisplay.setTransformation(new AffineTransformation(translationVector, rotationQuaternion, scaleVector, null));
    }

    private void complete() {
      if (!completed) {
        completed = true;
        if (blockDisplay != null) blockDisplay.discard();
        for (EjectedReward reward : activeRewards) {
          if (reward.display != null) reward.display.discard();
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

      if (ticks < 30) {
        currentVisualShake = (float) (Math.sin(ticks * 1.5) * 10.0);
        buildDisplayTransformation(currentVisualShake);

        if (ticks % 4 == 0) {
          sw.spawnParticles(ParticleTypes.HAPPY_VILLAGER, staticChestPos.x, chestTopY - 0.5, staticChestPos.z, 2, 0.3, 0.2, 0.3, 0.0);
          player.playSoundToPlayer(SoundEvents.BLOCK_CHEST_LOCKED, SoundCategory.BLOCKS, 0.4f, 1.2f);
        }
      } else if (ticks >= 30 && currentRewardIndex < rewards.size()) {
        buildDisplayTransformation(0f);

        if ((ticks - 30) % spawnIntervalTicks == 0) {
          ItemStack currentReward = rewards.get(currentRewardIndex);

          if (currentReward != null) {
            ThreadLocalRandom rand = ThreadLocalRandom.current();

            double angle = rand.nextDouble() * 2.0 * Math.PI;
            double speed = 0.18 + rand.nextDouble() * 0.12; // Eject items further outward
            double velX = Math.cos(angle) * speed;
            double velY = 0.32 + rand.nextDouble() * 0.12; // Fountain trajectory higher
            double velZ = Math.sin(angle) * speed;

            float itemYaw = player.getYaw() + 180;
            DisplayEntity.ItemDisplayEntity display = AnimationUtils.spawnItemDisplay(
              sw, new Vec3d(staticChestPos.x, chestTopY, staticChestPos.z),
              currentReward.copy(), new Vector3f(1.2f, 1.2f, 1.2f), itemYaw, 0
            );

            EjectedReward reward = new EjectedReward(
              display, staticChestPos.x, chestTopY, staticChestPos.z,
              velX, velY, velZ, staticChestPos.y, itemYaw
            );

            activeRewards.add(reward);

            sw.spawnParticles(ParticleTypes.CLOUD, staticChestPos.x, chestTopY, staticChestPos.z, 4, 0.1, 0.05, 0.1, 0.02);
            player.playSoundToPlayer(SoundEvents.ENTITY_CHICKEN_EGG, SoundCategory.BLOCKS, 0.5f, 0.6f);
          }

          currentRewardIndex++;
        }
      }

      // Update all active reward entities centrally
      for (int i = activeRewards.size() - 1; i >= 0; i--) {
        EjectedReward reward = activeRewards.get(i);
        reward.ticks++;

        if (!reward.landed) {
          reward.x += reward.velX;
          reward.z += reward.velZ;
          reward.velY -= 0.014;
          reward.y += reward.velY;

          if (reward.y <= reward.groundY) {
            reward.y = reward.groundY;
            reward.landed = true;

            sw.spawnParticles(ParticleTypes.CRIT, reward.x, reward.y + 0.5, reward.z, 5, 0.1, 0.1, 0.1, 0.02);
            player.playSoundToPlayer(SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.4f, 1.5f);
          }

          float spinPitch = reward.ticks * 6.0f;
          Quaternionf rotation = new Quaternionf().rotationYXZ((float) Math.toRadians(-reward.itemYaw), (float) Math.toRadians(spinPitch), 0);
          AnimationUtils.updateDisplayTransformation(
            reward.display, new Vec3d(reward.x, reward.y, reward.z),
            rotation, new Vector3f(1.2f, 1.2f, 1.2f), 2
          );

          if (reward.ticks % 2 == 0) {
            sw.spawnParticles(ParticleTypes.END_ROD, reward.x, reward.y + 0.5, reward.z, 1, 0.02, 0.02, 0.02, 0.0);
          }
        } else {
          // Keep updating orientation/position even when landed
          float spinPitch = reward.ticks * 3.0f; // Spin more slowly when landed
          Quaternionf rotation = new Quaternionf().rotationYXZ((float) Math.toRadians(-reward.itemYaw), (float) Math.toRadians(spinPitch), 0);
          AnimationUtils.updateDisplayTransformation(
            reward.display, new Vec3d(reward.x, reward.y, reward.z),
            rotation, new Vector3f(1.2f, 1.2f, 1.2f), 2
          );
        }

        if (reward.ticks >= 90) {
          sw.spawnParticles(ParticleTypes.ASH, reward.x, reward.y + 0.5, reward.z, 4, 0.1, 0.1, 0.1, 0.0);
          reward.display.discard();
          activeRewards.remove(i);
        }
      }

      int finishingTickThreshold = 30 + (rewards.size() * spawnIntervalTicks) + 80;
      if (ticks >= finishingTickThreshold) {
        sw.spawnParticles(ParticleTypes.POOF, staticChestPos.x, chestTopY - 1.0, staticChestPos.z, 15, 0.4, 0.4, 0.4, 0.05);
        player.playSoundToPlayer(SoundEvents.BLOCK_CHEST_CLOSE, SoundCategory.BLOCKS, 0.8f, 1.0f);

        this.kill();
        complete();
      }

      setTicks(ticks + 1);
    }
  }
}