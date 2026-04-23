package com.kingpixel.cobbleutils.Model.conditions;

import lombok.*;
import net.minecraft.server.network.ServerPlayerEntity;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Builder
@Data
public class SneakingCondition extends Condition {
  public static final String TYPE = "SNEAKING";
  @Builder.Default
  private boolean requiresSneaking = true;

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    return player.isSneaking() == requiresSneaking;
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return requiresSneaking ? "You must be sneaking" : "You must not be sneaking";
  }
}

