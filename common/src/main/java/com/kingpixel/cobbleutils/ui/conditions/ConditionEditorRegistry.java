package com.kingpixel.cobbleutils.ui.conditions;

import com.kingpixel.cobbleutils.Model.PokemonBlackList;
import com.kingpixel.cobbleutils.Model.conditions.*;
import com.kingpixel.cobbleutils.ui.conditions.defaults.CobblemonConditionEditors;
import com.kingpixel.cobbleutils.ui.conditions.defaults.CompositeConditionEditors;
import com.kingpixel.cobbleutils.ui.conditions.defaults.EnvironmentConditionEditors;
import com.kingpixel.cobbleutils.ui.conditions.defaults.ItemConditionEditors;
import com.kingpixel.cobbleutils.ui.conditions.defaults.PlayerConditionEditors;
import com.kingpixel.cobbleutils.ui.conditions.defaults.TimeConditionEditors;
import com.kingpixel.cobbleutils.util.ChatPrompt;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry mapping Condition types to their respective ConditionEditor implementations.
 * Enables mods to register UI adapters for custom conditions without modifying menus.
 */
public final class ConditionEditorRegistry {
  private static final Map<Class<? extends Condition>, ConditionEditor<?>> EDITORS = new ConcurrentHashMap<>();

  private static final ConditionEditor<Condition> DEFAULT_FALLBACK = new ConditionEditor<>() {
    @Override
    public List<String> getDetails(Condition condition) {
      return List.of("§7Type: §e" + (condition != null ? condition.getType() : "Unknown"));
    }

    @Override
    public void openEdit(ServerPlayerEntity player, Condition condition, Runnable onUpdated) {
      if (player != null && condition != null) {
        player.sendMessage(Text.literal("§e[Editor] No custom fields to edit for type: " + condition.getType()));
      }
      if (onUpdated != null) onUpdated.run();
    }
  };

  static {
    registerDefaults();
  }

  private ConditionEditorRegistry() {
  }

  /**
   * Registers a ConditionEditor for a specific Condition class.
   *
   * @param clazz the condition class
   * @param editor the editor adapter
   * @param <T> the condition type
   */
  public static <T extends Condition> void register(Class<T> clazz, ConditionEditor<T> editor) {
    if (clazz != null && editor != null) {
      EDITORS.put(clazz, editor);
    }
  }

  /**
   * Retrieves the registered editor for a condition instance.
   *
   * @param condition the condition instance
   * @return matching editor, or fallback if unregistered
   */
  @SuppressWarnings("unchecked")
  public static <T extends Condition> ConditionEditor<T> getEditor(T condition) {
    if (condition == null) return (ConditionEditor<T>) DEFAULT_FALLBACK;
    ConditionEditor<?> editor = EDITORS.get(condition.getClass());
    if (editor != null) {
      return (ConditionEditor<T>) editor;
    }
    // Search assignable classes
    for (Map.Entry<Class<? extends Condition>, ConditionEditor<?>> entry : EDITORS.entrySet()) {
      if (entry.getKey().isAssignableFrom(condition.getClass())) {
        return (ConditionEditor<T>) entry.getValue();
      }
    }
    return (ConditionEditor<T>) DEFAULT_FALLBACK;
  }

  /**
   * Returns formatted details for a condition.
   */
  public static <T extends Condition> List<String> getDetails(T condition) {
    ConditionEditor<T> editor = getEditor(condition);
    return editor.getDetails(condition);
  }

  /**
   * Returns the icon for a condition.
   */
  public static <T extends Condition> ItemStack getIcon(T condition) {
    ConditionEditor<T> editor = getEditor(condition);
    return editor.getIcon(condition);
  }

  /**
   * Opens the edit handler for a condition.
   */
  public static <T extends Condition> void openEdit(ServerPlayerEntity player, T condition, Runnable onUpdated) {
    ConditionEditor<T> editor = getEditor(condition);
    editor.openEdit(player, condition, onUpdated);
  }

  /**
   * Returns an unmodifiable copy of registered editor types.
   */
  public static Map<Class<? extends Condition>, ConditionEditor<?>> getRegisteredEditors() {
    return Collections.unmodifiableMap(EDITORS);
  }

