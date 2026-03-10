package com.kingpixel.cobbleutils.mixins.events;

import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.events.models.EventItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.SmithingScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 *
 * @author Carlos Varas Alonso - 29/12/2025 5:25
 */
@Mixin(SmithingScreenHandler.class)
public abstract class SmithingTableMixin {
  @Inject(method = "onTakeOutput", at = @At("HEAD"))
  private void onSmithingCraft(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
    if (CobbleUtilsEvents.SMITHING_TABLE_EVENT.isEmpty()) return;
    CobbleUtilsEvents.SMITHING_TABLE_EVENT.emit(EventItemStack.builder()
      .player((ServerPlayerEntity) player)
      .itemStack(stack)
      .build());
  }
}
