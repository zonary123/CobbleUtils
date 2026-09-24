package com.kingpixel.cobbleutils.ui.conditions.defaults;

import com.kingpixel.cobbleutils.Model.conditions.HasItemCondition;
import com.kingpixel.cobbleutils.Model.conditions.HoldingItemCondition;
import com.kingpixel.cobbleutils.ui.conditions.ConditionEditor;
import com.kingpixel.cobbleutils.ui.conditions.ConditionEditorRegistry;
import com.kingpixel.cobbleutils.util.ChatPrompt;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ConditionEditor implementations for item-related conditions.
 */
public final class ItemConditionEditors {

  private ItemConditionEditors() {
  }

  public static void register() {
    registerHasItem();
    registerHoldingItem();
  }

  private static void registerHasItem() {
    ConditionEditorRegistry.register(HasItemCondition.class, new ConditionEditor<HasItemCondition>() {
      @Override
      public ItemStack getIcon(HasItemCondition condition) {
        return new ItemStack(Items.CHEST);
      }

      @Override
      public List<String> getDetails(HasItemCondition condition) {
        String items = condition.getItemIds() != null && !condition.getItemIds().isEmpty()
          ? String.join(", ", condition.getItemIds())
          : "none";
        return List.of(
          "§7Items: §f" + items,
          "§7Min Amount: §f" + condition.getMinAmount()
        );
      }

      @Override
      public void openEdit(ServerPlayerEntity player, HasItemCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter item ID to toggle (e.g. minecraft:iron_ingot) or 'amount:<number>':", val -> {
          if (val != null && !val.trim().isEmpty()) {
            handleHasItemInput(player, condition, val.trim());
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });
  }

  private static void handleHasItemInput(ServerPlayerEntity player, HasItemCondition condition, String input) {
    if (input.toLowerCase().startsWith("amount:")) {
      try {
        int amt = Integer.parseInt(input.substring("amount:".length()).trim());
        condition.setMinAmount(Math.max(1, amt));
        player.sendMessage(Text.literal("§a[Editor] Min amount set to: " + condition.getMinAmount()));
      } catch (NumberFormatException ignored) {
        player.sendMessage(Text.literal("§c[Editor] Invalid amount."));
      }
      return;
    }

    Set<String> itemIds = new HashSet<>(condition.getItemIds() != null ? condition.getItemIds() : Set.of());
    String target = input.toLowerCase();
    if (itemIds.contains(target)) {
      itemIds.remove(target);
      player.sendMessage(Text.literal("§c[Editor] Removed item: " + target));
    } else {
      itemIds.add(target);
      player.sendMessage(Text.literal("§a[Editor] Added item: " + target));
    }
    condition.setItemIds(itemIds);
  }

  private static void registerHoldingItem() {
    ConditionEditorRegistry.register(HoldingItemCondition.class, new ConditionEditor<HoldingItemCondition>() {
      @Override
      public ItemStack getIcon(HoldingItemCondition condition) {
        return new ItemStack(Items.DIAMOND_SWORD);
      }

      @Override
      public List<String> getDetails(HoldingItemCondition condition) {
        String items = condition.getItemIds() != null && !condition.getItemIds().isEmpty()
          ? String.join(", ", condition.getItemIds())
          : "none";
        return List.of(
          "§7Items: §f" + items,
          "§7Main Hand: §f" + (condition.isMainHand() ? "§aYes" : "§cNo"),
          "§7Off Hand: §f" + (condition.isOffHand() ? "§aYes" : "§cNo")
        );
      }

      @Override
      public void openEdit(ServerPlayerEntity player, HoldingItemCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter item ID to toggle, or 'hand:main', 'hand:off', 'hand:both':", val -> {
          if (val != null && !val.trim().isEmpty()) {
            handleHoldingItemInput(player, condition, val.trim());
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });
  }

  private static void handleHoldingItemInput(ServerPlayerEntity player, HoldingItemCondition condition, String input) {
    String lower = input.toLowerCase();
    switch (lower) {
      case "hand:main" -> {
        condition.setMainHand(true);
        condition.setOffHand(false);
        player.sendMessage(Text.literal("§a[Editor] Set hand requirement: Main Hand only."));
      }
      case "hand:off" -> {
        condition.setMainHand(false);
        condition.setOffHand(true);
        player.sendMessage(Text.literal("§a[Editor] Set hand requirement: Off Hand only."));
      }
      case "hand:both" -> {
        condition.setMainHand(true);
        condition.setOffHand(true);
        player.sendMessage(Text.literal("§a[Editor] Set hand requirement: Either hand."));
      }
      default -> {
        Set<String> itemIds = new HashSet<>(condition.getItemIds() != null ? condition.getItemIds() : Set.of());
        if (itemIds.contains(lower)) {
          itemIds.remove(lower);
          player.sendMessage(Text.literal("§c[Editor] Removed item: " + lower));
        } else {
          itemIds.add(lower);
          player.sendMessage(Text.literal("§a[Editor] Added item: " + lower));
        }
        condition.setItemIds(itemIds);
      }
    }
  }
}
