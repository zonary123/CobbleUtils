package com.kingpixel.cobbleutils;

import com.cobblemon.mod.common.api.properties.CustomPokemonProperty;
import com.kingpixel.cobbleutils.command.CommandTree;
import com.kingpixel.cobbleutils.config.Config;
import com.kingpixel.cobbleutils.config.Lang;
import com.kingpixel.cobbleutils.database.DatabaseClientFactory;
import com.kingpixel.cobbleutils.events.ItemRightClickEvents;
import com.kingpixel.cobbleutils.features.Features;
import com.kingpixel.cobbleutils.features.breeding.config.BreedConfig;
import com.kingpixel.cobbleutils.properties.BreedablePropertyType;
import com.kingpixel.cobbleutils.properties.MinIvsPropertyType;
import com.kingpixel.cobbleutils.util.SpawnRates;
import com.kingpixel.cobbleutils.util.UtilsLogger;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

import static com.kingpixel.cobbleutils.util.EconomyUtil.setEconomyType;

public class CobbleUtils {
  public static final String MOD_ID = "cobbleutils";
  public static final String MOD_NAME = "CobbleUtils";
  public static final String PATH = "/config/cobbleutils";
  public static final String PATH_LANG = PATH + "/lang/";
  public static final String PATH_BREED = PATH + "/breed/";
  public static final String PATH_BREED_DATA = PATH_BREED + "data/";
  public static final UtilsLogger LOGGER = new UtilsLogger();
  public static MinecraftServer server;
  public static Config config = new Config();
  public static BreedConfig breedconfig = new BreedConfig();
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
    tasks();
    Features.register();
  }


  private static void files() {
    config.init();
    language.init();
    breedconfig.init();
    if (config.isApiMode()) return;
    DatabaseClientFactory.createDatabaseClient(config.getDatabase());
  }

  private static void sign() {
    info(MOD_NAME, "1.1.3", "CobbleUtils");
    LOGGER.info("§e| §6Breeding: " + isActive(CobbleUtils.breedconfig.isActive()));
    LOGGER.info("§e| §6Supported economies: Impactor, BlanketEconomy, CobbleDollars, PebbleEconomy and Vault");
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

    LifecycleEvent.SERVER_LEVEL_LOAD.register(level -> server = level.getServer());

    LifecycleEvent.SERVER_STARTED.register(server -> {
      load();
      if (config.isApiMode()) return;
      spawnRates.init();
      CustomPokemonProperty.Companion.register(MinIvsPropertyType.getInstance());
      if (CobbleUtils.breedconfig.isActive())
        CustomPokemonProperty.Companion.register(BreedablePropertyType.getInstance());
    });

    if (config.isApiMode()) return;
    CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> {
      CommandTree.register(dispatcher, registry);
    });

    InteractionEvent.RIGHT_CLICK_ITEM.register(ItemRightClickEvents::register);
  }

  private static void tasks() {


    setEconomyType();
  }


  private static String isActive(boolean active) {
    if (active) {
      return "§aActive";
    } else {
      return "§cInactive";
    }
  }
}
