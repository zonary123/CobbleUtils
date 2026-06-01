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
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class EWheelOfFortuneAnimation extends Animation {

  private static final int WHEEL_SIZE = 8;
  private static final double WHEEL_RADIUS = 1.3;

  @Override
  public void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                    List<ItemStack> allRewards, Runnable onComplete) {
    if (obtained.isEmpty()) {
      if (onComplete != null) onComplete.run();
      return;
    }

    Vec3d center = AnimationUtils.getPosition(player, position);

    CobbleUtils.server.executeSync(() -> {
      WheelController controller = new WheelController(
        player.getServerWorld(), center.x, center.y, center.z,
        obtained, allRewards, player, onComplete
      );
      player.getServerWorld().spawnEntity(controller);
    });
  }

  public static class WheelController extends CustomArmorStandEntity {
    private final ServerPlayerEntity player;
    private final List<ItemStack> obtained;
    private final List<ItemStack> allRewards;
    private final Runnable onDestroy;
    private boolean completed = false;

    private DisplayEntity.ItemDisplayEntity pointerDisplay;
    private final List<DisplayEntity.ItemDisplayEntity> wheelDisplays = new ArrayList<>();
    private final List<Vec3d> wheelPositions = new ArrayList<>();

    private int totalRotationsSteps = 35;
    private int currentStepIndex = 0;
    private int nextTickTrigger = 1;
    private int cycleShift = 0;
    private float facingYaw;

    public WheelController(World world, double x, double y, double z,
                           List<ItemStack> obtained, List<ItemStack> allRewards,
                           ServerPlayerEntity player, Runnable onDestroy) {
      super(world, x, y, z);
      this.player = player;
      this.obtained = obtained;
      this.allRewards = allRewards;
      this.onDestroy = onDestroy;

      setNoGravity(true);
      setInvisible(true);
      setInvulnerable(true);

      initPhysicalWheel();
    }

    private void initPhysicalWheel() {
      ServerWorld sw = (ServerWorld) this.getWorld();
      ThreadLocalRandom random = ThreadLocalRandom.current();

      Vec3d lookVec = Vec3d.fromPolar(0, player.getYaw()).normalize();
      Vec3d centerPos = this.getPos().add(lookVec.multiply(3.5)).add(0, 1.5, 0);
      facingYaw = AnimationUtils.getYawToFacePlayer(player, centerPos);

      Vec3d rightVec = new Vec3d(-lookVec.z, 0, lookVec.x).normalize();
      Vec3d upVec = new Vec3d(0, 1, 0);

      // 1. Spawn Pointer item display
      Vec3d pointerPos = centerPos.add(upVec.multiply(WHEEL_RADIUS + 0.35));
      pointerDisplay = AnimationUtils.spawnItemDisplay(
        sw, pointerPos, new ItemStack(Items.RED_STAINED_GLASS_PANE), new Vector3f(0.8f, 0.8f, 0.8f), facingYaw, 0
      );

      // 2. Spawn 8 Wheel item displays
      for (int k = 0; k < WHEEL_SIZE; k++) {
        double angle = (2 * Math.PI / WHEEL_SIZE) * k;
        Vec3d nodeOffset = rightVec.multiply(Math.cos(angle) * WHEEL_RADIUS).add(upVec.multiply(Math.sin(angle) * WHEEL_RADIUS));
        Vec3d standPos = centerPos.add(nodeOffset);
        wheelPositions.add(standPos);

        ItemStack item = allRewards.get(random.nextInt(allRewards.size()));
        DisplayEntity.ItemDisplayEntity display = AnimationUtils.spawnItemDisplay(
          sw, standPos, item.copy(), new Vector3f(1.0f, 1.0f, 1.0f), facingYaw, 0
        );
        wheelDisplays.add(display);
      }

      player.playSoundToPlayer(SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 1.0f, 1.0f);
    }

    private void complete() {
      if (!completed) {
        completed = true;
        if (pointerDisplay != null) pointerDisplay.discard();
        for (DisplayEntity.ItemDisplayEntity display : wheelDisplays) {
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

      if (ticks == nextTickTrigger && currentStepIndex < totalRotationsSteps) {
        cycleShift = (cycleShift + 1) % WHEEL_SIZE;
        currentStepIndex++;

        // Increase tick delay to simulate inertia/braking
        double progress = (double) currentStepIndex / (totalRotationsSteps - 1);
        int delayInTicks = (int) Math.max(1, (50 + Math.pow(progress * 15, 2.0)) / 50.0);
        nextTickTrigger = ticks + delayInTicks;

        // Update display nodes with item transitions
        for (int k = 0; k < WHEEL_SIZE; k++) {
          DisplayEntity.ItemDisplayEntity display = wheelDisplays.get(k);
          if (display == null) continue;

          ItemStack stack;
          if (currentStepIndex == totalRotationsSteps && k == 2) {
            stack = obtained.get(0).copy();
          } else {
            stack = allRewards.get((cycleShift + k) % allRewards.size()).copy();
          }
          display.setItemStack(stack);

          // Give a nice pulse effect to the passing nodes
          float scale = (k == 2) ? 1.4f : 1.0f;
          Quaternionf rotation = new Quaternionf().rotationY((float) Math.toRadians(-facingYaw));
          AnimationUtils.updateDisplayTransformation(
            display, wheelPositions.get(k), rotation, new Vector3f(scale, scale, scale), 2
          );
        }

        player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), SoundCategory.RECORDS, 0.8f, 1.2f);

        if (currentStepIndex == totalRotationsSteps) {
          pointerDisplay.setItemStack(new ItemStack(Items.LIME_STAINED_GLASS_PANE));

          DisplayEntity.ItemDisplayEntity winnerDisplay = wheelDisplays.get(2);
          if (winnerDisplay != null) {
            sw.spawnParticles(
              ParticleTypes.HAPPY_VILLAGER,
              winnerDisplay.getX(), winnerDisplay.getY(), winnerDisplay.getZ(),
              15, 0.25, 0.25, 0.25, 0.1
            );
          }
          player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.7f, 1.0f);

          nextTickTrigger = ticks + 30;
        }
      }

      if (currentStepIndex >= totalRotationsSteps && ticks >= nextTickTrigger) {
        this.kill();
        complete();
      }

      setTicks(ticks + 1);
    }
  }
}