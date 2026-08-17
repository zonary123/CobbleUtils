package com.kingpixel.cobbleutils.mixins.collect;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.events.models.EventCollect;
import net.minecraft.block.BlockState;
import net.minecraft.block.CaveVinesBodyBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CaveVinesBodyBlock.class)
public abstract class CaveVinesMixin {

  @Inject(
    method = "onUse",
    at = @At(value = "RETURN")
  )
  private static void cobbleUtils$onUse(BlockState state, World world, BlockPos pos, PlayerEntity playerEntity,
                                        BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
    try {
      if (CobbleUtilsEvents.COLLECT_EVENT.isEmpty()) return;
      if (!(playerEntity instanceof ServerPlayerEntity player)) return;

      var result = cir.getReturnValue();
      if (result.isAccepted()) {
        CobbleUtilsEvents.COLLECT_EVENT.emit(EventCollect.builder()
          .world(world)
          .player(player)
          .playerPlaced(false)
          .itemStack(new ItemStack(Items.GLOW_BERRIES, 1))
          .build());
      }
    } catch (Throwable e) {
      CobbleUtils.LOGGER_RAW.error("Error in CaveVinesMixin#cobbleUtils$onUse", e);
    }
  }
}


