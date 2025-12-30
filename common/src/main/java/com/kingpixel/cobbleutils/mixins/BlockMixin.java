package com.kingpixel.cobbleutils.mixins;

import com.kingpixel.cobbleutils.api.BlocksApi;
import com.kingpixel.cobbleutils.database.blocks.manager.ChunkBlockStorageManager;
import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.events.models.EventBlockBreak;
import com.kingpixel.cobbleutils.events.models.EventBlockPlaced;
import com.kingpixel.cobbleutils.events.models.EventCollect;
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
    try {
      if (placer == null) return;
      if (!(placer instanceof ServerPlayerEntity player)) return;
      boolean placed = ChunkBlockStorageManager.isPlacedByPlayer(world, world.getChunk(pos), pos);
      ChunkBlockStorageManager.markPlaced(world, world.getChunk(pos), pos, state);
      CobbleUtilsEvents.BLOCK_PLACED_EVENT.emit(new EventBlockPlaced(
        world,
        pos,
        state,
        player,
        placed
      ));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Inject(method = "onBreak", at = @At("HEAD"))
  private void CobbleUtils$onBreak(World world, BlockPos pos, BlockState state, PlayerEntity playerEntity,
                                   CallbackInfoReturnable<BlockState> cir) {
    try {
      if (!(playerEntity instanceof ServerPlayerEntity player)) return;
      boolean isPlaced = BlocksApi.isBlockPlaceByPlayer(world, pos);
      CobbleUtilsEvents.BLOCK_BREAK_EVENT.emit(new EventBlockBreak(
        world,
        pos,
        state,
        player,
        isPlaced
      ));

      var evt = EventCollect.builder()
        .player(player)
        .playerPlaced(isPlaced)
        .world(world)
        .state(state)
        .pos(pos)
        .build();
      if (evt.getAmount() > 0) {
        /*if (isPlaced) {
          int adjustedAmount = Math.max(0, evt.getAmount() - 1);
          evt.setAmount(adjustedAmount);
        }*/
        CobbleUtilsEvents.COLLECT_EVENT.emit(
          evt
        );
      }
      ChunkBlockStorageManager.removePlaced(world, world.getChunk(pos), pos, state);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
