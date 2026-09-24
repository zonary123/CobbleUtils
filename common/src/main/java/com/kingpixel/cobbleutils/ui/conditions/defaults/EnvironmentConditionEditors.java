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
 * ConditionEditor implementations for environmental conditions.
 */
public final class EnvironmentConditionEditors {

  private EnvironmentConditionEditors() {
  }

  public static void register() {
    registerHeight();
    registerLightLevel();
    registerWeather();
    registerNearBlock();
    registerStructure();
    registerZone();
  }

  private static void registerHeight() {
    ConditionEditorRegistry.register(HeightCondition.class, new ConditionEditor<HeightCondition>() {
      @Override
      public ItemStack getIcon(HeightCondition condition) {
        return new ItemStack(Items.LADDER);
      }

      @Override
      public List<String> getDetails(HeightCondition condition) {
        return List.of(
          "§7Min Y: §f" + condition.getMinHeight(),
          "§7Max Y: §f" + condition.getMaxHeight()
        );
      }

      @Override
      public void openEdit(ServerPlayerEntity player, HeightCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter Y range as 'min:max' (e.g. -64:320):", val -> {
          if (val != null && !val.trim().isEmpty()) {
            parseMinMax(val.trim(), (min, max) -> {
              condition.setMinHeight(min);
              condition.setMaxHeight(max);
              player.sendMessage(Text.literal("§a[Editor] Height range set to: " + min + " to " + max));
            }, player);
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });
  }

  private static void registerLightLevel() {
    ConditionEditorRegistry.register(LightLevelCondition.class, new ConditionEditor<LightLevelCondition>() {
      @Override
      public ItemStack getIcon(LightLevelCondition condition) {
        return new ItemStack(Items.TORCH);
      }

      @Override
      public List<String> getDetails(LightLevelCondition condition) {
        return List.of(
          "§7Light Range: §f" + condition.getMinLight() + " - " + condition.getMaxLight(),
          "§7Light Type: §f" + condition.getLightType()
        );
      }

      @Override
      public void openEdit(ServerPlayerEntity player, LightLevelCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter light range 'min:max' (0-15) or 'type:BLOCK' / 'type:SKY':", val -> {
          if (val != null && !val.trim().isEmpty()) {
            String str = val.trim();
            if (str.toLowerCase().startsWith("type:")) {
              String type = str.substring("type:".length()).trim().toUpperCase();
              condition.setLightType(type);
              player.sendMessage(Text.literal("§a[Editor] Light type set to: " + type));
            } else {
              parseMinMax(str, (min, max) -> {
                condition.setMinLight(Math.max(0, Math.min(15, min)));
                condition.setMaxLight(Math.max(0, Math.min(15, max)));
                player.sendMessage(Text.literal("§a[Editor] Light range set to: " + condition.getMinLight() + " - " + condition.getMaxLight()));
              }, player);
            }
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });
  }

  private static void registerWeather() {
    ConditionEditorRegistry.register(WeatherCondition.class, new ConditionEditor<WeatherCondition>() {
      @Override
      public ItemStack getIcon(WeatherCondition condition) {
        return new ItemStack(Items.SUNFLOWER);
      }

      @Override
      public List<String> getDetails(WeatherCondition condition) {
        String weather = condition.isRequiresThundering() ? "Thunder"
          : (condition.isRequiresRaining() ? "Rain" : "Clear");
        return List.of("§7Required Weather: §f" + weather);
      }

      @Override
      public void openEdit(ServerPlayerEntity player, WeatherCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter weather mode ('clear', 'rain', 'thunder'):", val -> {
          if (val != null && !val.trim().isEmpty()) {
            switch (val.trim().toLowerCase()) {
              case "clear" -> {
                condition.setRequiresRaining(false);
                condition.setRequiresThundering(false);
                player.sendMessage(Text.literal("§a[Editor] Weather requirement set to: Clear"));
              }
              case "rain" -> {
                condition.setRequiresRaining(true);
                condition.setRequiresThundering(false);
                player.sendMessage(Text.literal("§a[Editor] Weather requirement set to: Rain"));
              }
              case "thunder" -> {
                condition.setRequiresRaining(true);
                condition.setRequiresThundering(true);
                player.sendMessage(Text.literal("§a[Editor] Weather requirement set to: Thunder"));
              }
              default -> player.sendMessage(Text.literal("§c[Editor] Unknown weather mode. Use clear, rain, or thunder."));
            }
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });
  }

  private static void registerNearBlock() {
    ConditionEditorRegistry.register(NearBlockCondition.class, new ConditionEditor<NearBlockCondition>() {
      @Override
      public ItemStack getIcon(NearBlockCondition condition) {
        return new ItemStack(Items.COBBLESTONE);
      }

      @Override
      public List<String> getDetails(NearBlockCondition condition) {
        String blocks = condition.getBlockIds() != null && !condition.getBlockIds().isEmpty()
          ? String.join(", ", condition.getBlockIds())
          : "none";
        return List.of(
          "§7Blocks: §f" + blocks,
          "§7Radius: §f" + condition.getRadius()
        );
      }

      @Override
      public void openEdit(ServerPlayerEntity player, NearBlockCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter block ID to toggle (e.g. minecraft:chest) or 'radius:<number>':", val -> {
          if (val != null && !val.trim().isEmpty()) {
            String str = val.trim();
            if (str.toLowerCase().startsWith("radius:")) {
              try {
                int r = Integer.parseInt(str.substring("radius:".length()).trim());
                condition.setRadius(Math.max(1, Math.min(32, r)));
                player.sendMessage(Text.literal("§a[Editor] Radius set to: " + condition.getRadius()));
              } catch (NumberFormatException ignored) {
                player.sendMessage(Text.literal("§c[Editor] Invalid radius number."));
              }
            } else {
              Set<String> set = new HashSet<>(condition.getBlockIds() != null ? condition.getBlockIds() : Set.of());
              String target = str.toLowerCase();
              if (set.contains(target)) {
                set.remove(target);
                player.sendMessage(Text.literal("§c[Editor] Removed block: " + target));
              } else {
                set.add(target);
                player.sendMessage(Text.literal("§a[Editor] Added block: " + target));
              }
              condition.setBlockIds(set);
            }
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });
  }

  private static void registerStructure() {
    ConditionEditorRegistry.register(StructureCondition.class, new ConditionEditor<StructureCondition>() {
      @Override
      public ItemStack getIcon(StructureCondition condition) {
        return new ItemStack(Items.MOSSY_COBBLESTONE);
      }

      @Override
      public List<String> getDetails(StructureCondition condition) {
        String structures = condition.getStructures() != null && !condition.getStructures().isEmpty()
          ? String.join(", ", condition.getStructures())
          : "none";
        return List.of("§7Structures: §f" + structures);
      }

      @Override
      public void openEdit(ServerPlayerEntity player, StructureCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter structure ID to toggle (e.g. minecraft:village):", val -> {
          if (val != null && !val.trim().isEmpty()) {
            Set<String> set = new HashSet<>(condition.getStructures() != null ? condition.getStructures() : Set.of());
            String target = val.trim().toLowerCase();
            if (set.contains(target)) {
              set.remove(target);
              player.sendMessage(Text.literal("§c[Editor] Removed structure: " + target));
            } else {
              set.add(target);
              player.sendMessage(Text.literal("§a[Editor] Added structure: " + target));
            }
            condition.setStructures(set);
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });
  }

  private static void registerZone() {
    ConditionEditorRegistry.register(ZoneCondition.class, new ConditionEditor<ZoneCondition>() {
      @Override
      public ItemStack getIcon(ZoneCondition condition) {
        return new ItemStack(Items.SPECTRAL_ARROW);
      }

      @Override
      public List<String> getDetails(ZoneCondition condition) {
        String shape = condition.getZone() != null ? condition.getZone().getClass().getSimpleName() : "none";
        return List.of("§7Zone Shape: §f" + shape);
      }

      @Override
      public void openEdit(ServerPlayerEntity player, ZoneCondition condition, Runnable onUpdated) {
        if (condition.getZone() != null) {
          condition.render(player);
          player.sendMessage(Text.literal("§a[Editor] Displaying zone particle boundaries!"));
        } else {
          player.sendMessage(Text.literal("§e[Editor] No zone shape assigned."));
        }
        if (onUpdated != null) onUpdated.run();
      }
    });
  }

  private interface MinMaxConsumer {
    void accept(int min, int max);
  }

  private static void parseMinMax(String input, MinMaxConsumer consumer, ServerPlayerEntity player) {
    try {
      if (input.contains(":")) {
        String[] parts = input.split(":", 2);
        int min = Integer.parseInt(parts[0].trim());
        int max = Integer.parseInt(parts[1].trim());
        consumer.accept(min, max);
      } else {
        int val = Integer.parseInt(input.trim());
        consumer.accept(val, val);
      }
    } catch (NumberFormatException e) {
      player.sendMessage(Text.literal("§c[Editor] Invalid numbers. Format: 'min:max'"));
    }
  }
}
