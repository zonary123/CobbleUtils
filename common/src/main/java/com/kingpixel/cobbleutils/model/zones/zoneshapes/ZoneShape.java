package com.kingpixel.cobbleutils.model.zones.zoneshapes;

import lombok.Data;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

@Data
public abstract class ZoneShape {
  private String type;

  protected ZoneShape(String type) {
    this.type = type;
  }

  public abstract void fix();

  /**
   * Checks if a position is inside the shape.
   */
  public abstract boolean contains(BlockPos pos);

  /**
   * Spawns particles.
   * <p>
   * If player is null → should render for all players in world.
   * If player is not null → should render only for that player.
   */
  public abstract void spawnParticles(
    ServerWorld world,
    @Nullable ServerPlayerEntity player
  );

  /**
   * Utility method to spawn a single particle at the center of the block.
   *
   * @param world        The world to spawn the particle in.
   * @param player       The player to show the particle to (null for all players).
   * @param x            The x coordinate of the block.
   * @param y            The y coordinate of the block.
   * @param z            The z coordinate of the block.
   * @param particleType The type of particle to spawn (null for default END_ROD).
   */
  protected void spawn(ServerWorld world,
                       @Nullable ServerPlayerEntity player,
                       double x,
                       double y,
                       double z,
                       @Nullable SimpleParticleType particleType) {
    SimpleParticleType type = particleType != null ? particleType : ParticleTypes.END_ROD;
    if (player != null) {
      world.spawnParticles(
        player,
        type,
        true,
        x + 0.5,
        y + 0.5,
        z + 0.5,
        1,
        0, 0, 0,
        0
      );
    } else {
      world.spawnParticles(
        type,
        x + 0.5,
        y + 0.5,
        z + 0.5,
        1,
        0, 0, 0,
        0
      );
    }
  }
}