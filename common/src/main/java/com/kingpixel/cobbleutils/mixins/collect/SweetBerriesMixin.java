package com.kingpixel.cobbleutils.mixins.collect;

import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.events.models.EventCollect;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SweetBerryBushBlock.class)
public abstract class SweetBerriesMixin {

  @Unique @WrapOperation(
    method = "onUse",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/block/SweetBerryBushBlock;dropStack(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/item/ItemStack;)V"
    )
  )
  private void cobbleutils$onSweetBerryDrop(
    World world,
    BlockPos pos,
    ItemStack stack,
    Operation<Void> original,
    @Local(argsOnly = true) PlayerEntity player
  ) {
    CobbleUtilsEvents.COLLECT_EVENT.emit(
      EventCollect.builder()
        .world(world)
        .pos(pos)
        .itemStack(stack)
        .player((ServerPlayerEntity) player)
        .build()
    );
    original.call(world, pos, stack);
  }
}

