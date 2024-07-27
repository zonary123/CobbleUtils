package com.kingpixel.cobbleutils.config;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.Gson;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.*;
import com.kingpixel.cobbleutils.Model.options.Boss;
import com.kingpixel.cobbleutils.Model.options.ImpactorEconomy;
import com.kingpixel.cobbleutils.Model.options.Pokerus;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


/**
 * @author Carlos Varas Alonso - 29/04/2024 0:14
 */
@Getter
public class Config {
  private boolean debug;
  private String prefix;
  private String lang;
  private String fill;
  private String ecocommand;
  private List<String> commandrewards;
  private List<String> commandparty;
  private List<String> commmandplugin;
  private boolean randomsize;
  private boolean shulkers;
  private boolean fossil;
  private boolean shinyparticle;
  private boolean pickup;
  private boolean party;
  private boolean rewards;
  private boolean activeshinytoken;
  private boolean directreward;
  private boolean solveSizeRandom;
  private int alertreward;
  private float minpokemonsize;
  private float maxpokemonsize;
  private String pokeshout;
  private String pokeshoutall;
  private Pokerus pokerus;
  private Boss bosses;
  private ImpactorEconomy impactorEconomy;
  private List<SizeChance> pokemonsizes;
  private List<ScalePokemonData> specifiedSizes;
  private List<PokemonData> shinytokenBlacklist;
  private List<PokemonData> blacklist;
  private List<PokemonData> legends;
  private List<PokemonData> ultraBeasts;
  private List<String> forms;
  private ItemModel shinytoken;
  private Map<String, ItemModel> itemsCommands;
  private Map<String, Double> rarity;


  public Config() {
    debug = false;
    prefix = "§7[§6CobbleUtils§7] ";
    lang = "en";
    fill = "minecraft:gray_stained_glass_pane";
    ecocommand = "eco deposit %amount% dollars %player%";
    commandparty = List.of("party", "cuparty");
    commandrewards = List.of("rewards", "curewards");
    commmandplugin = List.of("cobbleutils", "pokeutils");
    shulkers = true;
    randomsize = true;
    fossil = true;
    solveSizeRandom = true;
    pickup = true;
    shinyparticle = true;
    party = true;
    rewards = true;
    directreward = true;
    activeshinytoken = true;
    minpokemonsize = 0.01f;
    maxpokemonsize = 10f;
    alertreward = 15;
    pokeshout = "cupokeshout";
    pokeshoutall = "cupokeshoutall";
    pokerus = new Pokerus();
    bosses = new Boss();
    impactorEconomy = new ImpactorEconomy();
    pokemonsizes = List.of(
      new SizeChance("Tiny", 0.5f, 5),
      new SizeChance("Small", 0.75f, 15),
      new SizeChance("Normal", 1.0f, 75),
      new SizeChance("Big", 1.25f, 15),
      new SizeChance("Giant", 1.5f, 5)
    );
    shinytoken = new ItemModel("minecraft:paper", "<gradient:#e0d234:#ede69a><bold>Shiny Token", List.of("§aShiny " +
      "Token"), 0);
    shinytokenBlacklist = List.of(new PokemonData("ditto", "normal"));
    blacklist = List.of(new PokemonData("ditto", "normal"));
    legends = List.of(new PokemonData("mewtwo", "normal"));
    forms = List.of("Normal", "Hisui", "Galar");
    itemsCommands = new HashMap<>();
    itemsCommands.put("give", new ItemModel("minecraft:chest", "<gradient:#e0d234:#ede69a><bold>Item", List.of(
      "§aThis give you a item")));
    rarity = new HashMap<>();
    rarity.put("common", 7.0);
    rarity.put("uncommon", 2.5);
    rarity.put("rare", 0.3);
    rarity.put("epic", 0.1);
    specifiedSizes = new ArrayList<>();
    specifiedSizes.add(new ScalePokemonData("ditto", "normal", SizeChanceWithoutItem.transform(pokemonsizes)));
    specifiedSizes.add(new ScalePokemonData("zorua", "hisui", SizeChanceWithoutItem.transform(pokemonsizes)));
  }

