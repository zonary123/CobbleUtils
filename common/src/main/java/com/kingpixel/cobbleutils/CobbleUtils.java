package com.kingpixel.cobbleutils;

import com.cobblemon.mod.common.api.properties.CustomPokemonProperty;
import com.kingpixel.cobbleutils.command.CommandTree;
import com.kingpixel.cobbleutils.config.Config;
import com.kingpixel.cobbleutils.config.Lang;
import com.kingpixel.cobbleutils.config.ShopConfig;
import com.kingpixel.cobbleutils.config.ShopLang;
import com.kingpixel.cobbleutils.database.DatabaseClientFactory;
import com.kingpixel.cobbleutils.events.ItemRightClickEvents;
import com.kingpixel.cobbleutils.features.Features;
import com.kingpixel.cobbleutils.features.breeding.config.BreedConfig;
import com.kingpixel.cobbleutils.features.shops.ShopTransactions;
import com.kingpixel.cobbleutils.managers.PartyManager;
import com.kingpixel.cobbleutils.party.command.CommandsParty;
import com.kingpixel.cobbleutils.party.config.PartyConfig;
import com.kingpixel.cobbleutils.party.config.PartyLang;
import com.kingpixel.cobbleutils.party.event.CreatePartyEvent;
import com.kingpixel.cobbleutils.party.event.DeletePartyEvent;
import com.kingpixel.cobbleutils.party.util.PartyPlaceholder;
import com.kingpixel.cobbleutils.properties.BreedablePropertyType;
import com.kingpixel.cobbleutils.properties.MinIvsPropertyType;
import com.kingpixel.cobbleutils.util.ShopExtend;
import com.kingpixel.cobbleutils.util.SpawnRates;
import com.kingpixel.cobbleutils.util.Utils;
import com.kingpixel.cobbleutils.util.UtilsLogger;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

import static com.kingpixel.cobbleutils.util.EconomyUtil.setEconomyType;

public class CobbleUtils extends ShopExtend {
  public static final String MOD_ID = "cobbleutils";
  public static final String PATH = "/config/cobbleutils";
  public static final String PATH_LANG = PATH + "/lang/";
  public static final String PATH_PARTY = PATH + "/party/";
  public static final String PATH_PARTY_LANG = PATH_PARTY + "lang/";
  public static final String PATH_PARTY_DATA = PATH_PARTY + "data/";
  public static final String PATH_BREED = PATH + "/breed/";
  public static final String PATH_BREED_DATA = PATH_BREED + "data/";
  public static final String PATH_SHOP = CobbleUtils.PATH + "/shop/";
  public static final String PATH_SHOPS = PATH_SHOP + "shops/";
  public static final UtilsLogger LOGGER = new UtilsLogger();
  public static final String MOD_NAME = "CobbleUtils";
  public static MinecraftServer server;
  public static Config config = new Config();
  public static BreedConfig breedconfig = new BreedConfig();
  public static ShopConfig shopConfig = new ShopConfig();
  public static SpawnRates spawnRates = new SpawnRates();
  // Lang
  public static Lang language = new Lang();
  public static ShopLang shopLang = new ShopLang();
  // Party
  public static PartyConfig partyConfig = new PartyConfig();
  public static PartyLang partyLang = new PartyLang();
  public static PartyManager partyManager = new PartyManager();
  public static List<String> modsInUse = new ArrayList<>();


  public static void init() {
    events();
    modsInUse.add(MOD_ID);
  }

  public static void load() {
    files(true);
    sign();
    tasks();
    Features.register();
  }


  private static void files(boolean shop) {
    config.init();
    language.init();
    shopLang.init();
    partyConfig.init();
    partyLang.init();
    breedconfig.init();
    if (config.isApiMode()) return;
    DatabaseClientFactory.createDatabaseClient(config.getDatabase());
    if (shop) {
      shopConfig.init(PATH_SHOP, MOD_ID, PATH_SHOPS);
    }
  }

  private static void sign() {
    info(MOD_NAME, "1.1.3", "CobbleUtils");
    LOGGER.info("§e| §6Shop: " + isActive(CobbleUtils.config.isShops()));
    LOGGER.info("§e| §6Party: " + isActive(CobbleUtils.config.isParty()));
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
    files(false);

    LifecycleEvent.SERVER_LEVEL_LOAD.register(level -> server = level.getServer());

    Utils.removeFiles(PATH_PARTY_DATA);

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
      CommandsParty.register(dispatcher, registry);
    });


    LifecycleEvent.SERVER_STOPPING.register(server -> {
      CreatePartyEvent.CREATE_PARTY_EVENT.clear();
      DeletePartyEvent.DELETE_PARTY_EVENT.clear();
    });


    PlayerEvent.PLAYER_JOIN.register(player -> {

    });


    PlayerEvent.PLAYER_QUIT.register(player -> {
      // leave party
      if (config.isParty() && partyManager.isPlayerInParty(player) && partyConfig.isTemporalParty()) {
        partyManager.leaveParty(player);
      }
      // Remove shop transactions data
      if (config.isShops()) {
        ShopTransactions.transactions.remove(player.getUuid());
      }
    });


    InteractionEvent.RIGHT_CLICK_ITEM.register(ItemRightClickEvents::register);

    PartyPlaceholder.register();
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
