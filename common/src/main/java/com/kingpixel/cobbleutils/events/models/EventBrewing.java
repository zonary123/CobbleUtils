package com.kingpixel.cobbleutils.events.models;

import lombok.Data;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * @author Carlos Varas Alonso - 26/08/2025 14:43
 */
@Data
public class EventBrewing {
  private ServerPlayerEntity player;
  private World world;
  private BlockPos pos;
  private DefaultedList<ItemStack> items;

  public EventBrewing(ServerPlayerEntity player, World world, BlockPos pos, DefaultedList<ItemStack> slots) {
    this.player = player;
    this.world = world;
    this.pos = pos;
    this.items = slots;
  }
}
