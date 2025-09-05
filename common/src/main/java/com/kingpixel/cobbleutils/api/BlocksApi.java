package com.kingpixel.cobbleutils.api;

import com.cobblemon.mod.common.block.MedicinalLeekBlock;
import com.kingpixel.cobbleutils.database.DataBaseFactory;
import net.minecraft.block.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * API para calcular la cantidad de bloques de cultivo o tipo columna
 * que deben recolectarse automáticamente.
 */
public class BlocksApi {

  /**
   * Check if a block was placed by a player (not naturally generated).
   *
   * @param world the world
   * @param pos   the block position
   *
   * @return true if the block was placed by a player
   */
  public static boolean isBlockPlaceByPlayer(World world, BlockPos pos) {
    return DataBaseFactory.dataBaseBlock.isBlockPlaceByPlayer(world, pos);
  }

  /**
   * Check if the crop is mature.
   *
   * @param block the block
   * @param state the block state
   *
   * @return true if the crop is mature
   */
  public static boolean isMature(Block block, BlockState state) {
    int age;
    int maxAge;

    switch (block) {
      case KelpBlock kelpBlock -> {
        age = state.getOrEmpty(KelpBlock.AGE).orElse(0);
        maxAge = KelpBlock.MAX_AGE;
      }
      case MedicinalLeekBlock medicinalLeekBlock -> {
        age = medicinalLeekBlock.getAge(state);
        maxAge = medicinalLeekBlock.getMaxAge();
      }
      case CropBlock cropBlock -> {
        age = state.getOrEmpty(CropBlock.AGE).orElse(0);
        maxAge = CropBlock.MAX_AGE;
      }
      case CactusBlock cactusBlock -> {
        age = 0;
        maxAge = 0;
      }
      case SugarCaneBlock bambooBlock -> {
        age = state.getOrEmpty(SugarCaneBlock.AGE).orElse(0);
        maxAge = 1;
      }
      case PumpkinBlock pumpkinBlock -> {
        age = 0;
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
      case PlantBlock plantBlock -> {
        age = 0;
        maxAge = 0;
      }
      case null, default -> {
        return false;
      }
    }

    return age < maxAge;
  }

  /**
   * Determina si el bloque es de los que crecen en columnas.
   */
  public static boolean isColumnBlock(Block block) {
    return block instanceof BambooBlock
      || block instanceof CactusBlock
      || block instanceof KelpBlock
      || block instanceof SugarCaneBlock;
  }

  /**
   * Devuelve el ID del bloque.
   */
  public String getBlockId(Block block) {
    return Registries.BLOCK.getId(block).toString();
  }

  /**
   * Calcula cuántos bloques deben recolectarse automáticamente.
   * - Columnas: cuenta todos los bloques iguales hacia arriba.
   * - Cultivos: solo devuelve 1 si están maduros.
   */
  public int getAmount(Block block, BlockPos pos, World world) {
    int amount = 0;

    if (isColumnBlock(block)) {
      BlockPos checkPos = pos;
      while (true) {
        BlockState state = world.getBlockState(checkPos);
        if (state.isOf(block) && isMature(block, state) && !isBlockPlaceByPlayer(world, checkPos)) {
          amount++;
          checkPos = checkPos.up();
        } else {
          break;
        }
      }
    } else {
      BlockState state = world.getBlockState(pos);
      if (isMature(block, state) && !isBlockPlaceByPlayer(world, pos)) {
        amount = 1;
      }
    }

    return amount;
  }
}
