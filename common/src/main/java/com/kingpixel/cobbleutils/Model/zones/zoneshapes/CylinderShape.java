package com.kingpixel.cobbleutils.Model.zones.zoneshapes;

import com.kingpixel.cobbleutils.Model.zones.Point2D;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

@EqualsAndHashCode(callSuper = true)
@Data
public class CylinderShape extends ZoneShape {

  public static final String TYPE = "CYLINDER";

  private static final int PARTICLE_TICK_INTERVAL = 4;   // Render cada X ticks
  private static final int VERTICAL_SPACING = 3;         // Espaciado vertical
  private static final int MIN_POINTS = 20;              // Mínimo puntos del círculo

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

    // Throttle por ticks
    if (world.getTime() % PARTICLE_TICK_INTERVAL != 0) return;

    // Si es preview por jugador, no renderizar si está muy lejos
    if (player != null) {
      double midY = (minY + maxY) / 2.0;
      double maxDistance = 128.0;

      if (player.squaredDistanceTo(center.x(), midY, center.z()) > maxDistance * maxDistance)
        return;
    }

    drawCircle(world, player, minY);
    drawCircle(world, player, maxY);
    drawWalls(world, player);
  }

  // =========================
  // Draw Top/Bottom Circle
  // =========================
  private void drawCircle(ServerWorld world,
                          @Nullable ServerPlayerEntity player,
                          int y) {

    double centerX = center.x();
    double centerZ = center.z();

    int points = Math.max(MIN_POINTS, (int) (radius * 6));
    double angleStep = (2 * Math.PI) / points;

    for (int i = 0; i < points; i++) {

      double angle = i * angleStep;

      double x = centerX + radius * Math.cos(angle);
      double z = centerZ + radius * Math.sin(angle);

      spawn(world, player, x, y, z, ParticleTypes.END_ROD);
    }
  }

  // =========================
  // Draw Vertical Walls
  // =========================
  private void drawWalls(ServerWorld world,
                         @Nullable ServerPlayerEntity player) {

    double centerX = center.x();
    double centerZ = center.z();

    int points = Math.max(MIN_POINTS, (int) (radius * 6));
    double angleStep = (2 * Math.PI) / points;

    for (int i = 0; i < points; i++) {

      double angle = i * angleStep;

      double x = centerX + radius * Math.cos(angle);
      double z = centerZ + radius * Math.sin(angle);

      for (int y = minY; y <= maxY; y += VERTICAL_SPACING) {
        spawn(world, player, x, y, z, ParticleTypes.END_ROD);
      }
    }
  }
}