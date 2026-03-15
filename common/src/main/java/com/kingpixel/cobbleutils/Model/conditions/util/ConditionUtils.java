package com.kingpixel.cobbleutils.Model.conditions.util;

import com.kingpixel.cobbleutils.Model.conditions.Condition;
import com.kingpixel.cobbleutils.Model.conditions.VisualizableCondition;
import com.kingpixel.cobbleutils.adapter.ConditionAdapter;
import lombok.Data;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

@Data
public final class ConditionUtils {

  private ConditionUtils() {
  }

  public static void register(String id, Class<? extends Condition> clazz) {
    ConditionAdapter.register(id, clazz);
  }

  public static boolean check(List<Condition> conditions, ServerPlayerEntity player) {
    if (conditions == null || conditions.isEmpty()) return true;

    for (Condition condition : conditions) {
      if (!condition.check(player)) {
        if (condition instanceof VisualizableCondition visual) visual.render(player);
        return false;
      }
    }

    return true;
  }
}