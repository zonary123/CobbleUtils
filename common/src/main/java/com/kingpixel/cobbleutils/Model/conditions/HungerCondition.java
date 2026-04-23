package com.kingpixel.cobbleutils.Model.conditions;

import lombok.*;
import net.minecraft.server.network.ServerPlayerEntity;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Builder
@Data
public class HungerCondition extends Condition {
  public static final String TYPE = "HUNGER";
  @Builder.Default
  private int minHunger = 0;
  @Builder.Default
  private int maxHunger = 20;

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    int food = player.getHungerManager().getFoodLevel();
    return food >= minHunger && food <= maxHunger;
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "Your hunger must be between " + minHunger + " and " + maxHunger;
  }
}

