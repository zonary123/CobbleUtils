package com.kingpixel.cobbleutils.Model.Animations.core;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public abstract class Animation {
  public abstract void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                             List<ItemStack> allRewards, Runnable onComplete);
}
