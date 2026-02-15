package com.kingpixel.cobbleutils.mixins.events;

import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.events.models.EventEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 *
 * @author Carlos Varas Alonso - 27/12/2025 1:33
 */
@Mixin(TameableEntity.class)
public abstract class TamedMixin {

  @Inject(
    method = "setOwner",
    at = @At("HEAD")
  )
  private void cobbleutils$onTame(PlayerEntity playerEntity, CallbackInfo ci) {
    if (CobbleUtilsEvents.TAMING_EVENT.isEmpty()) return;
    if (playerEntity == null || playerEntity.getWorld().isClient) return;

    TameableEntity entity = (TameableEntity) (Object) this;

    if (!(playerEntity instanceof ServerPlayerEntity player)) return;

    CobbleUtilsEvents.TAMING_EVENT.emit(EventEntity.builder()
      .entity(entity)
      .player(player)
      .build());
  }
}

