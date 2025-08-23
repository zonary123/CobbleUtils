package com.kingpixel.cobbleutils.api;

import com.kingpixel.cobbleutils.database.DataBaseFactory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * @author Carlos Varas Alonso - 23/08/2025 7:46
 */
public class BlocksApi {

  public static boolean isBlockPlaceByPlayer(World world, BlockPos pos) {
    return DataBaseFactory.INSTANCE.isBlockPlaceByPlayer(world, pos);
  }
}
