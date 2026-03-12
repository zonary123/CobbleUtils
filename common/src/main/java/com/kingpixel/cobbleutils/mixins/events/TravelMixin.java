package com.kingpixel.cobbleutils.mixins.events;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.events.models.EventTravel;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Mixin(ServerPlayerEntity.class)
public abstract class TravelMixin {

  @Unique
  private static final Cache<UUID, Vec3d> cobbleutils$lastPos = Caffeine.newBuilder()
    .expireAfterAccess(10, TimeUnit.SECONDS)
    .maximumSize(1000)
    .build();

  @Unique
  private static final Cache<UUID, Integer> cobbleutils$tickCounters = Caffeine.newBuilder()
    .expireAfterAccess(10, TimeUnit.SECONDS)
    .maximumSize(1000)
    .build();

  @Unique
  private static final double MOVEMENT_THRESHOLD = 0.01D;

  @Unique
  private short cobbleutils$teleportTicks = 0;

  @Inject(method = "requestTeleport", at = @At("HEAD"))
  private void cobbleutils$requestTeleport(double destX, double destY, double destZ, CallbackInfo ci) {
    cobbleutils$markTeleport();
  }

  @Inject(method = "teleport(Lnet/minecraft/server/world/ServerWorld;DDDFF)V", at = @At("HEAD"))
  private void cobbleutils$teleport(CallbackInfo ci) {
    cobbleutils$markTeleport();
  }

  @Inject(method = "teleport(Lnet/minecraft/server/world/ServerWorld;DDDLjava/util/Set;FF)Z", at = @At("HEAD"))
  private void cobbleutils$teleportWorld(ServerWorld world, double destX, double destY, double destZ,
                                         Set<PositionFlag> flags, float yaw, float pitch,
                                         CallbackInfoReturnable<Boolean> cir) {
    cobbleutils$markTeleport();
  }

  @Inject(method = "teleportTo", at = @At("HEAD"))
  private void cobbleutils$teleportTo(TeleportTarget teleportTarget,
                                      CallbackInfoReturnable<Entity> cir) {
    cobbleutils$markTeleport();
  }

  @Unique
  private void cobbleutils$markTeleport() {
    cobbleutils$teleportTicks = 3;
  }

  @Inject(method = "tick", at = @At("HEAD"))
  private void cobbleutils$onTick(CallbackInfo ci) {
    if (CobbleUtilsEvents.TRAVEL_EVENT.isEmpty()) return;
    try {
      ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
      UUID uuid = player.getUuid();

      if (cobbleutils$teleportTicks > 0) {
        cobbleutils$teleportTicks--;
        cobbleutils$lastPos.put(uuid, player.getPos());
        return;
      }

      int tickCount = cobbleutils$tickCounters.get(uuid, key -> 0) + 1;

      if (tickCount < 20) {
        cobbleutils$tickCounters.put(uuid, tickCount);
        return;
      }

      cobbleutils$tickCounters.put(uuid, 0);

      Vec3d currentPos = player.getPos();
      Vec3d lastPos = cobbleutils$lastPos.get(uuid, key -> currentPos);

      if (currentPos.squaredDistanceTo(lastPos) <= MOVEMENT_THRESHOLD * MOVEMENT_THRESHOLD) {
        cobbleutils$lastPos.put(uuid, currentPos);
        return;
      }

      boolean movingByInput =
        player.forwardSpeed != 0 ||
          player.sidewaysSpeed != 0;

      if (!movingByInput) {
        cobbleutils$lastPos.put(uuid, currentPos);
        return;
      }

      cobbleutils$lastPos.put(uuid, currentPos);

      CobbleUtilsEvents.TRAVEL_EVENT.emit(EventTravel.builder()
        .distance(lastPos.distanceTo(currentPos))
        .player(player)
        .build());

    } catch (Exception e) {
      CobbleUtils.LOGGER.error("Error in TravelMixin tick");
      e.printStackTrace();
    }
  }
}