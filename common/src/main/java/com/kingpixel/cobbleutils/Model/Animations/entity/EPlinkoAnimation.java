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
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class EPlinkoAnimation extends Animation {

  @Override
  public void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                    List<ItemStack> allRewards, Runnable onComplete) {
    if (obtained.isEmpty()) {
      if (onComplete != null) onComplete.run();
      return;
    }

    Vec3d center = AnimationUtils.getPosition(player, position);

    CobbleUtils.server.executeSync(() -> {
      PlinkoController controller = new PlinkoController(
        player.getServerWorld(), center.x, center.y, center.z,
        obtained, allRewards, player, onComplete
      );
      player.getServerWorld().spawnEntity(controller);
    });
  }

  public static class PlinkoChip {
    public final CustomItemDisplayEntity display;
    public Vec3d pos;
    public double velX = 0;
    public final double velY = -0.07;
    public int nextBounceRow = 1;
    public boolean landed = false;
    public int landTicks = 0;
    public final ItemStack finalReward;
    public int chipTicks = 0;

    public PlinkoChip(CustomItemDisplayEntity display, Vec3d pos, ItemStack finalReward) {
      this.display = display;
      this.pos = pos;
      this.finalReward = finalReward;
    }
  }

  public static class PlinkoController extends CustomArmorStandEntity {
    private final ServerPlayerEntity player;
    private final List<ItemStack> obtained;
    private final List<ItemStack> allRewards;
    private final Runnable onDestroy;
    private boolean completed = false;

    private Vec3d boardCenter;
    private Vec3d perpendicular;
    private float facingYaw;

    private int currentItemIndex = 0;
    private int spawnCooldownTicks = 0;
    private final List<PlinkoChip> activeChips = new ArrayList<>();

    public PlinkoController(World world, double x, double y, double z,
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

      initPlinkoBoardSetup();
    }

    private void initPlinkoBoardSetup() {
      Vec3d lookVec = Vec3d.fromPolar(0, player.getYaw()).normalize();
      this.boardCenter = this.getPos().add(lookVec.multiply(3.0)).add(0, 3.2, 0);
      this.perpendicular = new Vec3d(-lookVec.z, 0, lookVec.x).normalize();
      this.facingYaw = AnimationUtils.getYawToFacePlayer(player, boardCenter);

      player.playSoundToPlayer(SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 1.0f, 1.0f);
    }

    @Override public void complete() {
      if (!completed) {
        completed = true;
        for (PlinkoChip chip : activeChips) {
          if (chip.display != null) {
            chip.display.discard();
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

      ServerWorld sw = (ServerWorld) this.getWorld();
      ThreadLocalRandom random = ThreadLocalRandom.current();

      // 1. Draw Plinko Pins using End Rod particles
      for (int row = 1; row <= 4; row++) {
        double rowY = this.boardCenter.y + 2.0 - (row * 0.9);
        int pinsInRow = row + 1;
        for (int p = 0; p < pinsInRow; p++) {
          double offsetScalar = (p - (pinsInRow - 1) / 2.0) * 0.7;
          Vec3d pinPos = this.boardCenter.add(perpendicular.multiply(offsetScalar)).add(0, rowY - this.boardCenter.y, 0);
          sw.spawnParticles(ParticleTypes.END_ROD, pinPos.x, pinPos.y, pinPos.z, 1, 0, 0, 0, 0);
        }
      }

      // 2. Spawn next chip if cooldown allows
      if (currentItemIndex < obtained.size()) {
        if (spawnCooldownTicks <= 0) {
          ItemStack reward = obtained.get(currentItemIndex);
          Vec3d spawnPos = this.boardCenter.add(0, 2.0, 0);

          ItemStack initialRandomStack = allRewards.get(random.nextInt(allRewards.size()));
          CustomItemDisplayEntity display = AnimationUtils.spawnItemDisplay(
            sw, spawnPos, initialRandomStack.copy(), new Vector3f(1.0f, 1.0f, 1.0f), facingYaw, 0
          );

          PlinkoChip chip = new PlinkoChip(display, spawnPos, reward);
          activeChips.add(chip);

          currentItemIndex++;
          spawnCooldownTicks = 25;
        } else {
          spawnCooldownTicks--;
        }
      }

      // 3. Central Tick Loop for all active Plinko Chips
      for (int i = activeChips.size() - 1; i >= 0; i--) {
        PlinkoChip chip = activeChips.get(i);
        chip.chipTicks++;

        if (!chip.landed) {
          double nextY = chip.pos.y + chip.velY;
          double nextX = chip.pos.x + (perpendicular.x * chip.velX);
          double nextZ = chip.pos.z + (perpendicular.z * chip.velX);

          chip.pos = new Vec3d(nextX, nextY, nextZ);

          //Twinkle texture every 3 ticks
          if (chip.chipTicks % 3 == 0) {
            ItemStack randomStack = allRewards.get(random.nextInt(allRewards.size()));
            chip.display.setItemStack(randomStack.copy());
          }

          double currentRelativeY = this.boardCenter.y + 2.0 - chip.pos.y;
          int expectedRow = (int) (currentRelativeY / 0.9);

          // Handle pin bounce logic
          if (expectedRow == chip.nextBounceRow && chip.nextBounceRow <= 4) {
            chip.velX = random.nextBoolean() ? 0.13 : -0.13;
            chip.nextBounceRow++;

            player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), SoundCategory.RECORDS, 0.7f, 1.4f);
            sw.spawnParticles(ParticleTypes.CRIT, chip.pos.x, chip.pos.y, chip.pos.z, 3, 0.05, 0.05, 0.05, 0.02);
          }

          chip.velX *= 0.93;

          // Interpolated transform update
          Quaternionf rotation = new Quaternionf().rotationY((float) Math.toRadians(-facingYaw));
          AnimationUtils.updateDisplayTransformation(
            chip.display, chip.pos, rotation, new Vector3f(1.0f, 1.0f, 1.0f), 2
          );

          // Handle board bottom land
          if (expectedRow >= 5) {
            chip.landed = true;
            chip.display.setItemStack(chip.finalReward.copy());
            sw.spawnParticles(
              ParticleTypes.TOTEM_OF_UNDYING,
              chip.pos.x, chip.pos.y, chip.pos.z,
              12, 0.1, 0.1, 0.1, 0.08
            );
            player.playSoundToPlayer(SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.6f, 1.2f);
          }
        } else {
          chip.landTicks++;
          if (chip.landTicks >= 20) {
            chip.display.discard();
            activeChips.remove(i);
          }
        }
      }

      if (currentItemIndex >= obtained.size() && activeChips.isEmpty()) {
        player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.7f, 1.0f);
        this.kill();
        complete();
      }
    }
  }
}