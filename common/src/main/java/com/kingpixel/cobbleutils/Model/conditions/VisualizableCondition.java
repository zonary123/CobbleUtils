package com.kingpixel.cobbleutils.Model.conditions;

import net.minecraft.server.network.ServerPlayerEntity;

public interface VisualizableCondition {
  void render(ServerPlayerEntity player);
}