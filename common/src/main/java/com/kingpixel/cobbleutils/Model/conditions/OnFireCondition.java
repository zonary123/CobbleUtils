package com.kingpixel.cobbleutils.Model.conditions;

import lombok.*;
import net.minecraft.server.network.ServerPlayerEntity;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Builder
@Data
public class OnFireCondition extends Condition {
  public static final String TYPE = "ON_FIRE";
  @Builder.Default
  private boolean requiresOnFire = true;

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    return player.isOnFire() == requiresOnFire;
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return requiresOnFire ? "You must be on fire" : "You must not be on fire";
  }
}

