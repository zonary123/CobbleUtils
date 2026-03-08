package com.kingpixel.cobbleutils.Model.zones.zoneshapes;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

@EqualsAndHashCode(callSuper = true)
@Data
public class SphereShape extends ZoneShape {

  public static final String TYPE = "SPHERE";

  private static final double MAX_RENDER_DISTANCE = 128.0;

  private BlockPos center;
  private double radius;
  private transient Double radiusSquared;

  public SphereShape() {
    super(TYPE);
    this.center = new BlockPos(0, 0, 0);
    this.radius = 1.0;
  }

  public SphereShape(BlockPos center, double radius) {
    super(TYPE);

    if (center == null)
      throw new IllegalArgumentException("SphereShape requires a valid center.");

    if (radius <= 0)
      throw new IllegalArgumentException("Radius must be > 0");

    this.center = center;
    this.radius = radius;
  }

  public double getRadiusSquared() {
    if (radiusSquared == null) {
      radiusSquared = radius * radius;
    }
    return radiusSquared;
  }

  @Override
  public void fix() {
    if (center == null) {
      center = new BlockPos(0, 0, 0);
    }

    if (radius < 0) {
      radius = -radius;
    }

    radiusSquared = null;
  }

  @Override
  public boolean contains(BlockPos pos) {

    if (pos == null) return false;

    double cx = center.getX() + 0.5;
    double cy = center.getY() + 0.5;
    double cz = center.getZ() + 0.5;

    double px = pos.getX() + 0.5;
    double py = pos.getY() + 0.5;
    double pz = pos.getZ() + 0.5;

    double dx = px - cx;
    double dy = py - cy;
    double dz = pz - cz;

    return (dx * dx + dy * dy + dz * dz) <= getRadiusSquared();
  }

  @Override
  public void spawnParticles(ServerWorld world,
                             @Nullable ServerPlayerEntity player) {

    if (world == null) return;

    double cx = center.getX() + 0.5;
    double cy = center.getY() + 0.5;
    double cz = center.getZ() + 0.5;

    if (player != null) {
      if (player.squaredDistanceTo(cx, cy, cz)
        > MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE)
        return;
    }

    int steps = Math.max(8, (int) (radius * 1.5));
    steps = Math.min(steps, 25);

    double phiStep = Math.PI / steps;
    double thetaStep = (2 * Math.PI) / steps;

    for (int i = 0; i <= steps; i++) {

      double phi = i * phiStep;

      for (int j = 0; j <= steps * 2; j++) {

        double theta = j * thetaStep;

        double x = cx + radius * Math.sin(phi) * Math.cos(theta);
        double y = cy + radius * Math.cos(phi);
        double z = cz + radius * Math.sin(phi) * Math.sin(theta);

        spawn(world, player, x, y, z, ParticleTypes.END_ROD);
      }
    }
  }
}