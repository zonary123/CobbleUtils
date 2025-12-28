package com.kingpixel.cobbleutils.events.models;

import com.kingpixel.cobbleutils.api.BlocksApi;
import lombok.Builder;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * @param player puede ser null si no hay jugador
 *
 * @author Carlos Varas Alonso - 26/08/2025 14:43
 */
@Builder
public record EventCollect(World world, BlockPos pos, BlockState state, ServerPlayerEntity player,
                           boolean playerPlaced, int amount, ItemStack itemStack) {

  /**
   * Obtain the amount of items to collect from the block state and position.
   *
   * @return the amount of items to collect
   */
  public int getAmount() {
    if (itemStack != null) return itemStack.getCount();
    return BlocksApi.getAmount(state.getBlock(), pos, state, world);
  }

  /**
   * Obtain the ItemStack to collect from the block state.
   *
   * @return the ItemStack to collect
   */
  public ItemStack getItemStack() {
    return itemStack != null ? itemStack : BlocksApi.getItemStack(state.getBlock());
  }
}
