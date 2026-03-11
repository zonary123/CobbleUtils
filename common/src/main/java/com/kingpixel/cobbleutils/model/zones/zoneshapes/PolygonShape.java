package com.kingpixel.cobbleutils.model.zones.zoneshapes;

import com.kingpixel.cobbleutils.model.zones.Point2D;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PolygonShape extends ZoneShape {

  public static final String TYPE = "POLYGON";

  private static final double HORIZONTAL_SPACING = 1.5;
  private static final int VERTICAL_SPACING = 3;
  private static final double MAX_RENDER_DISTANCE = 128.0;

  private List<Point2D> points;
  private int minY;
  private int maxY;

  public PolygonShape() {
    super(TYPE);
    this.points = List.of(
      new Point2D(0, 0),
      new Point2D(5, 0),
      new Point2D(5, 5)
    );
    this.minY = 0;
    this.maxY = 255;
  }

  public PolygonShape(List<Point2D> points, int minY, int maxY) {
    super(TYPE);

    if (points == null || points.size() < 3) {
      throw new IllegalArgumentException("PolygonShape requires at least 3 points.");
    }

    this.points = List.copyOf(points);
    this.minY = Math.min(minY, maxY);
    this.maxY = Math.max(minY, maxY);
  }

  @Override
  public void fix() {
    if (points == null || points.size() < 3) {
      throw new IllegalStateException("PolygonShape must have at least 3 points.");
    }

    if (minY > maxY) {
      int temp = minY;
      minY = maxY;
      maxY = temp;
    }
  }

  @Override
  public boolean contains(BlockPos pos) {
    if (pos == null || points == null) return false;

    int x = pos.getX();
    int y = pos.getY();
    int z = pos.getZ();

    if (y < minY || y > maxY) return false;

    boolean inside = false;
    int size = points.size();

    for (int i = 0, j = size - 1; i < size; j = i++) {

      Point2D pi = points.get(i);
      Point2D pj = points.get(j);

      if ((pi.z() > z) != (pj.z() > z)) {

        double intersectionX =
          pi.x() + (double) (pj.x() - pi.x()) *
            (z - pi.z()) / (double) (pj.z() - pi.z());

        if (x < intersectionX) {
          inside = !inside;
        }
      }
    }

    return inside;
  }

  @Override
  public void spawnParticles(ServerWorld world,
                             @Nullable ServerPlayerEntity player) {

    if (world == null || points == null) return;

    if (player != null) {
      double centerX = points.stream().mapToDouble(Point2D::x).average().orElse(0);
      double centerZ = points.stream().mapToDouble(Point2D::z).average().orElse(0);
      double midY = (minY + maxY) / 2.0;

      if (player.squaredDistanceTo(centerX, midY, centerZ)
        > MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE) {
        return;
      }
    }

    drawOutline(world, player, maxY);
    drawWalls(world, player);
  }

  private void drawOutline(ServerWorld world,
                           @Nullable ServerPlayerEntity player,
                           int y) {

    for (int i = 0; i < points.size(); i++) {

      Point2D p1 = points.get(i);
      Point2D p2 = points.get((i + 1) % points.size());

      drawSegment(world, player, p1, p2, y);
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

      spawn(world, player,
        p1.x() + dx * t,
        y,
        p1.z() + dz * t,
        ParticleTypes.HAPPY_VILLAGER);
    }
  }

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