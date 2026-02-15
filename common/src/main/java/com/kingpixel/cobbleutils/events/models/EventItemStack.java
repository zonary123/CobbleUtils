package com.kingpixel.cobbleutils.events.models;

import lombok.Builder;
import lombok.Data;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

@Data
@Builder
public class EventItemStack {
  private ServerPlayerEntity player;
  private ItemStack itemStack;
  private List<ItemStack> itemStacks;

  public List<ItemStack> getItemStacks() {
    if (itemStacks == null) return List.of(itemStack);
    return itemStacks;
  }
}
