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
public class OrCondition extends Condition {
  public static final String TYPE = "OR";
  @Builder.Default
  private List<Condition> conditions = new ArrayList<>();

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    if (conditions == null || conditions.isEmpty()) return true;
    return conditions.stream().anyMatch(c -> c.check(player));
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    if (conditions == null || conditions.isEmpty()) return "No conditions";
    StringBuilder sb = new StringBuilder("At least ONE of these must be true: ");
    for (Condition c : conditions) {
      sb.append("\n  - ").append(c.getReason(player));
    }
    return sb.toString();
  }
}

