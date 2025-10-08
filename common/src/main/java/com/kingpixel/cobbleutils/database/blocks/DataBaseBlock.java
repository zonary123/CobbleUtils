package com.kingpixel.cobbleutils.database.blocks;

import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Carlos Varas Alonso - 23/08/2025 7:39
 */
public abstract class DataBaseBlock {
  public static final ExecutorService DB_THREAD_FACTORY = new ThreadPoolExecutor(
    4,
    16,
    0L,
    TimeUnit.MILLISECONDS,
    new java.util.concurrent.LinkedBlockingQueue<>(), // sin límite de tamaño
    r -> {
      Thread t = new Thread(r);
      t.setDaemon(true);
      t.setName("CobbleUtils Database Blocks - " + t.getId());
      return t;
    },
    new ThreadPoolExecutor.CallerRunsPolicy()
  );

  public abstract void connect();

  public abstract void disconnect();

  /**
   * Delete all the data of a world
   *
   * @param world the world to delete
   */
  public abstract void deleteWorld(World world);

  /**
   * Delete all the data of a player in a world
   *
   * @param world  the world to delete
   * @param player the player to delete
   */
  public abstract void placeBlock(World world, BlockPos pos, BlockState state, ServerPlayerEntity player);

  /**
   * Delete the data of a block in a world
   *
   * @param world  the world to delete
   * @param pos    the position of the block to delete
   * @param state  the state of the block to delete
   * @param player the player who broke the block
   */
  public abstract void removeBlock(World world, BlockPos pos, BlockState state, ServerPlayerEntity player);

  /**
   * Check if a block was placed by a player
   *
   * @param world the world to check
   * @param pos   the position of the block to check
   *
   * @return true if the block was placed by a player, false otherwise
   */
  public abstract boolean isBlockPlaceByPlayer(World world, BlockPos pos);

  /**
   * Insert the blocks in the database
   */
  public abstract void insertBlocks();
}
