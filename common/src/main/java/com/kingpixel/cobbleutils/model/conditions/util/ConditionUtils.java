package com.kingpixel.cobbleutils.model.conditions.util;

import com.kingpixel.cobbleutils.model.conditions.Condition;
import com.kingpixel.cobbleutils.model.conditions.VisualizableCondition;
import lombok.Data;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

@Data
public final class ConditionUtils {

  private ConditionUtils() {
  }

  public static boolean check(List<Condition> conditions, ServerPlayerEntity player) {
    if (conditions == null || conditions.isEmpty()) return true;

    for (Condition condition : conditions) {
      if (!condition.check(player)) {

        if (condition instanceof VisualizableCondition visual) {
          visual.render(player);
        }

        return false;
      }
    }

    return true;
  }
}