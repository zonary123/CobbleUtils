package com.kingpixel.cobbleutils.mixins;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.features.breeding.models.EggData;
import net.minecraft.entity.EntityPose;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
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

  @Inject(method = "onPlayerMove", at = @At("HEAD"))
  public void breeding$onPlayerMove(PlayerMoveC2SPacket packet, CallbackInfo ci) {
    if (!CobbleUtils.breedconfig.isActive() || CobbleUtils.config.isApiMode()) return;
    boolean isinpose = !player.isInPose(EntityPose.FALL_FLYING);
    boolean isinvulnerable = !player.isInvulnerable();
    boolean permittedVehicles = cobbleUtils$permittedVehicles(player);
    if (isinpose && isinvulnerable && permittedVehicles) { // No elytra or flight

      var party = Cobblemon.INSTANCE.getStorage().getParty(player);

      double deltaMovement = cobbleUtils$getDeltaMovement(player, packet,
        party);

      if (deltaMovement == 0 || deltaMovement >= 1) return;


      for (Pokemon pokemon : party) {
        if (pokemon == null) continue;
        if (pokemon.getSpecies().showdownId().equals("egg")) {
          cobbleUtils$updateEggSteps(pokemon, deltaMovement);
        }
      }
    }
  }

  @Unique private boolean cobbleUtils$permittedVehicles(ServerPlayerEntity player) {
    String id = player.getVehicle() == null ? "" : player.getVehicle().getSavedEntityId();
    if (id == null) id = "";
    return CobbleUtils.breedconfig.getPermittedVehicles().contains(id) || id.isEmpty();
  }

  @Unique
  private double cobbleUtils$getDeltaMovement(ServerPlayerEntity player, PlayerMoveC2SPacket packet, PlayerPartyStore party) {
    double oldX = player.getX();
    double oldZ = player.getZ();
    double newX = MathHelper.clamp(packet.getX(oldX), -3.0E7D, 3.0E7D);
    double newZ = MathHelper.clamp(packet.getZ(oldZ), -3.0E7D, 3.0E7D);

    if (Double.isNaN(newX) || Double.isNaN(newZ)) return 0;

    double deltaX = newX - oldX;
    double deltaZ = newZ - oldZ;

    if (Double.isNaN(deltaX) || Double.isNaN(deltaZ)) return 0;


    var deltaMovement = Math.min(20, Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaZ, 2)));
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