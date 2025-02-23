package com.kingpixel.cobbleutils.Model.Animations;

import com.kingpixel.cobbleutils.CobbleUtils;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Author: Carlos Varas Alonso - 16/02/2025 22:36
 */
public class CircleAnimation {
  public static void start(ServerPlayerEntity player, List<ItemStack> showRewards) {
    // TODO: Change this for Packets to player
    for (int i = 0; i < showRewards.size(); i++) {
      ItemStack showReward = showRewards.get(i);
      double angle = Math.toRadians((360.0 / showRewards.size()) * i); // Calcula el ángulo para la separación
      double radius = 2.0; // Radio de separación
      double offsetX = radius * Math.cos(angle);
      double offsetZ = radius * Math.sin(angle);
      var entity = new CircleEntity(player.getServerWorld(), player.getX() + offsetX, player.getY() + 1,
        player.getZ() + offsetZ, showReward, player);
      player.getServerWorld().spawnEntity(entity);
    }
  }

  public static class CircleEntity extends ItemEntity {
    private int ticks = 0;
    private final ServerPlayerEntity player;
    private final double initialOffsetX;
    private final double initialOffsetZ;

    public CircleEntity(World world, double x, double y, double z, ItemStack stack, ServerPlayerEntity player) {
      super(world, x, y, z, stack);
      CobbleUtils.LOGGER.info("ItemShowEntity");
      this.player = player;
      setNoGravity(true);
      setPickupDelay(Integer.MAX_VALUE);
      setInvisible(false);
      setInvulnerable(true);

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
          this.kill();
          return;
        }
        double angle = Math.toRadians((this.ticks * 2) % 360); // Incremento de ángulo más gradual
        double offsetX = initialOffsetX * Math.cos(angle) - initialOffsetZ * Math.sin(angle);
        double offsetZ = initialOffsetX * Math.sin(angle) + initialOffsetZ * Math.cos(angle);
        double targetX = this.player.getX() + offsetX;
        double targetY = this.player.getY() + 1;
        double targetZ = this.player.getZ() + offsetZ;

        // Interpola suavemente la posición y rotación
        this.lerpPosAndRotation(1, targetX, targetY, targetZ, 0, 0);

        if (this.ticks >= 160) {
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
}

