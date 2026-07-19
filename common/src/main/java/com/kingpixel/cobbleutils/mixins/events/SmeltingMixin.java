package com.kingpixel.cobbleutils.mixins.events;

import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.events.models.EventItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.FurnaceOutputSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to handle smelting/cooking output events.
 */
@Mixin(FurnaceOutputSlot.class)
public abstract class SmeltingMixin {
  @Inject(method = "onTakeItem", at = @At("HEAD"))
  private void cobbleutilsOnTakeItem(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
    if (CobbleUtilsEvents.SMELTING_EVENT.isEmpty()) return;
    if (player == null || stack.isEmpty()) return;
    if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

    CobbleUtilsEvents.SMELTING_EVENT.emit(EventItemStack.builder()
      .itemStack(stack.copy())
      .player(serverPlayer)
      .build());
  }
}
