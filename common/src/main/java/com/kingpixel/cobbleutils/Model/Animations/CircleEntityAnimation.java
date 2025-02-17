package com.kingpixel.cobbleutils.Model.Animations;

import com.kingpixel.cobbleutils.CobbleUtils;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Author: Carlos Varas Alonso - 16/02/2025 22:36
 */
public class CircleEntityAnimation extends ItemEntity {
  private int ticks = 0;
  private final ServerPlayerEntity player;
  private final double initialOffsetX;
  private final double initialOffsetZ;

  public CircleEntityAnimation(World world, double x, double y, double z, ItemStack stack, ServerPlayerEntity player) {
    super(world, x, y, z, stack);
    CobbleUtils.LOGGER.info("ItemShowEntity");
    this.player = player;
    setNoGravity(true);
    setPickupDelay(Integer.MAX_VALUE);
    setInvisible(false);
    setInvulnerable(true);
    setVelocity(Vec3d.ZERO);

    // Calculate initial offsets
    Vec3d playerPos = player.getPos();
    this.initialOffsetX = x - playerPos.x;
    this.initialOffsetZ = z - playerPos.z;
  }

  @Override
  public void tick() {
    try {
      super.tick();

      if (this.player == null || this.player.isRemoved() || !this.isAlive()) {
        CobbleUtils.LOGGER.info("Kill");
        this.kill();
        return;
      }

      if (ticks % 10 == 0) {
        double angle = Math.toRadians((this.ticks * 2) % 360); // Incremento de ángulo más gradual
        double offsetX = initialOffsetX * Math.cos(angle) - initialOffsetZ * Math.sin(angle);
        double offsetZ = initialOffsetX * Math.sin(angle) + initialOffsetZ * Math.cos(angle);
        Vec3d itemPos = new Vec3d(player.getPos().x + offsetX, player.getPos().y + 1, player.getPos().z + offsetZ);
        setPos(itemPos.x, itemPos.y, itemPos.z);
      }

      if (this.ticks >= 160) {
        CobbleUtils.LOGGER.info("Kill");
        this.kill();
      }
      this.ticks++;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public boolean shouldSave() {
    return false;
  }
}