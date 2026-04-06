package com.kingpixel.cobbleutils.api;

import com.cobblemon.mod.common.block.HeartyGrainsBlock;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.database.blocks.ChunkBlockStorageManager;
import net.minecraft.block.*;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * API para calcular la cantidad de bloques de cultivo o tipo columna
 * que deben recolectarse automáticamente.
 */
public class BlocksAPI {

  /**
   * Check if a block was placed by a player (not naturally generated).
   *
   * @param world the world
   * @param pos   the block position
   * @return true if the block was placed by a player
   */
  public static boolean isBlockPlaceByPlayer(World world, BlockPos pos) {
    return ChunkBlockStorageManager.isPlacedByPlayer(world, world.getChunk(pos), pos);
  }

  /**
   * Check if the crop is mature.
   *
   * @param block the block
   * @param state the block state
   * @return true if the crop is mature
   */
  public static boolean isMature(Block block, BlockState state) {
    try {
      int age = 0;
      int maxAge = 0;

      switch (block) {
        case CactusBlock cactusBlock -> {
          age = 0;
          maxAge = 0;
        }
        case CropBlock cropBlock -> {
          return cropBlock.isMature(state);
        }
        case KelpBlock kelpBlock -> {
          age = state.getOrEmpty(KelpBlock.AGE).orElse(0);
          maxAge = KelpBlock.MAX_AGE;
        }
        case SugarCaneBlock sugarCaneBlock -> {
          age = state.getOrEmpty(SugarCaneBlock.AGE).orElse(0);
          maxAge = 0;
        }
        case CocoaBlock cocoaBlock -> {
          age = state.getOrEmpty(CocoaBlock.AGE).orElse(0);
          maxAge = CocoaBlock.MAX_AGE;
        }
        case BambooBlock bambooBlock -> {
          age = state.getOrEmpty(BambooBlock.AGE).orElse(0);
          maxAge = 1;
        }
        case NetherWartBlock wartBlock -> {
          age = state.getOrEmpty(NetherWartBlock.AGE).orElse(0);
          maxAge = NetherWartBlock.MAX_AGE;
        }
        case null, default -> {
          return false;
        }
      }

      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER_RAW.info("Block age: " + age + "/" + maxAge + " for block " + getBlockId(block));
      }

      // Regla: si age y maxAge son 0 => consideramos maduro
      if (age == 0 && maxAge == 0) {
        return true;
      }

      return age >= maxAge;
    } catch (NoSuchMethodError | Exception e) {
      CobbleUtils.LOGGER_RAW.error("Error checking if block is mature: " + getBlockId(block));
      e.printStackTrace();
      return false;
    }
  }


  private static final Block[] COLUMN_BLOCKS = {
    Blocks.BAMBOO,
    Blocks.CACTUS,
    Blocks.KELP,
    Blocks.SUGAR_CANE
  };

  /**
   * Determina si el bloque es de los que crecen en columnas.
   */
  public static boolean isColumnBlock(Block block) {
    return block instanceof CactusBlock
      || block instanceof SugarCaneBlock
      || block instanceof KelpBlock
      || block instanceof BambooBlock
      || block instanceof HeartyGrainsBlock;
  }

  /**
   * Devuelve el ID del bloque.
   */
  public static String getBlockId(Block block) {
    return Registries.BLOCK.getId(block).toString();
  }

  private static boolean needCheckPlacedByPlayer(Block block) {
    return block instanceof PumpkinBlock ||
      getBlockId(block).equals("minecraft:melon");
  }

  /**
   * Calcula cuántos bloques deben recolectarse automáticamente.
   * - Columnas: cuenta todos los bloques iguales hacia arriba.
   * - Cultivos: solo devuelve 1 si están maduros.
   */
  public static int getAmount(Block block, BlockPos pos, BlockState state, World world) {
    int amount = 0;

    if (isColumnBlock(block)) {
      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER_RAW.info("Starting column count for block at " + pos);
      }

      BlockPos checkPos = pos;

      while (checkPos.getY() < world.getTopY()) {
        BlockState currentState = checkPos.equals(pos) ? state : world.getBlockState(checkPos);
        Block stateBlock = currentState.getBlock();
        if (stateBlock.equals(Blocks.AIR)) break;
        if (!currentState.isOf(block) || !isMature(stateBlock, currentState)) break;
        if (!isBlockPlaceByPlayer(world, checkPos)) amount++;
        checkPos = checkPos.up();
      }
    } else if (needCheckPlacedByPlayer(block)) {
      if (!isBlockPlaceByPlayer(world, pos)) amount = 1;
    } else if (isMature(block, state)) {
      amount = 1;
    }


    return amount;
  }


  public static ItemStack getItemStack(Block block) {
    return block.asItem().getDefaultStack();
  }
}
