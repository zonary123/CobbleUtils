package com.kingpixel.cobbleutils.mixins.events;

import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.events.models.EventItemStack;
import net.minecraft.advancement.criterion.FishingRodHookedCriterion;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

/**
 * @author Carlos Varas Alonso - 06/08/2025 21:39
 */
@Mixin(FishingRodHookedCriterion.class)
public abstract class FishingMixin {
  @Inject(method = "trigger", at = @At("HEAD"))
  private void trigger(ServerPlayerEntity player, ItemStack rod, FishingBobberEntity bobber,
                       Collection<ItemStack> fishingLoots, CallbackInfo ci) {
    if (CobbleUtilsEvents.FISHING_EVENT.isEmpty()) return;

    CobbleUtilsEvents.FISHING_EVENT.emit(EventItemStack.builder()
      .player(player)
      .itemStacks(fishingLoots.stream().toList())
      .build());
  }
}
