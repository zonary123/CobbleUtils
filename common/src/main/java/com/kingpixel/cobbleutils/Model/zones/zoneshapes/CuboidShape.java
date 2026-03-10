package com.kingpixel.cobbleutils.Model.zones.zoneshapes;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class CuboidShape extends ZoneShape {

  public static final String TYPE = "CUBOID";

  private BlockPos min;
  private BlockPos max;

  public CuboidShape() {
    super(TYPE);
    this.min = new BlockPos(0, 0, 0);
    this.max = new BlockPos(1, 1, 1);
  }

  public CuboidShape(BlockPos pos1, BlockPos pos2) {
    super(TYPE);

    if (pos1 == null || pos2 == null) {
      throw new IllegalArgumentException("CuboidShape requires two valid positions.");
    }

    this.min = new BlockPos(
      Math.min(pos1.getX(), pos2.getX()),
      Math.min(pos1.getY(), pos2.getY()),
      Math.min(pos1.getZ(), pos2.getZ())
    );

    this.max = new BlockPos(
      Math.max(pos1.getX(), pos2.getX()),
      Math.max(pos1.getY(), pos2.getY()),
      Math.max(pos1.getZ(), pos2.getZ())
    );
  }

  @Override
  public void fix() {
    if (min == null || max == null) return;

    int minX = Math.min(min.getX(), max.getX());
    int minY = Math.min(min.getY(), max.getY());
    int minZ = Math.min(min.getZ(), max.getZ());

    int maxX = Math.max(min.getX(), max.getX());
    int maxY = Math.max(min.getY(), max.getY());
    int maxZ = Math.max(min.getZ(), max.getZ());

    min = new BlockPos(minX, minY, minZ);
    max = new BlockPos(maxX, maxY, maxZ);
  }

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

    int minX = min.getX();
    int minY = min.getY();
    int minZ = min.getZ();

    int maxX = max.getX();
    int maxY = max.getY();
    int maxZ = max.getZ();

    final int spacing = 2;

    for (int x = minX; x <= maxX; x += spacing) {
      spawn(world, player, x, minY, minZ, ParticleTypes.END_ROD);
      spawn(world, player, x, minY, maxZ, ParticleTypes.END_ROD);
      spawn(world, player, x, maxY, minZ, ParticleTypes.END_ROD);
      spawn(world, player, x, maxY, maxZ, ParticleTypes.END_ROD);
    }

    for (int y = minY; y <= maxY; y += spacing) {
      spawn(world, player, minX, y, minZ, ParticleTypes.END_ROD);
      spawn(world, player, maxX, y, minZ, ParticleTypes.END_ROD);
      spawn(world, player, minX, y, maxZ, ParticleTypes.END_ROD);
      spawn(world, player, maxX, y, maxZ, ParticleTypes.END_ROD);
    }

    for (int z = minZ; z <= maxZ; z += spacing) {
      spawn(world, player, minX, minY, z, ParticleTypes.END_ROD);
      spawn(world, player, maxX, minY, z, ParticleTypes.END_ROD);
      spawn(world, player, minX, maxY, z, ParticleTypes.END_ROD);
      spawn(world, player, maxX, maxY, z, ParticleTypes.END_ROD);
    }
  }
}