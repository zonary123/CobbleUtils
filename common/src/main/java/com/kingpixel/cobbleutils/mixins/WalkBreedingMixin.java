package com.kingpixel.cobbleutils.mixins;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.features.breeding.models.EggData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class WalkBreedingMixin {

  @Shadow public ServerPlayerEntity player;
  @Unique private Entity entity;
  @Unique private double oldX;
  @Unique private double oldZ;
  @Unique private boolean tp;
  @Unique private long tick;

  @Inject(method = "onTeleportConfirm", at = @At("HEAD"))
  public void breeding$handlePendingTeleport(TeleportConfirmC2SPacket packet, CallbackInfo ci) {
    tp = true;
  }

  @Inject(method = "onPlayerMove", at = @At("HEAD"))
  public void breeding$onPlayerMove(PlayerMoveC2SPacket packet, CallbackInfo ci) {
    if (!CobbleUtils.breedconfig.isActive() || CobbleUtils.config.isApiMode()) return;
    tick++;

    if (tick % CobbleUtils.breedconfig.getTicksToWalking() == 0) {
      boolean isInPose = !player.isInPose(EntityPose.FALL_FLYING);
      boolean isInvulnerable = !player.isInvulnerable();
      boolean permittedVehicles = cobbleUtils$permittedVehicles(player);
      if (isInPose && isInvulnerable && permittedVehicles && (!player.isTouchingWater() || player.isInPose(EntityPose.SWIMMING))) {
        var party = Cobblemon.INSTANCE.getStorage().getParty(player);

        entity = player.getVehicle() != null ? player.getVehicle() : player;
        if (entity == null) return;

        double deltaMovement = cobbleUtils$getDeltaMovement(packet, party, entity);

        oldX = entity.getX();
        oldZ = entity.getZ();
        if (deltaMovement <= 0 || tp) {
          tp = false;
          return;
        }

        for (Pokemon pokemon : party) {
          if (pokemon != null && pokemon.showdownId().equals("egg")) {
            cobbleUtils$updateEggSteps(pokemon, deltaMovement);
          }
        }
      }

      tick = 0;
    }
  }

  @Unique private boolean cobbleUtils$permittedVehicles(ServerPlayerEntity player) {
    String id = player.getVehicle() == null ? "" : player.getVehicle().getSavedEntityId();
    if (id == null) id = "";
    return CobbleUtils.breedconfig.getPermittedVehicles().contains(id) || id.isEmpty();
  }

  @Unique
  private double cobbleUtils$getDeltaMovement(PlayerMoveC2SPacket packet, PlayerPartyStore party, Entity entity) {
    double newX = entity instanceof ServerPlayerEntity ? packet.getX(entity.getX()) : entity.getX();
    double newZ = entity instanceof ServerPlayerEntity ? packet.getZ(entity.getZ()) : entity.getZ();
    //double newX = MathHelper.clamp(valueX, -3.0E7D, 3.0E7D);
    //double newZ = MathHelper.clamp(valueZ, -3.0E7D, 3.0E7D);

    if (Double.isNaN(newX) || Double.isNaN(newZ)) return 0;

    double deltaX = newX - oldX;
    double deltaZ = newZ - oldZ;

    if (Double.isNaN(deltaX) || Double.isNaN(deltaZ)) return 0;


    double deltaMovement = Math.hypot(deltaX, deltaZ);

    if (!(entity instanceof ServerPlayerEntity)) {
      deltaMovement /= CobbleUtils.breedconfig.getReduceEggStepsVehicle();
    }
    return cobbleUtils$hasStepAcceleratingPokemon(party) ? deltaMovement * CobbleUtils.breedconfig.getMultiplierAbilityAcceleration() : deltaMovement / 2;
  }

  @Unique
  private boolean cobbleUtils$hasStepAcceleratingPokemon(PlayerPartyStore party) {
    for (Pokemon pokemon : party) {
      if (CobbleUtils.breedconfig.getAbilityAcceleration().contains(pokemon.getAbility().getName())) {
        return true;
      }
    }
    return false;
  }

  @Unique
  private void cobbleUtils$updateEggSteps(Pokemon egg, double deltaMovement) {
    egg.setCurrentHealth(0);
    EggData eggData = EggData.from(egg);
    eggData.steps(player, egg, deltaMovement);
  }
}