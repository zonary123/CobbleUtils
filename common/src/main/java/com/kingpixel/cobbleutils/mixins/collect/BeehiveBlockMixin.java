package com.kingpixel.cobbleutils.mixins.collect;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.events.models.EventCollect;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.item.Items.GLASS_BOTTLE;
import static net.minecraft.item.Items.SHEARS;

/**
 *
 * @author Carlos Varas Alonso - 27/12/2025 19:08
 */
@Mixin(BeehiveBlock.class)
public abstract class BeehiveBlockMixin {


  @Inject(
    method = "onUseWithItem",
    at = @At("RETURN")
  )
  private static void cobbleutils$onBeehiveCollect(
    ItemStack stack,
    BlockState state,
    World world,
    BlockPos pos,
    PlayerEntity player,
    Hand hand,
    BlockHitResult hit,
    CallbackInfoReturnable<ItemActionResult> cir
  ) {
    try {
      if (CobbleUtilsEvents.COLLECT_EVENT.isEmpty()) return;
      if (world.isClient()) return;
      if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

      ItemActionResult result = cir.getReturnValue();
      if (!result.isAccepted()) return;

      boolean isBottle = stack.isOf(GLASS_BOTTLE) || stack.isOf(Items.HONEY_BOTTLE) || stack.isEmpty();
      boolean isShears = stack.isOf(SHEARS);

      if (!isBottle && !isShears) return;

      ItemStack finalItemStack = null;
      if (isBottle) {
        finalItemStack = new ItemStack(Items.HONEY_BOTTLE, 1);
      } else if (isShears) {
        finalItemStack = new ItemStack(Items.HONEYCOMB, 3);
      } else {
        finalItemStack = ItemStack.EMPTY.copy();
      }
      CobbleUtilsEvents.COLLECT_EVENT.emit(
        EventCollect.builder()
          .world(world)
          .pos(pos)
          .player(serverPlayer)
          .itemStack(finalItemStack)
          .build()
      );
    } catch (Throwable e) {
      CobbleUtils.LOGGER_RAW.error("Error in BeehiveBlockMixin#cobbleutils$onBeehiveCollect", e);
    }
  }


}



