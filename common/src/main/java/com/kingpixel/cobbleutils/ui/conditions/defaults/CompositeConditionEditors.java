package com.kingpixel.cobbleutils.ui.conditions.defaults;

import com.kingpixel.cobbleutils.Model.conditions.AndCondition;
import com.kingpixel.cobbleutils.Model.conditions.Condition;
import com.kingpixel.cobbleutils.Model.conditions.NotCondition;
import com.kingpixel.cobbleutils.Model.conditions.OrCondition;
import com.kingpixel.cobbleutils.ui.conditions.ConditionEditor;
import com.kingpixel.cobbleutils.ui.conditions.ConditionEditorRegistry;
import com.kingpixel.cobbleutils.ui.conditions.ConditionListMenu;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * ConditionEditor implementations for logical composite conditions (AND, OR, NOT).
 */
public final class CompositeConditionEditors {

  private CompositeConditionEditors() {
  }

  public static void register() {
    registerAnd();
    registerOr();
    registerNot();
  }

  private static void registerAnd() {
    ConditionEditorRegistry.register(AndCondition.class, new ConditionEditor<AndCondition>() {
      @Override
      public ItemStack getIcon(AndCondition condition) {
        return new ItemStack(Items.REPEATING_COMMAND_BLOCK);
      }

      @Override
      public List<String> getDetails(AndCondition condition) {
        int count = condition.getConditions() != null ? condition.getConditions().size() : 0;
        return List.of(
          "§7Nested Rules (ALL must match): §f" + count,
          "§eLeft-click to open nested editor."
        );
      }

      @Override
      public void openEdit(ServerPlayerEntity player, AndCondition condition, Runnable onUpdated) {
        if (condition.getConditions() == null) {
          condition.setConditions(new ArrayList<>());
        }
        ConditionListMenu.open(player, condition.getConditions(), "AND Conditions", updatedList -> {
          condition.setConditions(updatedList);
          if (onUpdated != null) onUpdated.run();
        });
      }
    });
  }

  private static void registerOr() {
    ConditionEditorRegistry.register(OrCondition.class, new ConditionEditor<OrCondition>() {
      @Override
      public ItemStack getIcon(OrCondition condition) {
        return new ItemStack(Items.CHAIN_COMMAND_BLOCK);
      }

      @Override
      public List<String> getDetails(OrCondition condition) {
        int count = condition.getConditions() != null ? condition.getConditions().size() : 0;
        return List.of(
          "§7Nested Rules (ANY must match): §f" + count,
          "§eLeft-click to open nested editor."
        );
      }

      @Override
      public void openEdit(ServerPlayerEntity player, OrCondition condition, Runnable onUpdated) {
        if (condition.getConditions() == null) {
          condition.setConditions(new ArrayList<>());
        }
        ConditionListMenu.open(player, condition.getConditions(), "OR Conditions", updatedList -> {
          condition.setConditions(updatedList);
          if (onUpdated != null) onUpdated.run();
        });
      }
    });
  }

  private static void registerNot() {
    ConditionEditorRegistry.register(NotCondition.class, new ConditionEditor<NotCondition>() {
      @Override
      public ItemStack getIcon(NotCondition condition) {
        return new ItemStack(Items.BARRIER);
      }

      @Override
      public List<String> getDetails(NotCondition condition) {
        Condition inner = condition.getCondition();
        String innerType = inner != null ? inner.getType() : "none";
        List<String> list = new ArrayList<>();
        list.add("§7Negated Condition: §f" + innerType);
        if (inner != null) {
          list.addAll(ConditionEditorRegistry.getDetails(inner));
        }
        return list;
      }

      @Override
      public void openEdit(ServerPlayerEntity player, NotCondition condition, Runnable onUpdated) {
        Condition inner = condition.getCondition();
        if (inner != null) {
          ConditionEditorRegistry.openEdit(player, inner, onUpdated);
        } else {
          player.sendMessage(Text.literal("§e[Editor] No inner condition to negate yet."));
          if (onUpdated != null) onUpdated.run();
        }
      }
    });
  }
}
