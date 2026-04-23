package com.kingpixel.cobbleutils.Model.conditions;

import lombok.*;
import net.minecraft.server.network.ServerPlayerEntity;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Builder
@Data
public class InWaterCondition extends Condition {
  public static final String TYPE = "IN_WATER";
  @Builder.Default
  private boolean requiresInWater = true;

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    return player.isTouchingWater() == requiresInWater;
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return requiresInWater ? "You must be in water" : "You must not be in water";
  }
}

