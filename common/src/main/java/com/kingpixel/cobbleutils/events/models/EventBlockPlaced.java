package com.kingpixel.cobbleutils.events.models;

/**
 * @author Carlos Varas Alonso - 03/10/2025 22:39
 */

import com.kingpixel.cobbleutils.database.blocks.BlockData;
import lombok.Data;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Data
public class EventBlockPlaced {
  private final World world;
  private final BlockPos pos;
  private final BlockState state;
  private final ServerPlayerEntity player; // puede ser null si no hay jugador
  private final BlockData data;
  private final boolean playerPlaced;

  public EventBlockPlaced(World world, BlockPos pos, BlockState state,
                          ServerPlayerEntity player,
                          BlockData data,
                          boolean playerPlaced) {
    this.world = world;
    this.pos = pos;
    this.state = state;
    this.player = player;
    this.data = data;
    this.playerPlaced = playerPlaced;
  }

}

