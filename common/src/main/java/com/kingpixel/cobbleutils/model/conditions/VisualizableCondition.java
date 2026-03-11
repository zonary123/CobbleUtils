package com.kingpixel.cobbleutils.model.conditions;

import net.minecraft.server.network.ServerPlayerEntity;

public interface VisualizableCondition {
  void render(ServerPlayerEntity player);
}