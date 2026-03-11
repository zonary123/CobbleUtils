package com.kingpixel.cobbleutils.model.zones.zoneshapes;

import com.kingpixel.cobbleutils.model.zones.Point2D;
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

  private static final int VERTICAL_SPACING = 3;
  private static final int MIN_POINTS = 20;

  private Point2D center;
  private double radius;
  private transient Double radiusSquared;
  private int minY;
  private int maxY;

  public CylinderShape() {
    super(TYPE);
    this.center = new Point2D(0, 0);
    this.radius = 1.0;
    this.minY = 0;
    this.maxY = 0;
  }

  public CylinderShape(Point2D center, double radius, int minY, int maxY) {
    super(TYPE);
    this.center = center;
    this.radius = radius;
    this.minY = Math.min(minY, maxY);
    this.maxY = Math.max(minY, maxY);
  }

  public double getRadiusSquared() {
    if (radiusSquared == null) {
      radiusSquared = radius * radius;
    }
    return radiusSquared;
  }

  @Override
  public void fix() {
    if (center == null) center = new Point2D(0, 0);
    if (radius < 0) radius = -radius;
    if (minY > maxY) {
      int temp = minY;
      minY = maxY;
      maxY = temp;
    }
    radiusSquared = null; // reset cache
  }

  @Override
  public boolean contains(BlockPos pos) {
    if (pos == null) return false;

    int y = pos.getY();
    if (y < minY || y > maxY) return false;

    double dx = pos.getX() - center.x();
    double dz = pos.getZ() - center.z();

    return (dx * dx + dz * dz) <= getRadiusSquared();
  }

  @Override
  public void spawnParticles(ServerWorld world,
                             @Nullable ServerPlayerEntity player) {

    if (world == null) return;

    double cx = center.x();
    double cz = center.z();
    int points = Math.max(MIN_POINTS, (int) (radius * 6));
    double angleStep = (2 * Math.PI) / points;

    // Top & bottom circles
    drawCircle(world, player, minY, cx, cz, points, angleStep);
    drawCircle(world, player, maxY, cx, cz, points, angleStep);

    // Walls
    drawWalls(world, player, cx, cz, points, angleStep);
  }

  private void drawCircle(ServerWorld world,
                          @Nullable ServerPlayerEntity player,
                          int y, double cx, double cz,
                          int points, double angleStep) {

    for (int i = 0; i < points; i++) {
      double angle = i * angleStep;
      double x = cx + radius * Math.cos(angle);
      double z = cz + radius * Math.sin(angle);
      spawn(world, player, x, y, z, ParticleTypes.END_ROD);
    }
  }

  private void drawWalls(ServerWorld world,
                         @Nullable ServerPlayerEntity player,
                         double cx, double cz,
                         int points, double angleStep) {

    for (int i = 0; i < points; i++) {
      double angle = i * angleStep;
      double x = cx + radius * Math.cos(angle);
      double z = cz + radius * Math.sin(angle);

      for (int y = minY; y <= maxY; y += VERTICAL_SPACING) {
        spawn(world, player, x, y, z, ParticleTypes.END_ROD);
      }
    }
  }
}