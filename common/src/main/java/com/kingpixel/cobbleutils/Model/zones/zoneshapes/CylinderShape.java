package com.kingpixel.cobbleutils.Model.zones.zoneshapes;

import com.kingpixel.cobbleutils.Model.zones.Point2D;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

@EqualsAndHashCode(callSuper = true)
@Data
public class CylinderShape extends ZoneShape {

  public static final String TYPE = "CYLINDER";

  private final Point2D center;
  private final double radius;
  private transient Double radiusSquared;
  private final int minY;
  private final int maxY;

  // =========================
  // Default constructor
  // =========================
  public CylinderShape() {
    super(TYPE);
    this.center = new Point2D(0, 0);
    this.radius = 1.0;
    this.minY = 0;
    this.maxY = 0;
  }

  // =========================
  // Main constructor
  // =========================
  public CylinderShape(Point2D center, double radius, int minY, int maxY) {
    super(TYPE);
    this.center = center;
    this.radius = radius;
    this.minY = Math.min(minY, maxY);
    this.maxY = Math.max(minY, maxY);
  }

  // =========================
  // Cached radius²
  // =========================
  public double getRadiusSquared() {
    if (radiusSquared == null) {
      radiusSquared = radius * radius;
    }
    return radiusSquared;
  }

  // =========================
  // Containment check
  // =========================
  @Override
  public boolean contains(BlockPos pos) {
    if (pos == null) return false;

    int y = pos.getY();
    if (y < minY || y > maxY) return false;

    double dx = pos.getX() - center.x();
    double dz = pos.getZ() - center.z();

    return (dx * dx + dz * dz) <= getRadiusSquared();
  }

  // =========================
  // Visual Representation
  // =========================
  @Override
  public void spawnParticles(ServerWorld world,
                             @Nullable ServerPlayerEntity player) {

    if (world == null) return;

    drawCircle(world, player, minY, ParticleTypes.END_ROD);

    drawCircle(world, player, maxY, ParticleTypes.HAPPY_VILLAGER);

    drawWalls(world, player);
  }

  // =========================
  // Draw Circle
  // =========================
  private void drawCircle(ServerWorld world,
                          @Nullable ServerPlayerEntity player,
                          int y,
                          SimpleParticleType particle) {

    double angleStep = (2 * Math.PI) / 100;
    double centerX = center.x();
    double centerZ = center.z();

    for (int i = 0; i < 100; i++) {

      double angle = i * angleStep;

      double x = centerX + radius * Math.cos(angle);
      double z = centerZ + radius * Math.sin(angle);

      spawn(world, player, x, y, z, particle);
    }
  }

  // =========================
  // Draw Vertical Walls
  // =========================
  private void drawWalls(ServerWorld world,
                         @Nullable ServerPlayerEntity player) {

    double angleStep = (2 * Math.PI) / 100;
    double centerX = center.x();
    double centerZ = center.z();

    for (int i = 0; i < 100; i++) {

      double angle = i * angleStep;

      double x = centerX + radius * Math.cos(angle);
      double z = centerZ + radius * Math.sin(angle);

      for (int y = minY; y <= maxY; y += 3) {
        spawn(world, player, x, y, z, ParticleTypes.END_ROD);
      }
    }
  }
}