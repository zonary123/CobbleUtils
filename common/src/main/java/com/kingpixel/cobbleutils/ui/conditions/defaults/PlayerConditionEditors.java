package com.kingpixel.cobbleutils.ui.conditions.defaults;

import com.kingpixel.cobbleutils.Model.conditions.*;
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
 * ConditionEditor implementations for player state conditions.
 */
public final class PlayerConditionEditors {

  private PlayerConditionEditors() {
  }

  public static void register() {
    registerGameMode();
    registerHealth();
    registerHunger();
    registerEffect();
    registerSneaking();
    registerInWater();
    registerOnFire();
    registerStats();
  }

  private static void registerGameMode() {
    ConditionEditorRegistry.register(GameModeCondition.class, new ConditionEditor<GameModeCondition>() {
      @Override
      public ItemStack getIcon(GameModeCondition condition) {
        return new ItemStack(Items.COMPASS);
      }

      @Override
      public List<String> getDetails(GameModeCondition condition) {
        String modes = condition.getGameModes() != null && !condition.getGameModes().isEmpty()
          ? String.join(", ", condition.getGameModes())
          : "none";
        return List.of("§7GameModes: §f" + modes);
      }

      @Override
      public void openEdit(ServerPlayerEntity player, GameModeCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter game mode to toggle (survival, creative, adventure, spectator):", val -> {
          if (val != null && !val.trim().isEmpty()) {
            Set<String> set = new HashSet<>(condition.getGameModes() != null ? condition.getGameModes() : Set.of());
            String mode = val.trim().toLowerCase();
            if (set.contains(mode)) {
              set.remove(mode);
              player.sendMessage(Text.literal("§c[Editor] Removed gamemode: " + mode));
            } else {
              set.add(mode);
              player.sendMessage(Text.literal("§a[Editor] Added gamemode: " + mode));
            }
            condition.setGameModes(set);
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });
  }

  private static void registerHealth() {
    ConditionEditorRegistry.register(HealthCondition.class, new ConditionEditor<HealthCondition>() {
      @Override
      public ItemStack getIcon(HealthCondition condition) {
        return new ItemStack(Items.GOLDEN_APPLE);
      }

      @Override
      public List<String> getDetails(HealthCondition condition) {
        return List.of(
          "§7Min Health: §f" + condition.getMinHealth(),
          "§7Max Health: §f" + condition.getMaxHealth()
        );
      }

      @Override
      public void openEdit(ServerPlayerEntity player, HealthCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter health range as 'min:max' (e.g. 5.0:20.0):", val -> {
          if (val != null && !val.trim().isEmpty()) {
            parseRange(val.trim(), (min, max) -> {
              condition.setMinHealth(min.floatValue());
              condition.setMaxHealth(max.floatValue());
              player.sendMessage(Text.literal("§a[Editor] Health range set to: " + min + " - " + max));
            }, player);
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });
  }

  private static void registerHunger() {
    ConditionEditorRegistry.register(HungerCondition.class, new ConditionEditor<HungerCondition>() {
      @Override
      public ItemStack getIcon(HungerCondition condition) {
        return new ItemStack(Items.COOKED_BEEF);
      }

      @Override
      public List<String> getDetails(HungerCondition condition) {
        return List.of(
          "§7Min Hunger: §f" + condition.getMinHunger(),
          "§7Max Hunger: §f" + condition.getMaxHunger()
        );
      }

      @Override
      public void openEdit(ServerPlayerEntity player, HungerCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter hunger range as 'min:max' (e.g. 10:20):", val -> {
          if (val != null && !val.trim().isEmpty()) {
            parseRange(val.trim(), (min, max) -> {
              condition.setMinHunger(min.intValue());
              condition.setMaxHunger(max.intValue());
              player.sendMessage(Text.literal("§a[Editor] Hunger range set to: " + min.intValue() + " - " + max.intValue()));
            }, player);
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });
  }

  private static void registerEffect() {
    ConditionEditorRegistry.register(EffectCondition.class, new ConditionEditor<EffectCondition>() {
      @Override
      public ItemStack getIcon(EffectCondition condition) {
        return new ItemStack(Items.POTION);
      }

      @Override
      public List<String> getDetails(EffectCondition condition) {
        String effects = condition.getEffects() != null && !condition.getEffects().isEmpty()
          ? String.join(", ", condition.getEffects())
          : "none";
        return List.of(
          "§7Effects: §f" + effects,
          "§7Min Amplifier: §f" + condition.getMinAmplifier()
        );
      }

      @Override
      public void openEdit(ServerPlayerEntity player, EffectCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter effect ID to toggle (e.g. minecraft:speed) or 'amp:<number>':", val -> {
          if (val != null && !val.trim().isEmpty()) {
            String input = val.trim();
            if (input.toLowerCase().startsWith("amp:")) {
              try {
                int amp = Integer.parseInt(input.substring("amp:".length()).trim());
                condition.setMinAmplifier(Math.max(0, amp));
                player.sendMessage(Text.literal("§a[Editor] Min amplifier set to: " + condition.getMinAmplifier()));
              } catch (NumberFormatException ignored) {
                player.sendMessage(Text.literal("§c[Editor] Invalid amplifier number."));
              }
            } else {
              Set<String> set = new HashSet<>(condition.getEffects() != null ? condition.getEffects() : Set.of());
              String target = input.toLowerCase();
              if (set.contains(target)) {
                set.remove(target);
                player.sendMessage(Text.literal("§c[Editor] Removed effect: " + target));
              } else {
                set.add(target);
                player.sendMessage(Text.literal("§a[Editor] Added effect: " + target));
              }
              condition.setEffects(set);
            }
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });
  }

  private static void registerSneaking() {
    ConditionEditorRegistry.register(SneakingCondition.class, new ConditionEditor<SneakingCondition>() {
      @Override
      public ItemStack getIcon(SneakingCondition condition) {
        return new ItemStack(Items.LEATHER_BOOTS);
      }

      @Override
      public List<String> getDetails(SneakingCondition condition) {
        return List.of("§7Requires Sneaking: §f" + (condition.isRequiresSneaking() ? "§aYes" : "§cNo"));
      }

      @Override
      public void openEdit(ServerPlayerEntity player, SneakingCondition condition, Runnable onUpdated) {
        condition.setRequiresSneaking(!condition.isRequiresSneaking());
        player.sendMessage(Text.literal("§a[Editor] Toggled requires sneaking: " + (condition.isRequiresSneaking() ? "Yes" : "No")));
        if (onUpdated != null) onUpdated.run();
      }
    });
  }

  private static void registerInWater() {
    ConditionEditorRegistry.register(InWaterCondition.class, new ConditionEditor<InWaterCondition>() {
      @Override
      public ItemStack getIcon(InWaterCondition condition) {
        return new ItemStack(Items.WATER_BUCKET);
      }

      @Override
      public List<String> getDetails(InWaterCondition condition) {
        return List.of("§7Requires In Water: §f" + (condition.isRequiresInWater() ? "§aYes" : "§cNo"));
      }

      @Override
      public void openEdit(ServerPlayerEntity player, InWaterCondition condition, Runnable onUpdated) {
        condition.setRequiresInWater(!condition.isRequiresInWater());
        player.sendMessage(Text.literal("§a[Editor] Toggled requires in water: " + (condition.isRequiresInWater() ? "Yes" : "No")));
        if (onUpdated != null) onUpdated.run();
      }
    });
  }

  private static void registerOnFire() {
    ConditionEditorRegistry.register(OnFireCondition.class, new ConditionEditor<OnFireCondition>() {
      @Override
      public ItemStack getIcon(OnFireCondition condition) {
        return new ItemStack(Items.FLINT_AND_STEEL);
      }

      @Override
      public List<String> getDetails(OnFireCondition condition) {
        return List.of("§7Requires On Fire: §f" + (condition.isRequiresOnFire() ? "§aYes" : "§cNo"));
      }

      @Override
      public void openEdit(ServerPlayerEntity player, OnFireCondition condition, Runnable onUpdated) {
        condition.setRequiresOnFire(!condition.isRequiresOnFire());
        player.sendMessage(Text.literal("§a[Editor] Toggled requires on fire: " + (condition.isRequiresOnFire() ? "Yes" : "No")));
        if (onUpdated != null) onUpdated.run();
      }
    });
  }

  private static void registerStats() {
    ConditionEditorRegistry.register(StatsCondition.class, new ConditionEditor<StatsCondition>() {
      @Override
      public ItemStack getIcon(StatsCondition condition) {
        return new ItemStack(Items.BOOK);
      }

      @Override
      public List<String> getDetails(StatsCondition condition) {
        return List.of(
          "§7Type: §f" + (condition.getTypeId() != null && !condition.getTypeId().isEmpty() ? condition.getTypeId() : "custom"),
          "§7Stat: §f" + (condition.getStatId() != null && !condition.getStatId().isEmpty() ? condition.getStatId() : "none"),
          "§7Required: §f" + condition.getRequiredValue()
        );
      }

      @Override
      public void openEdit(ServerPlayerEntity player, StatsCondition condition, Runnable onUpdated) {
        player.sendMessage(Text.literal("§e[Editor] StatsCondition uses static definition in current version."));
        if (onUpdated != null) onUpdated.run();
      }
    });
  }

  private interface RangeConsumer {
    void accept(Double min, Double max);
  }

  private static void parseRange(String input, RangeConsumer consumer, ServerPlayerEntity player) {
    try {
      if (input.contains(":")) {
        String[] parts = input.split(":", 2);
        double min = Double.parseDouble(parts[0].trim());
        double max = Double.parseDouble(parts[1].trim());
        consumer.accept(min, max);
      } else {
        double val = Double.parseDouble(input.trim());
        consumer.accept(val, val);
      }
    } catch (NumberFormatException e) {
      player.sendMessage(Text.literal("§c[Editor] Invalid numbers. Format: 'min:max'"));
    }
  }
}