  public void init() {
    CompletableFuture<Boolean> futureRead = Utils.readFileAsync(CobbleUtils.PATH, "config.json",
      el -> {
        Gson gson = Utils.newGson();
        Config config = gson.fromJson(el, Config.class);
        debug = config.isDebug();
        prefix = config.getPrefix();
        lang = config.getLang();
        fill = config.getFill();
        shinytoken = config.getShinytoken();
        directreward = config.isDirectreward();
        randomsize = config.isRandomsize();
        shulkers = config.isShulkers();
        fossil = config.isFossil();
        pickup = config.isPickup();
        shinyparticle = config.isShinyparticle();
        minpokemonsize = config.getMinpokemonsize();
        maxpokemonsize = config.getMaxpokemonsize();
        pokemonsizes = config.getPokemonsizes();
        solveSizeRandom = config.isSolveSizeRandom();
        pokemonsizes.forEach(sizeChance -> {
          if (sizeChance.getItem() == null) {
            sizeChance.setItem(new ItemModel("minecraft:stone", "Stone", List.of()));
          }
        });
        pokeshout = config.getPokeshout();
        pokeshoutall = config.getPokeshoutall();
        pokerus = config.getPokerus();
        party = config.isParty();
        rewards = config.isRewards();
        ecocommand = config.getEcocommand();
        commandparty = config.getCommandparty();
        commandrewards = config.getCommandrewards();
        commmandplugin = config.getCommmandplugin();
        alertreward = config.getAlertreward();
        itemsCommands = config.getItemsCommands();
        bosses = config.getBosses();
        impactorEconomy = config.getImpactorEconomy();


        shinytokenBlacklist = config.getShinytokenBlacklist();
        blacklist = config.getBlacklist();
        legends = config.getLegends();
        ultraBeasts = config.getUltraBeasts();

        activeshinytoken = config.isActiveshinytoken();
        forms = config.getForms();
        rarity = config.getRarity();
        specifiedSizes = config.getSpecifiedSizes();

        String data = gson.toJson(this);
        CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(CobbleUtils.PATH, "config.json",
          data);
        if (!futureWrite.join()) {
          CobbleUtils.LOGGER.fatal("Could not write config.json file for " + CobbleUtils.MOD_NAME + ".");
        }
      });

    if (!futureRead.join()) {
      CobbleUtils.LOGGER.info("No config.json file found for" + CobbleUtils.MOD_NAME + ". Attempting to generate one.");
      Gson gson = Utils.newGson();
      String data = gson.toJson(this);
      CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(CobbleUtils.PATH, "config.json",
        data);

      if (!futureWrite.join()) {
        CobbleUtils.LOGGER.fatal("Could not write config.json file for " + CobbleUtils.MOD_NAME + ".");
      }
    }

  }

  /**
   * Método para obtener un tamaño de Pokémon basado en las probabilidades configuradas.
   *
   * @return El tamaño del Pokémon seleccionado según las probabilidades.
   */
  public SizeChance getRandomPokemonSize() {
    int totalWeight = pokemonsizes.stream().mapToInt(SizeChance::getChance).sum();
    int randomValue = Utils.RANDOM.nextInt(totalWeight) + 1;

    int currentWeight = 0;
    for (SizeChance sizeChance : pokemonsizes) {
      currentWeight += sizeChance.getChance();
      if (randomValue <= currentWeight) {
        return sizeChance;
      }
    }
    return new SizeChance();
  }

  public boolean isBlacklisted(Pokemon pokemon) {
    return blacklist.stream().anyMatch(pokemonData -> PokemonData.equals(pokemonData, PokemonData.from(pokemon)));
  }

  public boolean isShinyTokenBlacklisted(Pokemon pokemon) {
    return shinytokenBlacklist.stream().anyMatch(pokemonData -> PokemonData.equals(pokemonData, PokemonData.from(pokemon)));
  }

  public boolean isLegendary(Pokemon pokemon) {
    return legends.stream().anyMatch(pokemonData -> PokemonData.equals(pokemonData, PokemonData.from(pokemon)));
  }

  public boolean isUltraBeast(Pokemon pokemon) {
    return ultraBeasts.stream().anyMatch(pokemonData -> PokemonData.equals(pokemonData, PokemonData.from(pokemon)));
  }

  public boolean isForm(String form) {
    return forms.contains(form);
  }
}