  private static void registerDefaults() {
    // 1. PermissionCondition
    register(PermissionCondition.class, new ConditionEditor<PermissionCondition>() {
      @Override
      public ItemStack getIcon(PermissionCondition condition) {
        return new ItemStack(Items.NAME_TAG);
      }

      @Override
      public List<String> getDetails(PermissionCondition condition) {
        return List.of("§7Permission: §f" + (condition.getPermission() != null ? condition.getPermission() : "none"));
      }

      @Override
      public void openEdit(ServerPlayerEntity player, PermissionCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter permission node (e.g. vip.feature):", val -> {
          if (val != null && !val.trim().isEmpty()) {
            condition.setPermission(val.trim());
            player.sendMessage(Text.literal("§a[Editor] Permission updated to: " + val.trim()));
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });

    // 2. ExperienceLevelCondition
    register(ExperienceLevelCondition.class, new ConditionEditor<ExperienceLevelCondition>() {
      @Override
      public ItemStack getIcon(ExperienceLevelCondition condition) {
        return new ItemStack(Items.EXPERIENCE_BOTTLE);
      }

      @Override
      public List<String> getDetails(ExperienceLevelCondition condition) {
        return List.of(
          "§7Min Level: §f" + condition.getMinLevel(),
          "§7Max Level: §f" + condition.getMaxLevel()
        );
      }

      @Override
      public void openEdit(ServerPlayerEntity player, ExperienceLevelCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter min required level (integer):", val -> {
          if (val != null) {
            try {
              condition.setMinLevel(Math.max(0, Integer.parseInt(val.trim())));
              player.sendMessage(Text.literal("§a[Editor] Min level set to: " + condition.getMinLevel()));
            } catch (NumberFormatException ignored) {
              player.sendMessage(Text.literal("§c[Editor] Invalid number."));
            }
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });

    // 3. WorldCondition
    register(WorldCondition.class, new ConditionEditor<WorldCondition>() {
      @Override
      public ItemStack getIcon(WorldCondition condition) {
        return new ItemStack(Items.GRASS_BLOCK);
      }

      @Override
      public List<String> getDetails(WorldCondition condition) {
        return List.of("§7Worlds: §f" + (condition.getWorlds() != null ? String.join(", ", condition.getWorlds()) : "all"));
      }

      @Override
      public void openEdit(ServerPlayerEntity player, WorldCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter world ID to toggle (e.g. minecraft:overworld):", val -> {
          if (val != null && !val.trim().isEmpty()) {
            Set<String> worlds = new HashSet<>(condition.getWorlds() != null ? condition.getWorlds() : Set.of());
            String target = val.trim().toLowerCase();
            if (worlds.contains(target)) {
              worlds.remove(target);
              player.sendMessage(Text.literal("§c[Editor] Removed world: " + target));
            } else {
              worlds.add(target);
              player.sendMessage(Text.literal("§a[Editor] Added world: " + target));
            }
            condition.setWorlds(worlds);
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });

    // 4. ServerCondition
    register(ServerCondition.class, new ConditionEditor<ServerCondition>() {
      @Override
      public ItemStack getIcon(ServerCondition condition) {
        return new ItemStack(Items.IRON_BARS);
      }

      @Override
      public List<String> getDetails(ServerCondition condition) {
        return List.of("§7Servers: §f" + (condition.getServers() != null ? String.join(", ", condition.getServers()) : "any"));
      }

      @Override
      public void openEdit(ServerPlayerEntity player, ServerCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter server name to toggle (or 'all'):", val -> {
          if (val != null && !val.trim().isEmpty()) {
            Set<String> servers = new HashSet<>(condition.getServers() != null ? condition.getServers() : Set.of());
            String target = val.trim();
            if (servers.contains(target)) {
              servers.remove(target);
              player.sendMessage(Text.literal("§c[Editor] Removed server: " + target));
            } else {
              servers.add(target);
              player.sendMessage(Text.literal("§a[Editor] Added server: " + target));
            }
            condition.setServers(servers);
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });

    // 5. BiomeCondition
    register(BiomeCondition.class, new ConditionEditor<BiomeCondition>() {
      @Override
      public ItemStack getIcon(BiomeCondition condition) {
        return new ItemStack(Items.OAK_SAPLING);
      }

      @Override
      public List<String> getDetails(BiomeCondition condition) {
        return List.of("§7Biomes: §f" + (condition.getBiomes() != null ? String.join(", ", condition.getBiomes()) : "any"));
      }

      @Override
      public void openEdit(ServerPlayerEntity player, BiomeCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter biome ID to toggle (e.g. minecraft:plains):", val -> {
          if (val != null && !val.trim().isEmpty()) {
            Set<String> biomes = new HashSet<>(condition.getBiomes() != null ? condition.getBiomes() : Set.of());
            String target = val.trim().toLowerCase();
            if (biomes.contains(target)) {
              biomes.remove(target);
              player.sendMessage(Text.literal("§c[Editor] Removed biome: " + target));
            } else {
              biomes.add(target);
              player.sendMessage(Text.literal("§a[Editor] Added biome: " + target));
            }
            condition.setBiomes(biomes);
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });

    // 6. RandomChanceCondition
    register(RandomChanceCondition.class, new ConditionEditor<RandomChanceCondition>() {
      @Override
      public ItemStack getIcon(RandomChanceCondition condition) {
        return new ItemStack(Items.GOLD_NUGGET);
      }

      @Override
      public List<String> getDetails(RandomChanceCondition condition) {
        return List.of("§7Chance: §f" + condition.getChance() + "%");
      }

      @Override
      public void openEdit(ServerPlayerEntity player, RandomChanceCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter chance percentage (0.0 - 100.0):", val -> {
          if (val != null) {
            try {
              double chance = Double.parseDouble(val.trim());
              condition.setChance(Math.max(0.0, Math.min(100.0, chance)));
              player.sendMessage(Text.literal("§a[Editor] Chance set to: " + condition.getChance() + "%"));
            } catch (NumberFormatException ignored) {
              player.sendMessage(Text.literal("§c[Editor] Invalid number."));
            }
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });

    // 7. PokemonLevelCondition
    register(PokemonLevelCondition.class, new ConditionEditor<PokemonLevelCondition>() {
      @Override
      public ItemStack getIcon(PokemonLevelCondition condition) {
        return new ItemStack(Items.DIAMOND);
      }

      @Override
      public List<String> getDetails(PokemonLevelCondition condition) {
        return List.of(
          "§7Min Pokémon Level: §f" + condition.getMinLevel(),
          "§7Max Pokémon Level: §f" + condition.getMaxLevel(),
          "§7Any in Party: §f" + (condition.isAnyInParty() ? "§aYes" : "§cNo")
        );
      }

      @Override
      public void openEdit(ServerPlayerEntity player, PokemonLevelCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter min Pokémon level (1-100):", val -> {
          if (val != null) {
            try {
              condition.setMinLevel(Math.max(1, Math.min(100, Integer.parseInt(val.trim()))));
              player.sendMessage(Text.literal("§a[Editor] Min Pokémon level set to: " + condition.getMinLevel()));
            } catch (NumberFormatException ignored) {
              player.sendMessage(Text.literal("§c[Editor] Invalid number."));
            }
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });

    // 8. HasPokemonPartyCondition
    register(HasPokemonPartyCondition.class, new ConditionEditor<HasPokemonPartyCondition>() {
      @Override
      public ItemStack getIcon(HasPokemonPartyCondition condition) {
        return new ItemStack(Items.LEAD);
      }

      @Override
      public List<String> getDetails(HasPokemonPartyCondition condition) {
        PokemonBlackList filter = condition.getFilter();
        String pokemons = (filter != null && filter.getPokemons() != null && !filter.getPokemons().isEmpty())
          ? String.join(", ", filter.getPokemons())
          : "any";
        return List.of("§7Matching Pokémon: §f" + pokemons);
      }

      @Override
      public void openEdit(ServerPlayerEntity player, HasPokemonPartyCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter Pokémon species to match (e.g. pikachu or * for any):", val -> {
          if (val != null && !val.trim().isEmpty()) {
            PokemonBlackList filter = condition.getFilter() != null ? condition.getFilter() : new PokemonBlackList();
            Set<String> set = new HashSet<>(filter.getPokemons() != null ? filter.getPokemons() : Set.of());
            set.add(val.trim().toLowerCase());
            filter.setPokemons(set);
            condition.setFilter(filter);
            player.sendMessage(Text.literal("§a[Editor] Added Pokémon filter: " + val.trim()));
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });

    // Register modular default editors
    ItemConditionEditors.register();
    PlayerConditionEditors.register();
    EnvironmentConditionEditors.register();
    TimeConditionEditors.register();
    CobblemonConditionEditors.register();
    CompositeConditionEditors.register();
  }
}
