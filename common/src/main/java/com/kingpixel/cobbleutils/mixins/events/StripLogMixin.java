package com.kingpixel.cobbleutils.mixins.events;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.events.models.EventBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 *
 * @author Carlos Varas Alonso - 27/12/2025 1:33
 */
@Mixin(AxeItem.class)
public abstract class StripLogMixin {
  @Inject(
    method = "useOnBlock",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z",
      shift = At.Shift.AFTER
    )
  )
  private void cobbleutils$onStripLog(
    ItemUsageContext context,
    CallbackInfoReturnable<ActionResult> cir
  ) {
    try {
      if (CobbleUtilsEvents.STRIPPED_LOG_EVENT.isEmpty()) return;
      if (context.getWorld().isClient()) return;

      PlayerEntity player = context.getPlayer();
      if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

      BlockState oldState = context.getWorld().getBlockState(context.getBlockPos());

      if (!oldState.isIn(BlockTags.LOGS)) return;

      CobbleUtilsEvents.STRIPPED_LOG_EVENT.emit(EventBlock.builder()
        .player(serverPlayer)
        .block(oldState.getBlock())
        .build());
    } catch (Throwable e) {
      CobbleUtils.LOGGER_RAW.error("Error in StripLogMixin#cobbleutils$onStripLog", e);
    }
  }
}