package com.kingpixel.cobbleutils.Model.zones.zoneshapes;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class CuboidShape extends ZoneShape {

  public static final String TYPE = "CUBOID";

  private final BlockPos min;
  private final BlockPos max;

  // =========================
  // Constructor default
  // =========================
  public CuboidShape() {
    super(TYPE);
    this.min = new BlockPos(0, 0, 0);
    this.max = new BlockPos(1, 1, 1);
  }

  // =========================
  // Constructor principal
  // =========================
  public CuboidShape(BlockPos pos1, BlockPos pos2) {
    super(TYPE);

    if (pos1 == null || pos2 == null) {
      throw new IllegalArgumentException("CuboidShape requires two valid positions.");
    }

    int minX = Math.min(pos1.getX(), pos2.getX());
    int minY = Math.min(pos1.getY(), pos2.getY());
    int minZ = Math.min(pos1.getZ(), pos2.getZ());

    int maxX = Math.max(pos1.getX(), pos2.getX());
    int maxY = Math.max(pos1.getY(), pos2.getY());
    int maxZ = Math.max(pos1.getZ(), pos2.getZ());

    this.min = new BlockPos(minX, minY, minZ);
    this.max = new BlockPos(maxX, maxY, maxZ);
  }

  // =========================
  // Containment
  // =========================
  @Override
  public boolean contains(BlockPos pos) {

    if (pos == null) return false;

    return pos.getX() >= min.getX() && pos.getX() <= max.getX()
      && pos.getY() >= min.getY() && pos.getY() <= max.getY()
      && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
  }

  @Override
  public void spawnParticles(ServerWorld world,
                             @Nullable ServerPlayerEntity player) {

    if (world == null) return;

    final double step = 1.0;

    for (double x = min.getX(); x <= max.getX(); x += step) {
      for (double y = min.getY(); y <= max.getY(); y += step) {

        spawn(world, player, x, y, min.getZ(), ParticleTypes.END_ROD);
        spawn(world, player, x, y, max.getZ(), ParticleTypes.END_ROD);
      }
    }

    for (double z = min.getZ(); z <= max.getZ(); z += step) {
      for (double y = min.getY(); y <= max.getY(); y += step) {

        spawn(world, player, min.getX(), y, z, ParticleTypes.END_ROD);
        spawn(world, player, max.getX(), y, z, ParticleTypes.END_ROD);
      }
    }

    for (double x = min.getX(); x <= max.getX(); x += step) {
      for (double z = min.getZ(); z <= max.getZ(); z += step) {

        spawn(world, player, x, min.getY(), z, ParticleTypes.END_ROD);
        spawn(world, player, x, max.getY(), z, ParticleTypes.END_ROD);
      }
    }
  }


}