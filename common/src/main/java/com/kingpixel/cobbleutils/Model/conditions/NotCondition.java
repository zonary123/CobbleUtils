package com.kingpixel.cobbleutils.Model.conditions;

import lombok.*;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Builder
@Data
public class NotCondition extends Condition {
  public static final String TYPE = "NOT";
  @Builder.Default
  private Condition condition = null;

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    if (condition == null) return true;
    return !condition.check(player);
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    if (condition == null) return "No condition to negate";
    return "NOT: " + condition.getReason(player);
  }
}

