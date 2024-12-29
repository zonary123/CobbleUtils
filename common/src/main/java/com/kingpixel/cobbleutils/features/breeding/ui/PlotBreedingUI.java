package com.kingpixel.cobbleutils.features.breeding.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.database.DatabaseClientFactory;
import com.kingpixel.cobbleutils.features.breeding.config.BreedConfig;
import com.kingpixel.cobbleutils.features.breeding.models.PlotBreeding;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import com.kingpixel.cobbleutils.util.UIUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 02/08/2024 14:29
 */
public class PlotBreedingUI {
  public static void open(ServerPlayerEntity player) {
    try {
      int rows = CobbleUtils.breedconfig.getNeedRows();
      DatabaseClientFactory.CheckDaycarePlots(player);
      ChestTemplate template = ChestTemplate.builder(rows).build();


      int size = CobbleUtils.breedconfig.getPlotSlots().size();
      int max = CobbleUtils.breedconfig.getDefaultNumberPlots();

      for (int i = 0; i < size + 1; i++) {
        int n = i + 1;
        if (PermissionApi.hasPermission(player, "cobbleutils.breeding.plot." + (n), 2)) {
          if (max < n) {
            max = n;
          }
        }
      }


      if (max >= size) max = size;

      ItemModel info = CobbleUtils.breedconfig.getInfoItem();

      BreedConfig.SuccessItems successItems = CobbleUtils.breedconfig.getSuccessItems();

      List<String> infoLore = new ArrayList<>(info.getLore());
      infoLore.replaceAll(s ->
        s.replace("%ah%", String.format("%.2f%%", successItems.getPercentageTransmitAH()))
          .replace("%destinyknot%", String.format("%.2f%%", successItems.getPercentageDestinyKnot()))
          .replace("%everstone%", String.format("%.2f%%", successItems.getPercentageEverStone()))
          .replace("%poweritem%", String.format("%.2f%%", successItems.getPercentagePowerItem()))
          .replace("%eggmoves%", String.format("%.2f%%", CobbleUtils.breedconfig.getSuccessItems().getPercentageEggMoves()))
          .replace("%masuda%", CobbleUtils.breedconfig.isMethodmasuda() ? CobbleUtils.language.getYes() : CobbleUtils.language.getNo())
          .replace("%multipliermasuda%", String.valueOf(CobbleUtils.breedconfig.getMultipliermasuda()))
          .replace("%maxivs%", String.valueOf(CobbleUtils.breedconfig.getMaxIvsRandom()))
          .replace("%shinyrate%", String.valueOf(Cobblemon.INSTANCE.getConfig().getShinyRate()))
          .replace("%multipliershiny%", String.valueOf(CobbleUtils.breedconfig.getMultiplierShiny()))
          .replace("%cooldown%", PlayerUtils.getCooldown(CobbleUtils.breedconfig.getCooldown(player)))
      );

      if (UIUtils.isInside(info, rows)) {
        GooeyButton button = GooeyButton.builder()
          .display(info.getItemStack())
          .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(info.getDisplayname()))
          .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(infoLore)))
          .build();
        template.set(info.getSlot(), button);
      }

      List<PlotBreeding> plots = DatabaseClientFactory.databaseClient.getPlots(player);
      for (int i = 0; i < max; i++) {
        PlotBreeding plotBreeding = plots.get(i);
        List<String> lore = new ArrayList<>(CobbleUtils.breedconfig.getPlotItem().getLore());
        int amount = plotBreeding.getEggs().size();
        List<Pokemon> pokemons = new ArrayList<>();
        pokemons.add(plotBreeding.getMale() != null ? Pokemon.Companion.loadFromJSON(DynamicRegistryManager.EMPTY, plotBreeding.getMale()) : null);
        pokemons.add(plotBreeding.getFemale() != null ? Pokemon.Companion.loadFromJSON(DynamicRegistryManager.EMPTY, plotBreeding.getFemale()) : null);
        lore.replaceAll(s -> PokemonUtils.replace(s, pokemons)
          .replace("%cooldown%", PlayerUtils.getCooldown(new Date(plotBreeding.getCooldown())))
          .replace("%eggs%", String.valueOf(amount)));

        ItemStack itemStack;
        if (plotBreeding.getEggs().isEmpty()) {
          itemStack = CobbleUtils.breedconfig.getPlotItem().getItemStack();
        } else {
          itemStack = CobbleUtils.breedconfig.getPlotThereAreEggs().getItemStack(amount);
        }

        int finalI = i;
        GooeyButton button = GooeyButton.builder()
          .display(itemStack)
          .with(DataComponentTypes.CUSTOM_NAME,
            AdventureTranslator.toNative(CobbleUtils.breedconfig.getPlotItem().getDisplayname()))
          .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(lore)))
          .onClick(action -> {
            plotBreeding.checking(player);
            PlotBreedingManagerUI.open(player, plotBreeding, finalI);
          })
          .build();
        template.set(CobbleUtils.breedconfig.getPlotSlots().get(i), button);
      }

      GooeyPage page = GooeyPage.builder()
        .template(template)
        .title(AdventureTranslator.toNative(CobbleUtils.breedconfig.getTitleselectplot()))
        .build();

      UIManager.openUIForcefully(player, page);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
