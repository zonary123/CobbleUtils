package com.kingpixel.cobbleutils;

import com.cobblemon.mod.common.api.properties.CustomPokemonProperty;
import com.kingpixel.cobbleutils.Model.properties.LegendaryPropertyType;
import com.kingpixel.cobbleutils.Model.properties.MinIvsPropertyType;
import com.kingpixel.cobbleutils.api.EconomyApi;
import com.kingpixel.cobbleutils.command.CommandTree;
import com.kingpixel.cobbleutils.config.Config;
import com.kingpixel.cobbleutils.config.Lang;
import com.kingpixel.cobbleutils.events.ItemRightClickEvents;
import com.kingpixel.cobbleutils.util.RedisManager;
import com.kingpixel.cobbleutils.util.CobbleUtilsBridgeGTS;
import com.kingpixel.cobbleutils.util.SpawnRates;
import com.kingpixel.cobbleutils.util.UtilsLogger;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

public class CobbleUtils {
  public static final String MOD_ID = "cobbleutils";
  public static final String MOD_NAME = "CobbleUtils";
  public static final String PATH = "/config/cobbleutils";
  public static final String PATH_LANG = PATH + "/lang/";
  public static final String PATH_BREED = PATH + "/breed/";
  public static final String PATH_BREED_DATA = PATH_BREED + "data/";
  public static final UtilsLogger LOGGER = new UtilsLogger();
  public static CommandRegistryAccess commandRegistryAccess;
  public static MinecraftServer server;
  public static Config config = new Config();
  public static SpawnRates spawnRates = new SpawnRates();
  // Lang
  public static Lang language = new Lang();
  public static List<String> modsInUse = new ArrayList<>();


  public static void init() {
    events();
    modsInUse.add(MOD_ID);
  }

  public static void load() {
    files();
    sign();
    EconomyApi.setEconomyType();
    RedisManager.init();
    try {
      if (config.isGtsSupport()) {
        new CobbleUtilsBridgeGTS();
      }
    } catch (NoClassDefFoundError | NoSuchMethodError | Exception ignored) {
      LOGGER.error("Error while trying to get GtsEconomyProvider");
    }
  }


  private static void files() {
    config.init();
    language.init();
  }

  private static void sign() {
    info(MOD_NAME, "1.1.3", "CobbleUtils");
    LOGGER.info("§e| §6Supported economies: Impactor, BlanketEconomy, CobbleDollars, SDMEconomy, PebbleEconomy and Vault");
    LOGGER.info("§e+-------------------------------+");
  }

  public static void info(String mod, String version, String github) {
    LOGGER.info("§e+-------------------------------+");
    LOGGER.info("§e| §6" + mod);
    LOGGER.info("§e+-------------------------------+");
    LOGGER.info("§e| §6Version: §e" + version);
    LOGGER.info("§e| §6Author: §eZonary123");
    LOGGER.info("§e| §6Website: §9https://github.com/Zonary123/" + github);
    LOGGER.info("§e| §6Discord: §9https://discord.com/invite/fKNc7FnXpa");
    LOGGER.info("§e| §6Support: §9https://github.com/Zonary123/" + github + "/issues");
    LOGGER.info("§e| &dDonate: §9https://ko-fi.com/zonary123");
    LOGGER.info("§e+-------------------------------+");
  }


  private static void events() {
    files();

    LifecycleEvent.SERVER_LEVEL_LOAD.register(level -> {
      server = level.getServer();
    });

    LifecycleEvent.SERVER_STARTED.register(server -> {
      spawnRates.init();

      load();
    });

    LifecycleEvent.SERVER_STOPPED.register(server1 -> {
      //Utils.shutdownExecutor();
      RedisManager.close();
    });

    CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> {
      CustomPokemonProperty.Companion.register(new MinIvsPropertyType());
      CustomPokemonProperty.Companion.register(new LegendaryPropertyType());
      CommandTree.register(dispatcher, registry);
      commandRegistryAccess = registry;
    });

    InteractionEvent.RIGHT_CLICK_ITEM.register(ItemRightClickEvents::register);
  }

}
