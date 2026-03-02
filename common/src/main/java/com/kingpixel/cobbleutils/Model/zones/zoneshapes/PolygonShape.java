package com.kingpixel.cobbleutils.Model.zones.zoneshapes;

import com.kingpixel.cobbleutils.Model.zones.Point2D;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PolygonShape extends ZoneShape {

  public static final String TYPE = "POLYGON";

  private static final int PARTICLE_TICK_INTERVAL = 4;
  private static final int VERTICAL_SPACING = 3;
  private static final double HORIZONTAL_SPACING = 1.5;
  private static final double MAX_RENDER_DISTANCE = 128.0;

  private final List<Point2D> points;
  private final int minY;
  private final int maxY;

  // 🔥 Centro cacheado
  private final double centerX;
  private final double centerZ;

  public PolygonShape() {
    super(TYPE);

    this.points = List.of(
      new Point2D(0, 0),
      new Point2D(5, 0),
      new Point2D(5, 5)
    );

    this.minY = 0;
    this.maxY = 255;

    // Cache center
    double[] center = computeCenter();
    this.centerX = center[0];
    this.centerZ = center[1];
  }

  public PolygonShape(List<Point2D> points, int minY, int maxY) {
    super(TYPE);

    if (points == null || points.size() < 3) {
      throw new IllegalArgumentException("PolygonShape requires at least 3 points.");
    }

    this.points = List.copyOf(points);
    this.minY = Math.min(minY, maxY);
    this.maxY = Math.max(minY, maxY);

    // Cache center una sola vez
    double[] center = computeCenter();
    this.centerX = center[0];
    this.centerZ = center[1];
  }

  // =========================
  // Centro calculado una vez
  // =========================
  private double[] computeCenter() {

    double sumX = 0;
    double sumZ = 0;

    for (Point2D p : points) {
      sumX += p.x();
      sumZ += p.z();
    }

    double size = points.size();

    return new double[]{
      sumX / size,
      sumZ / size
    };
  }

  // =========================
  // Containment (sin cambios)
  // =========================
  @Override
  public boolean contains(BlockPos pos) {

    if (pos == null) return false;

    int x = pos.getX();
    int y = pos.getY();
    int z = pos.getZ();

    if (y < minY || y > maxY) return false;

    boolean inside = false;
    int size = points.size();

    for (int i = 0, j = size - 1; i < size; j = i++) {

      Point2D pi = points.get(i);
      Point2D pj = points.get(j);

      int xi = pi.x();
      int zi = pi.z();
      int xj = pj.x();
      int zj = pj.z();

      if ((zi > z) != (zj > z)) {

        double intersectionX =
          xi + (double) (xj - xi) * (z - zi) / (double) (zj - zi);

        if (x < intersectionX) {
          inside = !inside;
        }
      }
    }

    return inside;
  }

  // =========================
  // Render optimizado
  // =========================
  @Override
  public void spawnParticles(ServerWorld world,
                             @Nullable ServerPlayerEntity player) {

    if (world == null || points.isEmpty()) return;

    // 🔥 Throttling
    if (world.getTime() % PARTICLE_TICK_INTERVAL != 0) return;

    // 🔥 LOD por distancia
    if (player != null) {
      double midY = (minY + maxY) / 2.0;

      if (player.squaredDistanceTo(centerX, midY, centerZ)
        > MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE)
        return;
    }

    drawTopOutline(world, player);
    drawWalls(world, player);
  }

  // =========================
  // Contorno superior
  // =========================
  private void drawTopOutline(ServerWorld world,
                              @Nullable ServerPlayerEntity player) {

    for (int i = 0; i < points.size(); i++) {

      Point2D p1 = points.get(i);
      Point2D p2 = points.get((i + 1) % points.size());

      drawSegment(world, player, p1, p2, maxY);
    }
  }

  private void drawSegment(ServerWorld world,
                           @Nullable ServerPlayerEntity player,
                           Point2D p1,
                           Point2D p2,
                           int y) {

    double dx = p2.x() - p1.x();
    double dz = p2.z() - p1.z();
    double length = Math.sqrt(dx * dx + dz * dz);

    int steps = Math.max(1, (int) (length / HORIZONTAL_SPACING));

    for (int s = 0; s <= steps; s++) {

      double t = (double) s / steps;

      double x = p1.x() + dx * t;
      double z = p1.z() + dz * t;

      spawn(world, player, x, y, z, ParticleTypes.HAPPY_VILLAGER);
    }
  }

  // =========================
  // Paredes verticales
  // =========================
  private void drawWalls(ServerWorld world,
                         @Nullable ServerPlayerEntity player) {

    for (int i = 0; i < points.size(); i++) {

      Point2D p1 = points.get(i);
      Point2D p2 = points.get((i + 1) % points.size());

      double dx = p2.x() - p1.x();
      double dz = p2.z() - p1.z();
      double length = Math.sqrt(dx * dx + dz * dz);

      int steps = Math.max(1, (int) (length / HORIZONTAL_SPACING));

      for (int s = 0; s <= steps; s++) {

        double t = (double) s / steps;

        double x = p1.x() + dx * t;
        double z = p1.z() + dz * t;

        for (int y = minY; y <= maxY; y += VERTICAL_SPACING) {
          spawn(world, player, x, y, z, ParticleTypes.END_ROD);
        }
      }
    }
  }
}