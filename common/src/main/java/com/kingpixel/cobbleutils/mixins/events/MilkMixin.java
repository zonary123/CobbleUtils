package com.kingpixel.cobbleutils.mixins.events;

import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 *
 * @author Carlos Varas Alonso - 27/12/2025 1:33
 */
@Mixin(CowEntity.class)
public abstract class MilkMixin {
  @Inject(
    method = "interactMob",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/item/ItemUsage;exchangeStack(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;"
    )
  )
  private void cobbleutils$onMilk(
    PlayerEntity playerEntity, Hand hand, CallbackInfoReturnable<ActionResult> cir
  ) {
    if (CobbleUtilsEvents.MILKING_EVENT.isEmpty()) return;
    if (!(playerEntity instanceof ServerPlayerEntity player)) return;

    CobbleUtilsEvents.MILKING_EVENT.emit(player);
  }
}