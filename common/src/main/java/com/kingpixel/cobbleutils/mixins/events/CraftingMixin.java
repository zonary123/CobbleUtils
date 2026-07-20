package com.kingpixel.cobbleutils.mixins.events;

import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.events.models.EventItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to handle crafting events correctly, including shift-clicking.
 */
@Mixin(CraftingResultSlot.class)
public abstract class CraftingMixin {
  @Shadow @Final private PlayerEntity player;
  @Shadow private int amount;

  @Inject(method = "onCrafted(Lnet/minecraft/item/ItemStack;)V", at = @At("HEAD"))
  private void cobbleutilsOnCrafted(ItemStack stack, CallbackInfo ci) {
    if (CobbleUtilsEvents.CRAFTING_EVENT.isEmpty()) return;
    if (this.amount <= 0 || stack.isEmpty()) return;
    if (!(this.player instanceof ServerPlayerEntity serverPlayer)) return;

    ItemStack resultStack = stack.copy();
    resultStack.setCount(this.amount);

    CobbleUtilsEvents.CRAFTING_EVENT.emit(EventItemStack.builder()
      .itemStack(resultStack)
      .player(serverPlayer)
      .build());
  }
}

