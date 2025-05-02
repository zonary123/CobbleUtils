package com.kingpixel.cobbleutils.config;

import com.google.gson.Gson;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.*;
import com.kingpixel.cobbleutils.util.Utils;
import com.kingpixel.cobbleutils.util.economys.*;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
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
  private String prefix;
  private String lang;
  private DataBaseConfig database;
  private boolean GtsSupport;
  private EconomyUse GtsEconomyToUse;
  private List<PriorityEconomy> priorityEconomy;
  private boolean activeshinytoken;
  private String pokeshout;
  private String pokeshoutall;
  private int cooldownpokeshout;
  private String fill;
  private List<String> commmandplugin;
  private ItemModel shinytoken;
  private PokemonBlackList shinytokenBlacklist;
  private Map<String, ItemModel> itemsCommands;
  private Map<String, Double> rarity;

  public Config() {
    debug = false;
    prefix = "§7[§6CobbleUtils§7] ";
    lang = "en";
    GtsSupport = false;
    GtsEconomyToUse = new EconomyUse(ImpactorEconomy.IDENTIFY, "");
    priorityEconomy = new ArrayList<>();
    priorityEconomy.add(new PriorityEconomy(ImpactorEconomy.IDENTIFY, Priority.HIGHEST));
    priorityEconomy.add(new PriorityEconomy(BeEconomy.IDENTIFY, Priority.HIGH));
    priorityEconomy.add(new PriorityEconomy(CobbleDollarsEconomy.IDENTIFY, Priority.MEDIUM));
    priorityEconomy.add(new PriorityEconomy(PebbleEconomy.IDENTIFY, Priority.LOW));
    priorityEconomy.add(new PriorityEconomy(VaultEconomy.IDENTIFY, Priority.LOW));
    priorityEconomy.add(new PriorityEconomy(SDMEconomy.IDENTIFY, Priority.LOWEST));
    fill = "minecraft:gray_stained_glass_pane";
    commmandplugin = List.of("cobbleutils", "pokeutils");
    activeshinytoken = true;
    pokeshout = "pokeshoutplus";
    pokeshoutall = "pokeshoutplusall";
    cooldownpokeshout = 60;
    shinytoken = new ItemModel("minecraft:paper", "<gradient:#e0d234:#ede69a><bold>Shiny Token", List.of("§aShiny " +
      "Token"), 0);
    shinytokenBlacklist = new PokemonBlackList();
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
        CobbleUtils.config = gson.fromJson(el, Config.class);
        String data = gson.toJson(CobbleUtils.config);
        Utils.writeFileAsync(CobbleUtils.PATH, "config.json", data);
      });

    if (!futureRead.join()) {
      CobbleUtils.LOGGER.info("No config.json file found for" + CobbleUtils.MOD_NAME + ". Attempting to generate one.");
      Gson gson = Utils.newGson();
      CobbleUtils.config = this;
      String data = gson.toJson(CobbleUtils.config);
      Utils.writeFileAsync(CobbleUtils.PATH, "config.json",
        data);
    }

  }

}