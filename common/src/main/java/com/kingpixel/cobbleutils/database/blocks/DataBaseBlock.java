package com.kingpixel.cobbleutils.database.blocks;

import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * @author Carlos Varas Alonso - 23/08/2025 7:39
 */
public abstract class DataBaseBlock {
  public abstract void connect();

  public abstract void disconnect();

  public abstract void placeBlock(World world, BlockPos pos, BlockState state, ServerPlayerEntity player);

  public abstract void removeBlock(World world, BlockPos pos, BlockState state, ServerPlayerEntity player);

  public abstract boolean isBlockPlaceByPlayer(World world, BlockPos pos);

  public abstract void insertBlocks();
}
