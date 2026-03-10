package com.kingpixel.cobbleutils.events.models;

import lombok.Builder;
import lombok.Data;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

@Data
@Builder
public class EventEnchant {
  private ServerPlayerEntity player;
  private ItemStack itemStack;
  private int levels;

}
