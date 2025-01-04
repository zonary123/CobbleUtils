package com.kingpixel.cobbleutils.features.breeding.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.PlaceholderButton;
import ca.landonjw.gooeylibs2.api.button.linked.LinkType;
import ca.landonjw.gooeylibs2.api.button.linked.LinkedPageButton;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.database.DatabaseClientFactory;
import com.kingpixel.cobbleutils.features.breeding.models.EggData;
import com.kingpixel.cobbleutils.features.breeding.models.PlotBreeding;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import com.kingpixel.cobbleutils.util.Utils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 02/08/2024
 */
public class PlotSelectPokemonUI {

  public static void selectPokemon(ServerPlayerEntity player, PlotBreeding plotBreeding, Gender gender, int finalI) {
    int row = CobbleUtils.breedconfig.getRowmenuselectpokemon();
    ChestTemplate template = ChestTemplate.builder(row).build();

    List<Pokemon> pokemons = getPlayerPokemons(player, gender, plotBreeding);
    List<Button> buttons = createPokemonButtons(pokemons, player, plotBreeding, gender, finalI);

    configureTemplate(template, buttons, row, player, plotBreeding, finalI);

    LinkedPage page = PaginationHelper.createPagesFromPlaceholders(
      template, buttons, LinkedPage.builder()
        .title(AdventureTranslator.toNative(CobbleUtils.breedconfig.getSelectMenu().getTitle()))
    );
    UIManager.openUIForcefully(player, page);
  }

  private static List<Pokemon> getPlayerPokemons(ServerPlayerEntity player, Gender gender, PlotBreeding plotBreeding) {
    List<Pokemon> pokemons = new ArrayList<>();
    Cobblemon.INSTANCE.getStorage().getParty(player)
      .forEach(pokemon -> addIfAcceptable(pokemons, pokemon, player, gender, plotBreeding));
    Cobblemon.INSTANCE.getStorage().getPC(player)
      .forEach(pokemon -> addIfAcceptable(pokemons, pokemon, player, gender, plotBreeding));
    return pokemons;
  }

  private static void addIfAcceptable(List<Pokemon> pokemons, Pokemon pokemon, ServerPlayerEntity player,
                                      Gender gender, PlotBreeding plotBreeding) {
    if (pokemon.getForm().getEggGroups().contains(EggGroup.UNDISCOVERED)) return;
    if (CobbleUtils.breedconfig.getBlacklist().contains(pokemon.getSpecies().showdownId())) return;
    if (isAcceptablePokemon(pokemon, gender, plotBreeding, player, false)) {
      pokemons.add(pokemon);
    }
  }

  private static List<Button> createPokemonButtons(List<Pokemon> pokemons, ServerPlayerEntity player,
                                                   PlotBreeding plotBreeding, Gender gender, int finalI) {
    List<Button> buttons = new ArrayList<>();
    for (Pokemon pokemon : pokemons) {
      buttons.add(createPokemonButton(pokemon, player, plotBreeding, gender, finalI));
    }
    return buttons;
  }

