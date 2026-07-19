package com.kingpixel.cobbleutils.config;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.*;
import com.kingpixel.cobbleutils.util.UtilsFile;
import com.kingpixel.cobbleutils.util.economys.v1.*;
import com.kingpixel.cobbleutils.util.redis.RedisConfig;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Data
@ToString
public class Config {
  private String server = "default";
  private boolean debug;
  private boolean adminServer = true;
  private boolean useDefault;
  private String lang;
  private String prefix;
  private List<String> commmandplugin;
  private List<String> storageCommand;
  private int decimals;
  private String priceFormat;
  private DurationValue timeSinceLastLoginToSuggest;
  private boolean GtsSupport;
  private EconomyUse GtsEconomyToUse;
  private boolean activeshinytoken;
  private String pokeshout;
  private String pokeshoutall;
  private DurationValue cooldownpokeshout;
  private boolean redisMessaging;
  private RedisConfig redis;
  private DataBaseConfig database;
  private String fill;
  private List<PriorityEconomy> priorityEconomy;
  private ItemModel shinytoken;
  private PokemonBlackList shinytokenBlacklist;
  private Map<String, ItemModel> itemsCommands;
  private Map<String, Double> rarity;


  public Config() {
    server = "default";
    debug = false;
    useDefault = false;
    prefix = "§7[§6CobbleUtils§7] ";
    lang = "en";
    timeSinceLastLoginToSuggest = DurationValue.parse("1y");
    decimals = 2;
    priceFormat = "#,##0.00";
    GtsSupport = false;
    GtsEconomyToUse = new EconomyUse(ImpactorEconomy.IDENTIFY, "");
    priorityEconomy = new ArrayList<>();
    priorityEconomy.add(new PriorityEconomy(UltraEEconomy.IDENTIFY, Priority.HIGHEST));
    priorityEconomy.add(new PriorityEconomy(ImpactorEconomy.IDENTIFY, Priority.HIGH));
    priorityEconomy.add(new PriorityEconomy(BeEconomy.IDENTIFY, Priority.HIGH));
    priorityEconomy.add(new PriorityEconomy(CobbleDollarsEconomy.IDENTIFY, Priority.MEDIUM));
    priorityEconomy.add(new PriorityEconomy(PebbleEconomy.IDENTIFY, Priority.LOW));
    priorityEconomy.add(new PriorityEconomy(VaultEconomy.IDENTIFY, Priority.LOW));
    priorityEconomy.add(new PriorityEconomy(SDMEconomy.IDENTIFY, Priority.LOWEST));
    fill = "minecraft:gray_stained_glass_pane";
    commmandplugin = List.of("cobbleutils", "pokeutils");
    storageCommand = List.of("storage", "s");
    activeshinytoken = true;
    pokeshout = "pokeshoutplus";
    pokeshoutall = "pokeshoutplusall";
    cooldownpokeshout = DurationValue.parse("60s");
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
    redisMessaging = false;
    redis = new RedisConfig();
  }

  public void init() {
    Path configPath = CobbleUtils.getPathMod().resolve("config.json");
    try {
      CobbleUtils.config = UtilsFile.readOrCreate(configPath, Config.class, Config::new);
      UtilsFile.write(configPath, CobbleUtils.config);
    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.error("Error loading config.json: " + e.getMessage());
      CobbleUtils.config = this;
      try {
        UtilsFile.write(configPath, CobbleUtils.config);
      } catch (Exception writeEx) {
        CobbleUtils.LOGGER_RAW.error("Error writing config.json: " + writeEx.getMessage());
      }
    }
  }


  // Transient para no serializarlo en el JSON
  private transient DecimalFormat lazyFormat;

  /**
   * Obtiene un DecimalFormat con lazy initialization.
   * Si aún no existe, se crea según el formato y decimales configurados.
   */
  private DecimalFormat getLazyFormat() {
    if (lazyFormat != null) return lazyFormat;
    synchronized (this) {
      if (lazyFormat == null) {
        String format = this.priceFormat != null ? this.priceFormat : "#.##";
        lazyFormat = new DecimalFormat(format);
        lazyFormat.setMaximumFractionDigits(this.decimals > 0 ? this.decimals : 2);
        lazyFormat.setMinimumFractionDigits(0);
        lazyFormat.setGroupingUsed(false);
      }
    }
    return lazyFormat;
  }

  /**
   * Formatea un BigDecimal con el formato configurado.
   */
  public String getFormat(BigDecimal amount) {
    if (amount == null) return "0";
    return getLazyFormat().format(amount);
  }
}