package com.kingpixel.cobbleutils.features.breeding.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.ButtonAction;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.database.DatabaseClientFactory;
import com.kingpixel.cobbleutils.features.breeding.models.PlotBreeding;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import com.kingpixel.cobbleutils.util.RewardsUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author Carlos Varas Alonso - 02/08/2024 14:29
 */
public class PlotBreedingManagerUI {
  public static void open(ServerPlayerEntity player, PlotBreeding plotBreeding, int finalI) {
    int row = CobbleUtils.breedconfig.getRowmenuplot();
    ChestTemplate template = ChestTemplate.builder(row).build();
    List<PlotBreeding> plots = DatabaseClientFactory.databaseClient.getPlots(player);
    Pokemon pokemonmale = plotBreeding.obtainMale();
    Pokemon pokemonfemale = plotBreeding.obtainFemale();

    GooeyButton male = createButton(pokemonmale, action -> {
      if (pokemonmale == null) {
        PlotSelectPokemonUI.selectPokemon(player, plotBreeding, Gender.MALE, finalI);
        return;
      }
      Cobblemon.INSTANCE.getStorage().getParty(player).add(pokemonmale);
      plotBreeding.setMale(null);
      plots.set(finalI, plotBreeding);
      DatabaseClientFactory.databaseClient.savePlots(player, plots);
      open(player, plotBreeding, finalI);
    }, Gender.MALE);

    CobbleUtils.breedconfig.getMaleSlots().forEach(slot -> {
      template.set(slot, createEmptyButton(pokemonmale, action -> {
        if (pokemonmale == null) {
          PlotSelectPokemonUI.selectPokemon(player, plotBreeding, Gender.MALE, finalI);
          return;
        }
        Cobblemon.INSTANCE.getStorage().getParty(player).add(pokemonmale);
        plotBreeding.setMale(null);
        plots.set(finalI, plotBreeding);
        DatabaseClientFactory.databaseClient.savePlots(player, plots);
        open(player, plotBreeding, finalI);
      }, Gender.MALE));
    });

    // Female
    GooeyButton female = createButton(pokemonfemale, action -> {
      if (pokemonfemale == null) {
        PlotSelectPokemonUI.selectPokemon(player, plotBreeding, Gender.FEMALE, finalI);
        return;
      }
      Cobblemon.INSTANCE.getStorage().getParty(player).add(pokemonfemale);
      plotBreeding.setFemale(null);
      plots.set(finalI, plotBreeding);
      DatabaseClientFactory.databaseClient.savePlots(player, plots);
      open(player, plotBreeding, finalI);
    }, Gender.FEMALE);

    CobbleUtils.breedconfig.getFemaleSlots().forEach(slot -> {
      template.set(slot, createEmptyButton(pokemonfemale, action -> {
        if (pokemonfemale == null) {
          PlotSelectPokemonUI.selectPokemon(player, plotBreeding, Gender.FEMALE, finalI);
          return;
        }
        Cobblemon.INSTANCE.getStorage().getParty(player).add(pokemonfemale);
        plotBreeding.setFemale(null);
        plots.set(finalI, plotBreeding);
        DatabaseClientFactory.databaseClient.savePlots(player, plots);
        open(player, plotBreeding, finalI);
      }, Gender.FEMALE));
    });

    Pokemon pokemonegg = null;

    if (!plotBreeding.getEggs().isEmpty()) {
      pokemonegg = Pokemon.Companion.loadFromJSON(DynamicRegistryManager.EMPTY, plotBreeding.getEggs().get(0));
    } else {
      pokemonegg = PokemonProperties.Companion.parse("egg").create();
    }

    GooeyButton egg = GooeyButton.builder()
      .display(PokemonItem.from(pokemonegg,
        plotBreeding.getEggs().size()))
      .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(plotBreeding.getEggs().isEmpty() ? "" :
        PokemonUtils.getTranslatedName(plotBreeding.getFirstEgg())))
      .onClick(action -> {
        if (!plotBreeding.getEggs().isEmpty()) {
          plotBreeding.getEggs().forEach(pokemon -> {

            RewardsUtils.saveRewardPokemon(action.getPlayer(),
              Pokemon.Companion.loadFromJSON(DynamicRegistryManager.EMPTY, pokemon));
          });
          plotBreeding.getEggs().clear();
          plots.set(finalI, plotBreeding);
          DatabaseClientFactory.databaseClient.savePlots(player, plots);
          open(player, plotBreeding, finalI);
        }
      })
      .build();

