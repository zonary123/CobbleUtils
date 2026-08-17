package com.kingpixel.cobbleutils.mixins.events;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.events.models.EventItemStack;
import net.minecraft.block.entity.BrushableBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrushableBlockEntity.class)
public abstract class ArcheologyMixin {

  @Inject(
    method = "spawnItem",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/block/entity/BrushableBlockEntity;generateItem(Lnet/minecraft/entity/player/PlayerEntity;)V",
      shift = At.Shift.AFTER
    )
  )
  private void afterGenerateItem(PlayerEntity playerEntity, CallbackInfo ci) {
    try {
      if (CobbleUtilsEvents.ARCHEOLOGY_EVENT.isEmpty()) return;
      if (playerEntity == null || playerEntity.getWorld() == null) return;

      if (!(playerEntity instanceof ServerPlayerEntity player)) return;

      BrushableBlockEntity self = (BrushableBlockEntity) (Object) this;
      ItemStack stackCopy = self.getItem().copy();

      if (stackCopy.isEmpty()) return;

      CobbleUtilsEvents.ARCHEOLOGY_EVENT.emit(EventItemStack.builder()
        .player(player)
        .itemStack(stackCopy)
        .build());
    } catch (Throwable e) {
      CobbleUtils.LOGGER_RAW.error("Error in ArcheologyMixin#afterGenerateItem", e);
    }
  }
}
