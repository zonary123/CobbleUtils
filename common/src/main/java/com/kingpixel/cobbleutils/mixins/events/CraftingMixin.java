package com.kingpixel.cobbleutils.mixins.events;

import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.events.models.EventItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to handle crafting events correctly, including shift-clicking.
 */
@Mixin(CraftingResultSlot.class)
public abstract class CraftingMixin {
  @Inject(method = "onTakeItem", at = @At("HEAD"))
  private void cobbleutilsOnTakeItem(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
    if (CobbleUtilsEvents.CRAFTING_EVENT.isEmpty()) return;
    if (player == null || stack.isEmpty()) return;
    if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

    CobbleUtilsEvents.CRAFTING_EVENT.emit(EventItemStack.builder()
      .itemStack(stack.copy())
      .player(serverPlayer)
      .build());
  }
}
