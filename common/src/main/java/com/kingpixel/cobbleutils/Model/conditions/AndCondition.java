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
public class AndCondition extends Condition {
  public static final String TYPE = "AND";
  @Builder.Default
  private List<Condition> conditions = new ArrayList<>();

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    if (conditions == null || conditions.isEmpty()) return true;
    return conditions.stream().allMatch(c -> c.check(player));
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    if (conditions == null || conditions.isEmpty()) return "No conditions";
    StringBuilder sb = new StringBuilder("ALL of these must be true: ");
    for (Condition c : conditions) {
      if (!c.check(player)) {
        sb.append("\n  - ").append(c.getReason(player));
      }
    }
    return sb.toString();
  }
}

