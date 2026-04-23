package com.kingpixel.cobbleutils.Model.conditions;

import lombok.*;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Random;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Builder
@Data
public class RandomChanceCondition extends Condition {
  public static final String TYPE = "RANDOM_CHANCE";
  private static final Random RANDOM = new Random();
  @Builder.Default
  private double chance = 0.5;

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    return RANDOM.nextDouble() < chance;
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "Random chance (" + (chance * 100) + "%) was not met";
  }
}

