package com.kingpixel.cobbleutils.Model.conditions;

import com.kingpixel.cobbleutils.util.MinecraftUtils;
import lombok.*;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 *
 * @author Carlos Varas Alonso - 13/06/2026 13:21
 */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@ToString(callSuper = true)
@Builder
@Data
public class StatsCondition extends Condition {
  public static final String TYPE = "STATS";
  private final String typeId = "";
  private final String statId = "";
  private final int requiredValue = 0;

  @Override public String getType() {
    return TYPE;
  }

  @Override public boolean check(ServerPlayerEntity player) {
    return MinecraftUtils.getStatValue(player, typeId, statId) >= requiredValue;
  }

  @Override public String getReason(ServerPlayerEntity player) {
    return "Stats condition not implemented yet";
  }
}
