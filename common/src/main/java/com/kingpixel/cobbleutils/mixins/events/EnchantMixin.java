package com.kingpixel.cobbleutils.mixins.events;

import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.events.models.EventEnchant;
import net.minecraft.advancement.criterion.EnchantedItemCriterion;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Carlos Varas Alonso - 06/08/2025 21:39
 */
@Mixin(EnchantedItemCriterion.class)
public abstract class EnchantMixin {

  @Inject(method = "trigger", at = @At("HEAD"))
  private void CobbleQuests$trigger(ServerPlayerEntity player, ItemStack itemStack, int levels, CallbackInfo ci) {
    if (CobbleUtilsEvents.ENCHANT_EVENT.isEmpty()) return;
    if (player == null || itemStack.isEmpty()) return;


    CobbleUtilsEvents.ENCHANT_EVENT.emit(EventEnchant.builder()
      .player(player)
      .itemStack(itemStack.copy())
      .levels(levels)
      .build());
  }
}