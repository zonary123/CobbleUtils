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
import net.minecraft.util.math.MathHelper;
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
    if (CobbleUtils.config.isDebug()) {
      CobbleUtils.LOGGER.info("Teleport confirm packet received");
    }
  }

  @Inject(method = "onPlayerMove", at = @At("HEAD"))
  public void breeding$onPlayerMove(PlayerMoveC2SPacket packet, CallbackInfo ci) {
    if (!CobbleUtils.breedconfig.isActive() || CobbleUtils.config.isApiMode()) return;
    tick++;

    if (tick % CobbleUtils.breedconfig.getTicksToWalking() == 0) {
      boolean isInPose = !player.isInPose(EntityPose.FALL_FLYING);
      boolean isInvulnerable = !player.isInvulnerable();
      boolean permittedVehicles = cobbleUtils$permittedVehicles(player);
      if ((isInPose && isInvulnerable && permittedVehicles && (!player.isTouchingWater() || player.isInPose(EntityPose.SWIMMING)))) { //
        var party = Cobblemon.INSTANCE.getStorage().getParty(player);

        entity = player;
        if (player.getVehicle() != null) entity = player.getVehicle();
        if (entity == null) {
          CobbleUtils.LOGGER.error("Entity is null");
          return;
        }

        double deltaMovement = cobbleUtils$getDeltaMovement(packet, party, entity);
        oldX = entity.getX();
        oldZ = entity.getZ();
        if (deltaMovement <= 0 || tp) {
          if (CobbleUtils.config.isDebug()) {
            CobbleUtils.LOGGER.info("Delta movement -> " + deltaMovement + " | Teleport -> " + tp);
          }
          tp = false;
          return;
        }


        for (Pokemon pokemon : party) {
          if (pokemon == null) continue;
          if (pokemon.showdownId().equals("egg")) {
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
    double valueX = entity instanceof ServerPlayerEntity ? packet.getX(entity.getX()) : entity.getX();
    double valueZ = entity instanceof ServerPlayerEntity ? packet.getZ(entity.getZ()) : entity.getZ();
    double newX = MathHelper.clamp(valueX, -3.0E7D, 3.0E7D);
    double newZ = MathHelper.clamp(valueZ, -3.0E7D, 3.0E7D);


    if (Double.isNaN(newX) || Double.isNaN(newZ)) return 0;

    double deltaX = newX - oldX;
    double deltaZ = newZ - oldZ;

    if (Double.isNaN(deltaX) || Double.isNaN(deltaZ)) return 0;


    var deltaMovement = Math.min(20, Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaZ, 2)));
    if (!(entity instanceof ServerPlayerEntity)) {
      double reduce = CobbleUtils.breedconfig.getReduceEggStepsVehicle();
      deltaMovement = deltaMovement / reduce;
    }
    return cobbleUtils$hasStepAcceleratingPokemon(party) ? deltaMovement : deltaMovement / 2;
  }

  @Unique
  private boolean cobbleUtils$hasStepAcceleratingPokemon(PlayerPartyStore party) {
    for (Pokemon pokemon : party) {
      if (CobbleUtils.breedconfig.getAbilityAcceleration().contains(pokemon.getAbility().getName()))
        return true;
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