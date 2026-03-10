package com.kingpixel.cobbleutils.mixins.events;

import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.events.models.EventItemStack;
import net.minecraft.advancement.criterion.VillagerTradeCriterion;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Carlos Varas Alonso - 06/08/2025 21:39
 */
@Mixin(VillagerTradeCriterion.class)
public abstract class VillagerTradeMixin {

  @Inject(method = "trigger", at = @At("HEAD"))
  private void CobbleQuests$onKilledOther(ServerPlayerEntity player, MerchantEntity merchant, ItemStack itemStack, CallbackInfo ci) {
    if (CobbleUtilsEvents.TRADE_EVENT.isEmpty()) return;

    CobbleUtilsEvents.TRADE_EVENT.emit(EventItemStack.builder()
      .player(player)
      .itemStack(itemStack)
      .build());
  }
}
