package com.kingpixel.cobbleutils.Model;

import com.cobblemon.mod.common.net.messages.client.effect.SpawnSnowstormParticlePacket;
import com.kingpixel.cobbleutils.CobbleUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 06/11/2024 22:41
 */
@Getter
@Setter
@ToString
public class Particle {
  private String particle;
  private int numberParticles;
  private Integer offsetX;
  private Integer offsetY;
  private Integer offsetZ;
  private Integer speed;
  private Double radius;

  public Particle() {
    this.particle = "minecraft:flame";
    this.numberParticles = 25;
    this.offsetX = 1;
    this.offsetY = 1;
    this.offsetZ = 1;
    this.speed = 0;
    this.radius = 32.0;
  }

  public Particle(boolean isNeedNearPlayers) {
    // Legacy constructor kept for backwards compatibility.
    this("minecraft:flame", 1);
  }

  public Particle(String particle) {
    this.particle = particle;
    this.numberParticles = 1;
    this.offsetX = 1;
    this.offsetY = 1;
    this.offsetZ = 1;
    this.speed = 0;
    this.radius = 32.0;
  }

  public Particle(String particle, int numberParticles) {
    this.particle = particle;
    this.numberParticles = numberParticles;
    this.offsetX = 1;
    this.offsetY = 1;
    this.offsetZ = 1;
    this.speed = 0;
    this.radius = 32.0;
  }

  public void sendParticles(@NotNull ServerPlayerEntity player, @NotNull List<Entity> entities) {
    try {
      if (isParticleBlank()) return;
      entities.forEach(entity -> sendParticles(player, entity));
    } catch (NoSuchMethodError | RuntimeException e) {
      CobbleUtils.LOGGER_RAW.warn("Failed to send particles: {}", e.getMessage());
    }
  }

  public void sendParticles(@NotNull ServerPlayerEntity player, @NotNull Entity entity) {
    try {
      if (isParticleBlank()) return;
      if (isCobblemonParticle()) {
        sendCobblemonParticle(player, entity);
      } else {
        player.networkHandler.sendPacket(getParticleS2CPacket(entity, getVanillaParticleType()));
      }
    } catch (NoSuchMethodError | RuntimeException e) {
      CobbleUtils.LOGGER_RAW.warn("Failed to send particle '{}': {}", getParticle(), e.getMessage());
    }
  }

  /**
   * Sends a Cobblemon Snowstorm (Bedrock-format) particle to the player at the entity's position.
   * Used when the particle namespace is "cobblemon".
   *
   * <p>Example particle IDs:
   * <ul>
   *   <li>{@code cobblemon:evo_sparkle}</li>
   *   <li>{@code cobblemon:generic/shiny_ring}</li>
   *   <li>{@code cobblemon:generic/ailments/burn_actor}</li>
   * </ul>
   */
  private void sendCobblemonParticle(@NotNull ServerPlayerEntity player, @NotNull Entity entity) {
    Identifier identifier = Identifier.tryParse(getParticle());
    if (identifier == null) return;
    Entity target = entity.getVehicle() != null ? entity.getVehicle() : entity;
    new SpawnSnowstormParticlePacket(identifier, target.getPos()).sendToPlayer(player);
  }

  /**
   * Returns true if the configured particle ID belongs to the "cobblemon" namespace,
   * meaning it should be rendered via Cobblemon's Snowstorm particle system
   * instead of the vanilla {@link ParticleS2CPacket}.
   */
  private boolean isCobblemonParticle() {
    if (isParticleBlank()) return false;
    Identifier id = Identifier.tryParse(getParticle());
    return id != null && "cobblemon".equals(id.getNamespace());
  }

  private @NotNull ParticleEffect getVanillaParticleType() {
    try {
      if (isParticleBlank()) return ParticleTypes.LAVA;

      Identifier identifier = Identifier.tryParse(getParticle());
      if (identifier == null) return ParticleTypes.LAVA;

      if (Registries.PARTICLE_TYPE.get(identifier) instanceof ParticleEffect particleType) {
        return particleType;
      }

      CobbleUtils.LOGGER_RAW.warn("Unknown vanilla particle '{}', falling back to lava.", getParticle());
      return ParticleTypes.LAVA;
    } catch (NoSuchMethodError | RuntimeException e) {
      CobbleUtils.LOGGER_RAW.warn("Error resolving particle '{}': {}", getParticle(), e.getMessage());
      return ParticleTypes.LAVA;
    }
  }

  public void sendParticlesNearPlayers(@NotNull Entity entity) {
    try {
      if (isParticleBlank()) return;
      List<ServerPlayerEntity> players = entity.getWorld().getEntitiesByClass(ServerPlayerEntity.class,
        Box.from(entity.getPos()).expand(radius == null ? 32 : radius), p -> true);
      if (players != null && !players.isEmpty()) {
        players.forEach(player -> sendParticles(player, entity));
      }
    } catch (NoSuchMethodError | RuntimeException e) {
      CobbleUtils.LOGGER_RAW.warn("Failed to send particles near entity: {}", e.getMessage());
    }
  }

  private boolean isParticleBlank() {
    return getParticle() == null || getParticle().isBlank();
  }

  // Privates
  private @NotNull ParticleS2CPacket getParticleS2CPacket(@NotNull Entity entity,
                                                          @NotNull ParticleEffect particleType) {

    int offsetX = this.getOffsetX() == null ? 0 : this.getOffsetX();
    int offsetY = this.getOffsetY() == null ? 0 : this.getOffsetY();
    int offsetZ = this.getOffsetZ() == null ? 0 : this.getOffsetZ();
    int speed = this.getSpeed() == null ? 0 : this.getSpeed();
    Entity finalEntity = entity;
    if (entity.getVehicle() != null) {
      finalEntity = entity.getVehicle();
    }

    return new ParticleS2CPacket(particleType, true,
      finalEntity.getX(),
      finalEntity.getY(), finalEntity.getZ(), offsetX, offsetY, offsetZ, speed, this.getNumberParticles());

  }
}
