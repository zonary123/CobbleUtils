package com.kingpixel.cobbleutils.mixins.events;

import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.events.models.EventEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Carlos Varas Alonso - 06/08/2025 21:39
 */
@Mixin(PlayerEntity.class)
public abstract class KillEntityMixin {

  @Inject(method = "onKilledOther", at = @At("HEAD"))
  private void cobbleutils$onKilledOther(ServerWorld world, LivingEntity other, CallbackInfoReturnable<Boolean> cir) {
    if (CobbleUtilsEvents.ENTITY_KILLED_EVENT.isEmpty()) return;
    PlayerEntity playerEntity = (PlayerEntity) (Object) this;
    if (!(playerEntity instanceof ServerPlayerEntity player)) return;

    CobbleUtilsEvents.ENTITY_KILLED_EVENT.emit(EventEntity.builder()
      .entity(other)
      .player(player)
      .build());
  }
}
