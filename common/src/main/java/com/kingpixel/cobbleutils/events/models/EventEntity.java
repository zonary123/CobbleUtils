package com.kingpixel.cobbleutils.events.models;

import lombok.Builder;
import lombok.Data;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;

@Data
@Builder
public class EventEntity {
  private ServerPlayerEntity player;
  private Entity entity;
}
