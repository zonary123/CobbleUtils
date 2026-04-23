package com.kingpixel.cobbleutils.Model.conditions;

import lombok.*;
import net.minecraft.server.network.ServerPlayerEntity;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Builder
@Data
public class ExperienceLevelCondition extends Condition {
  public static final String TYPE = "EXPERIENCE_LEVEL";
  @Builder.Default
  private int minLevel = 0;
  @Builder.Default
  private int maxLevel = Integer.MAX_VALUE;

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    int level = player.experienceLevel;
    return level >= minLevel && level <= maxLevel;
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "Your experience level must be between " + minLevel + " and " + maxLevel;
  }
}

