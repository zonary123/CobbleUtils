package com.kingpixel.cobbleutils.events.models;

import lombok.Builder;
import lombok.Data;
import net.minecraft.server.network.ServerPlayerEntity;

@Data
@Builder
public class EventTravel {
  private ServerPlayerEntity player;
  private double distance;
}