    CobbleUtils.breedconfig.getEggSlots().forEach(slot -> {
      template.set(slot, GooeyButton.builder()
        .display(CobbleUtils.breedconfig.getEmptySlots().getItemStack())
        .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(
          plotBreeding.getEggs().isEmpty() ? "" :
            PokemonUtils.getTranslatedName(plotBreeding.getFirstEgg())
        ))
        .onClick(action -> {
          if (!plotBreeding.getEggs().isEmpty()) {
            plotBreeding.getEggs().forEach(pokemon -> {

              RewardsUtils.saveRewardPokemon(action.getPlayer(), Pokemon.Companion.loadFromJSON(DynamicRegistryManager.EMPTY, pokemon));
            });
            plotBreeding.getEggs().clear();
            plots.set(finalI, plotBreeding);
            DatabaseClientFactory.databaseClient.savePlots(player, plots);
            open(player, plotBreeding, finalI);
          }
        })
        .build());
    });

    ItemModel itemClose = CobbleUtils.breedconfig.getCloseItem();
    if (itemClose == null) {
      itemClose = CobbleUtils.language.getItemClose();
      itemClose.setSlot((row * 9) - 5);
      CobbleUtils.breedconfig.setCloseItem(itemClose);
      CobbleUtils.breedconfig.write();
    }


    template.set(CobbleUtils.breedconfig.getMaleSelectItem().getSlot(), male);
    template.set(13, egg);
    template.set(CobbleUtils.breedconfig.getFemaleSelectItem().getSlot(), female);
    template.set(itemClose.getSlot(), itemClose.getButton(action -> PlotBreedingUI.open(player)));

    String title = "";
    if (!plotBreeding.getEggs().isEmpty()) {
      title = CobbleUtils.breedconfig.getTitleplot();
    } else {
      title = CobbleUtils.breedconfig.getTitleemptyplot();
    }

    GooeyPage page = GooeyPage.builder()
      .template(template)
      .title(AdventureTranslator.toNative(title))
      .build();

    UIManager.openUIForcefully(player, page);
  }

  private static GooeyButton createButton(Pokemon pokemon, Consumer<ButtonAction> action, Gender gender) {
    ItemModel itemModel = new ItemModel();
    if (gender == Gender.MALE) {
      itemModel = CobbleUtils.breedconfig.getMaleSelectItem();
    } else if (gender == Gender.FEMALE) {
      itemModel = CobbleUtils.breedconfig.getFemaleSelectItem();
    }
    return GooeyButton.builder()
      .display((pokemon != null ? PokemonItem.from(pokemon) : itemModel.getItemStack()))
      .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator
        .toNative((pokemon != null ? PokemonUtils.replace(pokemon) :
          CobbleUtils.language.getGender().getOrDefault(gender.getShowdownName(), gender.name()))))
      .with(DataComponentTypes.LORE,
        new LoreComponent(AdventureTranslator.toNativeL((pokemon != null ? PokemonUtils.replaceLore(pokemon) : itemModel.getLore()))))
      .onClick(action)
      .build();
  }

  private static GooeyButton createEmptyButton(Pokemon pokemon, Consumer<ButtonAction> action, Gender gender) {
    ItemModel supportItemModel = new ItemModel();
    ItemModel emptyItemModel = CobbleUtils.breedconfig.getEmptySlots();
    if (gender == Gender.MALE) {
      supportItemModel = CobbleUtils.breedconfig.getMaleSelectItem();
    } else if (gender == Gender.FEMALE) {
      supportItemModel = CobbleUtils.breedconfig.getFemaleSelectItem();
    }
    return GooeyButton.builder()
      .display(emptyItemModel.getItemStack())
      .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator
        .toNative((pokemon != null ? PokemonUtils.replace(pokemon) :
          CobbleUtils.language.getGender().getOrDefault(gender.getShowdownName(), gender.name()))))
      .with(DataComponentTypes.LORE,
        new LoreComponent(AdventureTranslator.toNativeL((pokemon != null ? PokemonUtils.replaceLore(pokemon) : supportItemModel.getLore()))))
      .onClick(action)
      .build();
  }
}
