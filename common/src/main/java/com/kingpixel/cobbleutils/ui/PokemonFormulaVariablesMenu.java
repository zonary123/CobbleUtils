package com.kingpixel.cobbleutils.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.PlaceholderButton;
import ca.landonjw.gooeylibs2.api.button.linked.LinkType;
import ca.landonjw.gooeylibs2.api.button.linked.LinkedPageButton;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.Model.PokemonFormula;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * UI to inspect PokemonFormula variables and resolved values for a concrete pokemon.
 */
public final class PokemonFormulaVariablesMenu {

  private PokemonFormulaVariablesMenu() {
  }

  public static void open(ServerPlayerEntity player, PokemonFormula formula, Pokemon pokemon) {
    if (player == null || formula == null || pokemon == null) {
      return;
    }

    List<PokemonFormula.VariableInfo> variableInfoList = formula.evaluateVariableInfo(pokemon);

    ChestTemplate template = ChestTemplate.builder(6).build();
    template.rectangle(0, 0, 5, 9, new PlaceholderButton());

    double formulaValue = formula.getPokemonValue(pokemon);
    List<String> resultLore = new ArrayList<>();
    resultLore.add("&7Pokemon: &f" + pokemon.getDisplayName(false).getString());
    resultLore.add("&7Formula: &e" + formula.getFormula());
    resultLore.add("&7Variables: &b" + variableInfoList.size());
    resultLore.add("&7Result: &a" + formatFormulaResult(formulaValue));

    template.set(51, GooeyButton.builder()
      .display(new ItemStack(Items.EMERALD))
      .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&aFormula Result"))
      .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(resultLore)))
      .build());

    List<Button> buttons = new ArrayList<>();
    for (PokemonFormula.VariableInfo info : variableInfoList) {
      List<String> lore = new ArrayList<>();
      lore.add("&7Description: &f" + info.description());
      lore.add("&7Value: &a" + info.value());
      lore.add("&7Category: &e" + info.category());
      lore.add("&7Source: &b" + info.source());
      lore.add("&7Priority: &6" + info.priority());
      lore.add("&7Key: &e" + info.key());

      ItemStack paper = new ItemStack(Items.PAPER);
      GooeyButton button = GooeyButton.builder()
        .display(paper)
        .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e" + info.key()))
        .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(lore)))
        .build();

      buttons.add(button);
    }

    ItemModel itemPrevious = CobbleUtils.language.getItemPrevious();
    template.set(45, LinkedPageButton.builder()
      .display(itemPrevious.getItemStack())
      .linkType(LinkType.Previous)
      .build());

    ItemModel itemClose = CobbleUtils.language.getItemClose();
    template.set(49, itemClose.getButton(action -> UIManager.closeUI(action.getPlayer()), 1, TimeUnit.SECONDS, 1));

    ItemModel itemNext = CobbleUtils.language.getItemNext();
    template.set(53, LinkedPageButton.builder()
      .display(itemNext.getItemStack())
      .linkType(LinkType.Next)
      .build());

    String title = "Formula Variables - " + pokemon.getDisplayName(false).getString();
    LinkedPage.Builder linkedPageBuilder = LinkedPage.builder().title(AdventureTranslator.toNative(title));

    CobbleUtils.server.execute(() ->
      UIManager.openUIForcefully(player,
        PaginationHelper.createPagesFromPlaceholders(template, buttons, linkedPageBuilder)));
  }

  private static String formatFormulaResult(double value) {
    if (!Double.isFinite(value)) {
      return String.valueOf(value);
    }
    return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
  }
}
