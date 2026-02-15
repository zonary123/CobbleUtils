package com.kingpixel.cobbleutils.mixins.events;

import com.cobblemon.mod.common.block.campfirepot.CookingPotResultSlot;
import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.events.models.EventItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(CookingPotResultSlot.class)
public abstract class CampfirePotMixin {
  @Inject(method = "onTakeItem", at = @At("HEAD"))
  private void cobbleutils$onTakeItem(PlayerEntity playerEntity, ItemStack itemStack, CallbackInfo ci) {
    if (CobbleUtilsEvents.CAMPFIRE_POT_EVENT.isEmpty()) return;
    if (playerEntity == null || itemStack.isEmpty()) return;

    if (!(playerEntity instanceof ServerPlayerEntity player)) return;

    CobbleUtilsEvents.CAMPFIRE_POT_EVENT.emit(EventItemStack.builder()
      .itemStack(itemStack)
      .player(player)
      .build());
  }
}
