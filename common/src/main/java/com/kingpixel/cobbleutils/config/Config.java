package com.kingpixel.cobbleutils.config;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.Gson;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.Model.PokemonData;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 29/04/2024 0:14
 */
@Getter
@Data
@ToString
public class Config {
  private boolean debug;
  private boolean ApiMode;
  private String prefix;
  private String lang;
  private DataBaseConfig database;
  private boolean activeshinytoken;
  private String pokeshout;
  private String pokeshoutall;
  private int cooldownpokeshout;
  private String fill;
  private List<String> commmandplugin;
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
    commmandplugin = List.of("cobbleutils", "pokeutils");
    ApiMode = false;
    activeshinytoken = true;
    pokeshout = "pokeshoutplus";
    pokeshoutall = "pokeshoutplusall";
    cooldownpokeshout = 60;
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
    database = new DataBaseConfig();
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
        database = config.getDatabase();
        pokeshout = config.getPokeshout();
        pokeshoutall = config.getPokeshoutall();
        commmandplugin = config.getCommmandplugin();
        itemsCommands = config.getItemsCommands();
        cooldownpokeshout = config.getCooldownpokeshout();
        shinytokenBlacklist = config.getShinytokenBlacklist();
        blacklist = config.getBlacklist();
        legends = config.getLegends();
        ultraBeasts = config.getUltraBeasts();
        activeshinytoken = config.isActiveshinytoken();
        forms = config.getForms();
        rarity = config.getRarity();
        ApiMode = config.isApiMode();
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

  public boolean isShinyTokenBlacklisted(Pokemon pokemon) {
    return shinytokenBlacklist.stream()
      .anyMatch(pokemonData -> PokemonData.equals(pokemonData, PokemonData.from(pokemon)));
  }

}