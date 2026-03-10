package com.kingpixel.cobbleutils.events.models;

import lombok.Builder;
import lombok.Data;
import net.minecraft.block.Block;
import net.minecraft.server.network.ServerPlayerEntity;

@Data
@Builder
public class EventBlock {
  private ServerPlayerEntity player;
  private Block block;

}
