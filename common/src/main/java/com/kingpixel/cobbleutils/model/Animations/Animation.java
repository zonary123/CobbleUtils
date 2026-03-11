package com.kingpixel.cobbleutils.model.Animations;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 02/05/2025 9:17
 */
public abstract class Animation {
  private ServerPlayerEntity player;
  private Vec3d position;
  private List<ItemStack> obtained;
  private List<ItemStack> allRewards;

  public abstract void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                             List<ItemStack> allRewards);

  public Vec3d getPos() {
    if (player == null) {
      return position;
    }
    return player.getPos().add(position);
  }
}
