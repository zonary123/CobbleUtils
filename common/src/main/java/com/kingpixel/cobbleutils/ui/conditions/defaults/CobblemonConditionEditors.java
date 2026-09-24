package com.kingpixel.cobbleutils.ui.conditions.defaults;

import com.kingpixel.cobbleutils.Model.PokemonBlackList;
import com.kingpixel.cobbleutils.Model.conditions.HasPokemonPartyAmountCondition;
import com.kingpixel.cobbleutils.Model.conditions.MolangCondition;
import com.kingpixel.cobbleutils.ui.conditions.ConditionEditor;
import com.kingpixel.cobbleutils.ui.conditions.ConditionEditorRegistry;
import com.kingpixel.cobbleutils.util.ChatPrompt;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ConditionEditor implementations for Cobblemon-specific conditions.
 */
public final class CobblemonConditionEditors {

  private CobblemonConditionEditors() {
  }

  public static void register() {
    registerHasPokemonPartyAmount();
    registerMolang();
  }

  private static void registerHasPokemonPartyAmount() {
    ConditionEditorRegistry.register(HasPokemonPartyAmountCondition.class, new ConditionEditor<HasPokemonPartyAmountCondition>() {
      @Override
      public ItemStack getIcon(HasPokemonPartyAmountCondition condition) {
        return new ItemStack(Items.LEAD);
      }

      @Override
      public List<String> getDetails(HasPokemonPartyAmountCondition condition) {
        PokemonBlackList filter = condition.getFilter();
        String pokemons = (filter != null && filter.getPokemons() != null && !filter.getPokemons().isEmpty())
          ? String.join(", ", filter.getPokemons())
          : "any";
        return List.of(
          "§7Required Amount: §f" + condition.getSize(),
          "§7Filter Species: §f" + pokemons
        );
      }

      @Override
      public void openEdit(ServerPlayerEntity player, HasPokemonPartyAmountCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter required amount (e.g. 3) or 'add:<pokemon>' to filter:", val -> {
          if (val != null && !val.trim().isEmpty()) {
            String str = val.trim();
            if (str.toLowerCase().startsWith("add:")) {
              String poke = str.substring("add:".length()).trim().toLowerCase();
              PokemonBlackList filter = condition.getFilter() != null ? condition.getFilter() : new PokemonBlackList();
              Set<String> set = new HashSet<>(filter.getPokemons() != null ? filter.getPokemons() : Set.of());
              set.add(poke);
              filter.setPokemons(set);
              condition.setFilter(filter);
              player.sendMessage(Text.literal("§a[Editor] Added Pokémon species to filter: " + poke));
            } else {
              try {
                int size = Integer.parseInt(str);
                condition.setSize(Math.max(1, size));
                player.sendMessage(Text.literal("§a[Editor] Required party amount set to: " + condition.getSize()));
              } catch (NumberFormatException ignored) {
                player.sendMessage(Text.literal("§c[Editor] Invalid number."));
              }
            }
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });
  }

  private static void registerMolang() {
    ConditionEditorRegistry.register(MolangCondition.class, new ConditionEditor<MolangCondition>() {
      @Override
      public ItemStack getIcon(MolangCondition condition) {
        return new ItemStack(Items.REPEATER);
      }

      @Override
      public List<String> getDetails(MolangCondition condition) {
        String expr = (condition.getExpression() != null && !condition.getExpression().isEmpty())
          ? String.join(" && ", condition.getExpression())
          : "none";
        return List.of("§7Molang: §f" + expr);
      }

      @Override
      public void openEdit(ServerPlayerEntity player, MolangCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter Molang expression string to add:", val -> {
          if (val != null && !val.trim().isEmpty()) {
            List<String> list = new ArrayList<>(condition.getExpression() != null ? condition.getExpression() : List.of());
            list.add(val.trim());
            condition.setExpression(list);
            player.sendMessage(Text.literal("§a[Editor] Added Molang expression."));
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });
  }
}
