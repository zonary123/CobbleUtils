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

    // Renderizar cada 4 ticks (opcional pero recomendado)
    if (world.getTime() % 4 != 0) return;

    final int spacing = 2; // Cada cuantos bloques se spawnea una partícula

    int minX = min.getX();
    int minY = min.getY();
    int minZ = min.getZ();

    int maxX = max.getX();
    int maxY = max.getY();
    int maxZ = max.getZ();

    // =========================
    // 1️⃣ Aristas paralelas al eje X
    // =========================
    for (int x = minX; x <= maxX; x += spacing) {

      spawn(world, player, x, minY, minZ, ParticleTypes.END_ROD);
      spawn(world, player, x, minY, maxZ, ParticleTypes.END_ROD);
      spawn(world, player, x, maxY, minZ, ParticleTypes.END_ROD);
      spawn(world, player, x, maxY, maxZ, ParticleTypes.END_ROD);
    }

    // =========================
    // 2️⃣ Aristas paralelas al eje Y
    // =========================
    for (int y = minY; y <= maxY; y += spacing) {

      spawn(world, player, minX, y, minZ, ParticleTypes.END_ROD);
      spawn(world, player, maxX, y, minZ, ParticleTypes.END_ROD);
      spawn(world, player, minX, y, maxZ, ParticleTypes.END_ROD);
      spawn(world, player, maxX, y, maxZ, ParticleTypes.END_ROD);
    }

    // =========================
    // 3️⃣ Aristas paralelas al eje Z
    // =========================
    for (int z = minZ; z <= maxZ; z += spacing) {

      spawn(world, player, minX, minY, z, ParticleTypes.END_ROD);
      spawn(world, player, maxX, minY, z, ParticleTypes.END_ROD);
      spawn(world, player, minX, maxY, z, ParticleTypes.END_ROD);
      spawn(world, player, maxX, maxY, z, ParticleTypes.END_ROD);
    }
  }


}