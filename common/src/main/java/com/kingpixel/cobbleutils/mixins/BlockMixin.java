package com.kingpixel.cobbleutils.mixins;

import com.kingpixel.cobbleutils.database.DataBaseFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Carlos Varas Alonso - 23/08/2025 8:29
 */
@Mixin(Block.class)
public abstract class BlockMixin {
  @Inject(method = "onPlaced", at = @At("HEAD"))
  private void CobbleUtils$onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer,
                                    ItemStack itemStack,
                                    CallbackInfo ci) {
    if (placer == null) return;
    ServerPlayerEntity player = (placer instanceof ServerPlayerEntity serverPlayer) ? serverPlayer : null;
    if (player == null) return;
    DataBaseFactory.INSTANCE.placeBlock(
      world,
      pos,
      state,
      player
    );
  }

  @Inject(method = "onBreak", at = @At("HEAD"))
  private void CobbleUtils$onBreak(World world, BlockPos pos, BlockState state, PlayerEntity playerEntity,
                                   CallbackInfoReturnable<BlockState> cir) {
    if (playerEntity == null) return;
    ServerPlayerEntity player = (playerEntity instanceof ServerPlayerEntity serverPlayer) ? serverPlayer : null;
    if (player == null) return;
    DataBaseFactory.INSTANCE.removeBlock(
      world,
      pos,
      state,
      player
    );
  }
}
