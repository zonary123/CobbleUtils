package com.kingpixel.cobbleutils.mixins;

import com.kingpixel.cobbleutils.api.BlocksAPI;
import com.kingpixel.cobbleutils.database.blocks.ChunkBlockStorageManager;
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
 * @author Carlos Varas Alonso
 */
@Mixin(Block.class)
public abstract class BlockMixin {

  /* -------------------- PLACE -------------------- */

  @Inject(method = "onPlaced", at = @At("HEAD"))
  private void cobbleutils$onPlaced(
    World world,
    BlockPos pos,
    BlockState state,
    LivingEntity placer,
    ItemStack stack,
    CallbackInfo ci
  ) {
    if (!(placer instanceof ServerPlayerEntity player)) return;
    if (CobbleUtilsEvents.BLOCK_PLACED_EVENT.isEmpty() && CobbleUtilsEvents.BLOCK_BREAK_EVENT.isEmpty()) return;

    try {
      var chunk = world.getChunk(pos);

      boolean alreadyPlaced = ChunkBlockStorageManager
        .isPlacedByPlayer(world, chunk, pos);

      ChunkBlockStorageManager.markPlaced(world, chunk, pos, state);

      CobbleUtilsEvents.BLOCK_PLACED_EVENT.emit(
        new EventBlockPlaced(world, pos, state, player, alreadyPlaced)
      );

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /* -------------------- BREAK -------------------- */

  @Inject(method = "onBreak", at = @At("HEAD"))
  private void cobbleutils$onBreak(
    World world,
    BlockPos pos,
    BlockState state,
    PlayerEntity entity,
    CallbackInfoReturnable<BlockState> cir
  ) {
    if (!(entity instanceof ServerPlayerEntity player)) return;

    boolean hasBreak = !CobbleUtilsEvents.BLOCK_BREAK_EVENT.isEmpty();
    boolean hasCollect = !CobbleUtilsEvents.COLLECT_EVENT.isEmpty();

    if (!hasBreak && !hasCollect) return;

    try {
      boolean isPlaced = BlocksAPI.isBlockPlaceByPlayer(world, pos);

      if (hasBreak) {
        CobbleUtilsEvents.BLOCK_BREAK_EVENT.emit(
          new EventBlockBreak(world, pos, state, player, isPlaced)
        );
      }

      if (hasCollect) {
        var collectEvent = EventCollect.builder()
          .player(player)
          .playerPlaced(isPlaced)
          .world(world)
          .state(state)
          .pos(pos)
          .build();

        if (collectEvent.getAmount() > 0) {
          CobbleUtilsEvents.COLLECT_EVENT.emit(collectEvent);
        }
      }

      ChunkBlockStorageManager.removePlaced(
        world,
        world.getChunk(pos),
        pos,
        state
      );

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
