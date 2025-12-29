package com.kingpixel.cobbleutils.events.models;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.api.BlocksApi;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 *
 *
 * @author Carlos Varas Alonso - 26/08/2025 14:43
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventCollect {
  private World world;
  private BlockPos pos;
  private BlockState state;
  private ServerPlayerEntity player;
  private boolean playerPlaced;
  private int amount;
  private ItemStack itemStack;
  private boolean cachedAmount;

  /**
   * Obtain the amount of items to collect from the block state and position.
   *
   * @return the amount of items to collect
   */
  public int getAmount() {
    if (cachedAmount) {
      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER.info("EventCollect: Using preset amount: " + amount);
      }
      return amount;
    }

    if (state != null) {
      amount = BlocksApi.getAmount(state.getBlock(), pos, state, world);
      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER.info("EventCollect: BlocksApi amount for "
          + state.getBlock().toString() + " at " + pos + ": " + amount);
      }
      if (amount != -1) {
        cachedAmount = true;
        return amount;
      }
    }

    amount = itemStack != null ? itemStack.getCount() : 1;
    if (CobbleUtils.config.isDebug()) {
      CobbleUtils.LOGGER.info("EventCollect: Final fallback amount: " + amount);
    }
    cachedAmount = true;
    return amount;
  }


  /**
   * Obtain the ItemStack to collect from the block state.
   *
   * @return the ItemStack to collect
   */
  public ItemStack getItemStack() {
    if (itemStack != null) return itemStack;
    return BlocksApi.getItemStack(state.getBlock());
  }
}