  private static GooeyButton createPokemonButton(Pokemon pokemon, ServerPlayerEntity player,
                                                 PlotBreeding plotBreeding, Gender gender, int finalI) {
    return GooeyButton.builder()
      .display(PokemonItem.from(pokemon))
      .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(PokemonUtils.replace(pokemon)))
      .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(PokemonUtils.replaceLore(pokemon))))
      .onClick(action -> handlePokemonSelection(pokemon, player, plotBreeding, gender, finalI))
      .build();
  }

  private static void handlePokemonSelection(Pokemon pokemon, ServerPlayerEntity player,
                                             PlotBreeding plotBreeding, Gender gender, int finalI) {
    boolean pc, party;
    pc = Cobblemon.INSTANCE.getStorage().getPC(player).remove(pokemon);
    party = Cobblemon.INSTANCE.getStorage().getParty(player).remove(pokemon);
    if (CobbleUtils.config.isDebug()) {
      CobbleUtils.LOGGER.info("Pokemon removed from storage");
      CobbleUtils.LOGGER.info("Pokemon: " + pokemon.getSpecies().showdownId());
      CobbleUtils.LOGGER.info("PC: " + pc);
      CobbleUtils.LOGGER.info("Party: " + party);
    }
    plotBreeding.add(pokemon, gender);
    List<PlotBreeding> plots = DatabaseClientFactory.databaseClient.getPlots(player);
    plots.set(finalI, plotBreeding);
    DatabaseClientFactory.databaseClient.savePlots(player, plots);
    PlotBreedingManagerUI.open(player, plotBreeding, finalI);
  }

  private static void configureTemplate(ChestTemplate template, List<Button> buttons, int row,
                                        ServerPlayerEntity player, PlotBreeding plotBreeding, int finalI) {
    LinkedPageButton previous = LinkedPageButton.builder()
      .display(CobbleUtils.breedconfig.getSelectMenu().getPrevious().getItemStack())
      .linkType(LinkType.Previous)
      .build();
    template.set(row - 1, 0, previous);

    template.set(row - 1, 4, CobbleUtils.breedconfig.getSelectMenu().getClose().getButton(action -> {
      PlotBreedingManagerUI.open(player, plotBreeding, finalI);
    }));

    LinkedPageButton next = LinkedPageButton.builder()
      .display(CobbleUtils.breedconfig.getSelectMenu().getNext().getItemStack())
      .linkType(LinkType.Next)
      .build();
    template.set(row - 1, 8, next);

    template.fill(GooeyButton.builder().display(Utils.parseItemId(CobbleUtils.config.getFill()))
      .with(DataComponentTypes.CUSTOM_NAME, Text.empty())
      .build());
    template.rectangle(0, 0, row - 1, 9, new PlaceholderButton());
    template.fillFromList(buttons);
  }

  public static boolean isAcceptablePokemon(Pokemon pokemon, Gender gender, PlotBreeding plotBreeding,
                                            ServerPlayerEntity player, boolean notify) {
    if (!isPokemonBreedable(pokemon, player, notify)) return false;
    return checkCompatibility(pokemon, gender, plotBreeding, player, notify);
  }

  private static boolean isPokemonBreedable(Pokemon pokemon, ServerPlayerEntity player, boolean notify) {
    boolean isNotBreedable = pokemon.getForm().getEggGroups().contains(EggGroup.UNDISCOVERED)
      || pokemon.getSpecies().showdownId().equalsIgnoreCase("egg")
      || (pokemon.getSpecies().showdownId().equalsIgnoreCase("ditto") && !CobbleUtils.breedconfig.isDitto())
      || !PokemonUtils.isBreedable(pokemon)
      || CobbleUtils.breedconfig.getBlacklist().contains(pokemon.showdownId())
      || CobbleUtils.breedconfig.getBlacklistForm().contains(pokemon.getForm().formOnlyShowdownId());

    if (isNotBreedable && notify) {
      sendMessageIfNeeded(player, notify, CobbleUtils.breedconfig.getNotbreedable(), pokemon);
    }

    return !isNotBreedable;
  }

  private static boolean checkCompatibility(Pokemon pokemon, Gender gender, PlotBreeding plotBreeding,
                                            ServerPlayerEntity player, boolean notify) {
    Pokemon otherGender = plotBreeding.obtainOtherGender(gender);
    boolean isInWhitelist = CobbleUtils.breedconfig.getWhitelist().contains(pokemon.getSpecies().showdownId());
    boolean isLegendaryOrUltraBeast = pokemon.isLegendary() || pokemon.isUltraBeast();

    if (isInWhitelist && otherGender != null) {
      return otherGender.showdownId().equals("ditto");
    }

    if (isLegendaryOrUltraBeast && !isInWhitelist) {
      sendMessageIfNeeded(player, notify, CobbleUtils.breedconfig.getBlacklisted(), pokemon);
      return false;
    }


    return otherGender == null
      ? isInWhitelist || isGenderMatching(pokemon, gender) || isDittoBreedingAllowed(pokemon)
      : checkEggCompatibility(pokemon, gender, otherGender, isInWhitelist, notify, player);
  }

  private static boolean isGenderMatching(Pokemon pokemon, Gender gender) {
    return pokemon.getGender() == gender || pokemon.getGender() == Gender.GENDERLESS;
  }

  private static boolean isDittoBreedingAllowed(Pokemon pokemon) {
    return CobbleUtils.breedconfig.isDitto() && pokemon.getSpecies().showdownId().equalsIgnoreCase("ditto");
  }

  private static boolean checkEggCompatibility(Pokemon pokemon, Gender gender,
                                               Pokemon otherGender, boolean isInWhitelist,
                                               boolean notify, ServerPlayerEntity player) {
    boolean isOtherDitto = otherGender.getSpecies().showdownId().equalsIgnoreCase("ditto");
    boolean areCompatible = EggData.isCompatible(otherGender, pokemon);

    if (otherGender.getGender() == Gender.GENDERLESS && !isOtherDitto) return false;

    if (!isDittoCompatibility(pokemon, isOtherDitto, areCompatible, notify, player)) return false;

    return (areCompatible && isGenderMatching(pokemon, gender))
      || (isInWhitelist && isGenderMatching(pokemon, gender));
  }

  private static boolean isDittoCompatibility(Pokemon pokemon, boolean isOtherDitto, boolean areCompatible,
                                              boolean notify, ServerPlayerEntity player) {
    if (pokemon.getSpecies().showdownId().equalsIgnoreCase("ditto")) {
      if (isOtherDitto && !CobbleUtils.breedconfig.isDoubleditto()) {
        return false;
      } else return areCompatible || CobbleUtils.breedconfig.isDitto();
    }
    return true;
  }

  public static boolean arePokemonsCompatible(Pokemon malePokemon, Pokemon femalePokemon, ServerPlayerEntity player, boolean notify) {
    return isPokemonBreedable(malePokemon, player, notify)
      && isPokemonBreedable(femalePokemon, player, notify)
      && checkDittoBreedingCompatibility(malePokemon, femalePokemon,
      EggData.isCompatible(malePokemon, femalePokemon), notify, player);
  }

  private static boolean checkDittoBreedingCompatibility(Pokemon malePokemon, Pokemon femalePokemon, boolean areCompatible,
                                                         boolean notify, ServerPlayerEntity player) {
    boolean isMaleDitto = malePokemon.getSpecies().showdownId().equalsIgnoreCase("ditto");
    boolean isFemaleDitto = femalePokemon.getSpecies().showdownId().equalsIgnoreCase("ditto");
    boolean isDoubledittoEnabled = CobbleUtils.breedconfig.isDoubleditto();

    if (isMaleDitto && isFemaleDitto) {
      return isDoubledittoEnabled;
    } else if (isMaleDitto || isFemaleDitto) {
      return areCompatible || CobbleUtils.breedconfig.isDitto();
    } else {
      return areCompatible;
    }
  }

  private static void sendMessageIfNeeded(ServerPlayerEntity player, boolean notify, String message, Pokemon pokemon) {
    if (notify) {
      player.sendMessage(AdventureTranslator.toNative(PokemonUtils.replace(message, pokemon)));
    }
  }
}
