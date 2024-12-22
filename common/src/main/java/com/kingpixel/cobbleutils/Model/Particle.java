package com.kingpixel.cobbleutils.Model;

import lombok.Getter;
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
    this.particle = "minecraft:flame";
    this.numberParticles = 1;
    this.offsetX = 1;
    this.offsetY = 1;
    this.offsetZ = 1;
    this.speed = 0;
    this.radius = 32.0;
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
    entities.forEach(entity -> sendParticles(player, entity));
  }

  public void sendParticles(@NotNull ServerPlayerEntity player, @NotNull Entity entity) {
    player.networkHandler.sendPacket(getParticleS2CPacket(entity, getParticleType()));
  }

  private @NotNull ParticleEffect getParticleType() {
    if (getParticle() == null || getParticle().isEmpty()) return null;

    String[] split = getParticle().split(":");
    Identifier identifier = Identifier.of(split[0], split[1]);

    ParticleEffect particleType;
    try {
      if (Registries.PARTICLE_TYPE.get(identifier) instanceof ParticleEffect) {
        particleType = (ParticleEffect) Registries.PARTICLE_TYPE.get(identifier);
      } else {
        particleType = ParticleTypes.LAVA;
      }
    } catch (Exception e) {
      particleType = ParticleTypes.LAVA;
    }
    return particleType;
  }

  public void sendParticlesNearPlayers(@NotNull Entity entity) {
    List<ServerPlayerEntity> players = entity.getWorld().getEntitiesByClass(ServerPlayerEntity.class,
      Box.from(entity.getPos()).expand(radius == null ? 32 : radius), entity1 -> true);
    if (players != null && !players.isEmpty()) {
      players.forEach(player -> sendParticles(player, entity));
    }
  }

  // Privates
  private @NotNull ParticleS2CPacket getParticleS2CPacket(@NotNull Entity entity,
                                                          @NotNull ParticleEffect particleType) {
    int offsetX = this.getOffsetX() == null ? 0 : this.getOffsetX();
    int offsetY = this.getOffsetY() == null ? 0 : this.getOffsetY();
    int offsetZ = this.getOffsetZ() == null ? 0 : this.getOffsetZ();
    int speed = this.getSpeed() == null ? 0 : this.getSpeed();

    return new ParticleS2CPacket(particleType, true,
      entity.getX(),
      entity.getY(), entity.getZ(), offsetX, offsetY, offsetZ, speed, this.getNumberParticles());
  }
}
