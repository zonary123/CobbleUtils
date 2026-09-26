package com.kingpixel.cobbleutils.mixins.events;

import com.cobblemon.mod.common.block.campfirepot.CookingPotMenu;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.events.CampfirePotTracker;
import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.events.models.EventItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CookingPotMenu.class)
public abstract class CookingPotMenuMixin {

  @Inject(method = "quickMove", at = @At("HEAD"))
  private void cobbleutils$onQuickMoveHead(PlayerEntity player, int slot, CallbackInfoReturnable<ItemStack> cir) {
    CampfirePotTracker.IN_QUICK_MOVE.set(true);
  }

  @Inject(method = "quickMove", at = @At("RETURN"))
  private void cobbleutils$onQuickMoveReturn(PlayerEntity player, int slot, CallbackInfoReturnable<ItemStack> cir) {
    try {
      if (slot == 0) { // CampfireBlockEntity.RESULT_SLOT
        if (CobbleUtilsEvents.CAMPFIRE_POT_EVENT.isEmpty()) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        ItemStack result = cir.getReturnValue();
        if (result != null && !result.isEmpty()) {
          CobbleUtilsEvents.CAMPFIRE_POT_EVENT.emit(EventItemStack.builder()
            .itemStack(result.copy())
            .player(serverPlayer)
            .build());
        }
      }
    } catch (Throwable e) {
      CobbleUtils.LOGGER_RAW.error("Error in CookingPotMenuMixin#cobbleutils$onQuickMoveReturn", e);
    } finally {
      CampfirePotTracker.IN_QUICK_MOVE.set(false);
    }
  }
}
