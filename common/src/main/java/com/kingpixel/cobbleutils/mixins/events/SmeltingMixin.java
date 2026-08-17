package com.kingpixel.cobbleutils.mixins.events;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.events.models.EventItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.FurnaceOutputSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to handle smelting/cooking output events.
 */
@Mixin(FurnaceOutputSlot.class)
public abstract class SmeltingMixin {
  @Shadow @Final private PlayerEntity player;
  @Shadow private int amount;

  @Inject(method = "onCrafted(Lnet/minecraft/item/ItemStack;)V", at = @At("HEAD"))
  private void cobbleutilsOnCrafted(ItemStack stack, CallbackInfo ci) {
    try {
      if (CobbleUtilsEvents.SMELTING_EVENT.isEmpty()) return;
      if (this.amount <= 0 || stack.isEmpty()) return;
      if (!(this.player instanceof ServerPlayerEntity serverPlayer)) return;

      ItemStack resultStack = stack.copy();
      resultStack.setCount(this.amount);

      CobbleUtilsEvents.SMELTING_EVENT.emit(EventItemStack.builder()
        .itemStack(resultStack)
        .player(serverPlayer)
        .build());
    } catch (Throwable e) {
      CobbleUtils.LOGGER_RAW.error("Error in SmeltingMixin#cobbleutilsOnCrafted", e);
    }
  }
}

