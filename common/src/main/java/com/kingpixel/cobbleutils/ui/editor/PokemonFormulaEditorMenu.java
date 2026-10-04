package com.kingpixel.cobbleutils.ui.editor;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.PokemonFormula;
import com.kingpixel.cobbleutils.ui.PokemonFormulaVariablesMenu;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.ChatPrompt;
import com.kingpixel.cobbleutils.util.UIUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Reusable editor GUI for {@link PokemonFormula}.
 * Enables editing the mathematical expression, base values, shiny multipliers,
 * viewing dynamic variables, and evaluating live against party Pokémon.
 */
public final class PokemonFormulaEditorMenu {

  private PokemonFormulaEditorMenu() {
  }

  /**
   * Opens the PokemonFormula editor GUI.
   *
   * @param player  the player opening the editor
   * @param initial current formula configuration
   * @param onSave  callback with the updated formula
   * @param onBack  callback to return to previous menu
   */
  public static void open(ServerPlayerEntity player, PokemonFormula initial, Consumer<PokemonFormula> onSave, Runnable onBack) {
    if (player == null) return;

    PokemonFormula working = initial != null ? initial : new PokemonFormula();

    ChestTemplate template = ChestTemplate.builder(4).build();

    // 1. Math Formula String (Slot 10)
    ItemStack formulaItem = new ItemStack(Items.WRITABLE_BOOK);
    formulaItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e&lExpression Formula"));
    List<String> formulaLore = new ArrayList<>();
    formulaLore.add("&7Formula: &f" + (working.getFormula() != null ? working.getFormula() : "0"));
    formulaLore.add("");
    formulaLore.add("&eClick to edit math formula via chat.");
    formulaItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(formulaLore)));

    template.set(10, GooeyButton.builder()
      .display(formulaItem)
      .onClick(action -> ChatPrompt.prompt(player, "Enter mathematical formula string:", input -> {
        if (input != null && !input.trim().isEmpty()) {
          String expr = input.trim();
          try {
            // Validate syntax
            ExpressionBuilder builder = new ExpressionBuilder(expr);
            for (String variableKey : working.getAvailableVariables()) {
              builder.variable(variableKey);
            }
            builder.build();
            working.setFormula(expr);
            player.sendMessage(Text.literal("§a[Editor] Formula updated to: " + expr));
          } catch (Exception e) {
            player.sendMessage(Text.literal("§c[Editor] Invalid math syntax: " + e.getMessage()));
          }
        }
        open(player, working, onSave, onBack);
      }))
      .build());

    // 2. Base Value (Slot 12)
    ItemStack baseItem = new ItemStack(Items.GOLD_INGOT);
    baseItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e&lBase Value"));
    List<String> baseLore = new ArrayList<>();
    baseLore.add("&7Current Base: &a" + working.getBase());
    baseLore.add("");
    baseLore.add("&eClick to change base value.");
    baseItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(baseLore)));

    template.set(12, GooeyButton.builder()
      .display(baseItem)
      .onClick(action -> ChatPrompt.prompt(player, "Enter base numeric value (current: " + working.getBase() + "):", val -> {
        if (val != null && !val.trim().isEmpty()) {
          try {
            float b = Float.parseFloat(val.trim());
            working.setBase(b);
            player.sendMessage(Text.literal("§a[Editor] Base value set to: " + b));
          } catch (NumberFormatException ignored) {
            player.sendMessage(Text.literal("§c[Editor] Invalid number."));
          }
        }
        open(player, working, onSave, onBack);
      }))
      .build());

    // 3. Shiny Multipliers (Slot 14)
    ItemStack shinyItem = new ItemStack(Items.NETHER_STAR);
    shinyItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e&lShiny Multipliers"));
    List<String> shinyLore = new ArrayList<>();
    shinyLore.add("&7Shiny Bonus: &e" + working.getShiny());
    shinyLore.add("&7Not Shiny Bonus: &7" + working.getNotShiny());
    shinyLore.add("");
    shinyLore.add("&eClick to edit (format: 'shiny:notShiny').");
    shinyItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(shinyLore)));

    template.set(14, GooeyButton.builder()
      .display(shinyItem)
      .onClick(action -> ChatPrompt.prompt(player, "Enter 'shiny:notShiny' values (e.g. 2.0:1.0):", val -> {
        if (val != null && !val.trim().isEmpty()) {
          try {
            if (val.contains(":")) {
              String[] parts = val.split(":", 2);
              working.setShiny(Float.parseFloat(parts[0].trim()));
              working.setNotShiny(Float.parseFloat(parts[1].trim()));
            } else {
              working.setShiny(Float.parseFloat(val.trim()));
            }
            player.sendMessage(Text.literal("§a[Editor] Shiny multipliers updated."));
          } catch (Exception e) {
            player.sendMessage(Text.literal("§c[Editor] Invalid format."));
          }
        }
        open(player, working, onSave, onBack);
      }))
      .build());

    // 4. Ability Multipliers (Slot 16)
    ItemStack abilityItem = new ItemStack(Items.ENCHANTED_BOOK);
    abilityItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e&lHidden Ability Multipliers"));
    List<String> abilityLore = new ArrayList<>();
    abilityLore.add("&7Hidden Ability: &b" + working.getHiddenAbility());
    abilityLore.add("&7Regular Ability: &7" + working.getNotHiddenAbility());
    abilityLore.add("");
    abilityLore.add("&eClick to edit (format: 'ha:notHa').");
    abilityItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(abilityLore)));

    template.set(16, GooeyButton.builder()
      .display(abilityItem)
      .onClick(action -> ChatPrompt.prompt(player, "Enter 'ha:notHa' values (e.g. 1.5:1.0):", val -> {
        if (val != null && !val.trim().isEmpty()) {
          try {
            if (val.contains(":")) {
              String[] parts = val.split(":", 2);
              working.setHiddenAbility(Float.parseFloat(parts[0].trim()));
              working.setNotHiddenAbility(Float.parseFloat(parts[1].trim()));
            } else {
              working.setHiddenAbility(Float.parseFloat(val.trim()));
            }
            player.sendMessage(Text.literal("§a[Editor] Hidden ability multipliers updated."));
          } catch (Exception e) {
            player.sendMessage(Text.literal("§c[Editor] Invalid format."));
          }
        }
        open(player, working, onSave, onBack);
      }))
      .build());

    // 5. Variables Inspector & Debug (Slot 21)
    List<String> variables = working.getAvailableVariables();
    ItemStack varsItem = new ItemStack(Items.COMPASS);
    varsItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&b&lAvailable Variables"));
    List<String> varsLore = new ArrayList<>();
    varsLore.add("&7Registered Variables: &e" + variables.size());
    varsLore.add("");
    varsLore.add("&eClick to inspect variables with first party Pokémon.");
    varsItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(varsLore)));

    template.set(21, GooeyButton.builder()
      .display(varsItem)
      .onClick(action -> {
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        Pokemon first = party.get(0);
        if (first != null) {
          PokemonFormulaVariablesMenu.open(player, working, first);
        } else {
          player.sendMessage(Text.literal("§c[Editor] You have no Pokémon in your party to test variables."));
        }
      })
      .build());

    // 6. Live Test Evaluation (Slot 23)
    ItemStack evalItem = new ItemStack(Items.EMERALD);
    evalItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&a&lLive Formula Evaluation"));
    List<String> evalLore = new ArrayList<>();
    evalLore.add("&7Evaluates formula on your first party Pokémon.");
    evalLore.add("");
    evalLore.add("&eClick to evaluate.");
    evalItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(evalLore)));

    template.set(23, GooeyButton.builder()
      .display(evalItem)
      .onClick(action -> {
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        Pokemon first = party.get(0);
        if (first != null) {
          double val = working.getPokemonValue(first);
          player.sendMessage(Text.literal("§a[Formula Result] §e" + first.getDisplayName(false).getString() + " §7=> §b" + val));
        } else {
          player.sendMessage(Text.literal("§c[Editor] No Pokémon in party to evaluate."));
        }
      })
      .build());

    // 7. Cancel / Back Button (Slot 27)
    template.set(27, UIUtils.getCancelButton(action -> {
      if (onBack != null) onBack.run();
      else UIManager.closeUI(action.getPlayer());
    }));

    // 8. Save & Confirm Button (Slot 35)
    template.set(35, UIUtils.getConfirmButton(action -> {
      if (onSave != null) onSave.accept(working);
      player.sendMessage(Text.literal("§a[Editor] Formula saved successfully!"));
      if (onBack != null) onBack.run();
      else UIManager.closeUI(action.getPlayer());
    }));

    GooeyPage page = GooeyPage.builder()
      .template(template)
      .title(AdventureTranslator.toNative("&8Formula Editor"))
      .build();

    CobbleUtils.server.execute(() -> UIManager.openUIForcefully(player, page));
  }
}
