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

  private final List<Point2D> points;
  private final int minY;
  private final int maxY;

  private transient Integer minX;
  private transient Integer maxX;
  private transient Integer minZ;
  private transient Integer maxZ;

  public PolygonShape() {
    super(TYPE);
    this.points = List.of(
      new Point2D(0, 0),
      new Point2D(1, 0),
      new Point2D(1, 1)
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

  private void ensureBounds() {

    if (minX != null) return;

    int minXTemp = Integer.MAX_VALUE;
    int maxXTemp = Integer.MIN_VALUE;
    int minZTemp = Integer.MAX_VALUE;
    int maxZTemp = Integer.MIN_VALUE;

    for (Point2D p : points) {

      int px = p.x();
      int pz = p.z();

      if (px < minXTemp) minXTemp = px;
      if (px > maxXTemp) maxXTemp = px;
      if (pz < minZTemp) minZTemp = pz;
      if (pz > maxZTemp) maxZTemp = pz;
    }

    this.minX = minXTemp;
    this.maxX = maxXTemp;
    this.minZ = minZTemp;
    this.maxZ = maxZTemp;
  }

  @Override
  public boolean contains(BlockPos pos) {

    if (pos == null) return false;

    ensureBounds();

    int x = pos.getX();
    int y = pos.getY();
    int z = pos.getZ();

    if (y < minY || y > maxY) return false;

    if (x < minX || x > maxX || z < minZ || z > maxZ)
      return false;

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

  @Override
  public void spawnParticles(ServerWorld world,
                             @Nullable ServerPlayerEntity player) {

    if (world == null || points.isEmpty()) return;

    ensureBounds();

    int wallVerticalStep = 3;
    int wallSteps = 30;
    int fillStep = 2;

    drawTopFilled(world, player, maxY, fillStep);

    for (int i = 0; i < points.size(); i++) {

      Point2D p1 = points.get(i);
      Point2D p2 = points.get((i + 1) % points.size());

      drawWall(world, player, p1, p2, wallVerticalStep, wallSteps);
    }
  }

  private void drawTopFilled(ServerWorld world,
                             @Nullable ServerPlayerEntity player,
                             int y,
                             int step) {

    for (int x = minX; x <= maxX; x += step) {
      for (int z = minZ; z <= maxZ; z += step) {

        BlockPos pos = new BlockPos(x, y, z);

        if (contains(pos)) {
          spawn(world, player, x, y, z, ParticleTypes.HAPPY_VILLAGER);
        }
      }
    }
  }

  private void drawWall(ServerWorld world,
                        @Nullable ServerPlayerEntity player,
                        Point2D p1,
                        Point2D p2,
                        int verticalStep,
                        int horizontalSteps) {

    for (int s = 0; s <= horizontalSteps; s++) {

      double t = (double) s / horizontalSteps;

      double x = p1.x() + (p2.x() - p1.x()) * t;
      double z = p1.z() + (p2.z() - p1.z()) * t;

      for (int y = minY; y <= maxY; y += verticalStep) {
        spawn(world, player, x, y, z, ParticleTypes.END_ROD);
      }
    }
  }
}