package com.kingpixel.cobbleutils;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.properties.CustomPokemonProperty;
import com.kingpixel.cobbleutils.Model.RewardsData;
import com.kingpixel.cobbleutils.command.CommandTree;
import com.kingpixel.cobbleutils.config.*;
import com.kingpixel.cobbleutils.database.DatabaseClientFactory;
import com.kingpixel.cobbleutils.events.ItemRightClickEvents;
import com.kingpixel.cobbleutils.events.ScaleEvent;
import com.kingpixel.cobbleutils.events.features.FeaturesRegister;
import com.kingpixel.cobbleutils.features.Features;
import com.kingpixel.cobbleutils.features.breeding.config.BreedConfig;
import com.kingpixel.cobbleutils.features.shops.ShopTransactions;
import com.kingpixel.cobbleutils.managers.PartyManager;
import com.kingpixel.cobbleutils.managers.RewardsManager;
import com.kingpixel.cobbleutils.party.command.CommandsParty;
import com.kingpixel.cobbleutils.party.config.PartyConfig;
import com.kingpixel.cobbleutils.party.config.PartyLang;
import com.kingpixel.cobbleutils.party.event.CreatePartyEvent;
import com.kingpixel.cobbleutils.party.event.DeletePartyEvent;
import com.kingpixel.cobbleutils.party.util.PartyPlaceholder;
import com.kingpixel.cobbleutils.properties.*;
import com.kingpixel.cobbleutils.util.*;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static com.kingpixel.cobbleutils.util.EconomyUtil.setEconomyType;

public class CobbleUtils extends ShopExtend {
  public static final String MOD_ID = "cobbleutils";
  public static final String PATH = "/config/cobbleutils";
  public static final String PATH_LANG = PATH + "/lang/";
  public static final String PATH_RANDOM = PATH + "/random/";
  public static final String PATH_PARTY = PATH + "/party/";
  public static final String PATH_PARTY_LANG = PATH_PARTY + "lang/";
  public static final String PATH_PARTY_DATA = PATH_PARTY + "data/";
  public static final String PATH_REWARDS_DATA = PATH + "/rewards/";
  public static final String PATH_BREED = PATH + "/breed/";
  public static final String PATH_BREED_DATA = PATH_BREED + "data/";
  public static final String PATH_SHOP = CobbleUtils.PATH + "/shop/";
  public static final String PATH_SHOPS = PATH_SHOP + "shops/";
  public static final String PATH_BOSS = PATH + "/boss/";
  public static final UtilsLogger LOGGER = new UtilsLogger();
  public static final String MOD_NAME = "CobbleUtils";
  public static MinecraftServer server;
  public static Config config = new Config();
  public static BreedConfig breedconfig = new BreedConfig();
  public static ShopConfig shopConfig = new ShopConfig();
  public static PoolMoney poolMoney = new PoolMoney();
  public static PoolItems poolItems = new PoolItems();
  public static PoolPokemons poolPokemons = new PoolPokemons();
  public static SpawnRates spawnRates = new SpawnRates();
  // Lang
  public static Lang language = new Lang();
  public static ShopLang shopLang = new ShopLang();
  // Party
  public static PartyConfig partyConfig = new PartyConfig();
  public static PartyLang partyLang = new PartyLang();
  public static PartyManager partyManager = new PartyManager();
  public static List<String> modsInUse = new ArrayList<>();
  // Rewards
  public static RewardsManager rewardsManager = new RewardsManager();
  // Tasks
  private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private static final List<ScheduledFuture<?>> scheduledTasks = new CopyOnWriteArrayList<>();


  public static void init() {
    events();
    modsInUse.add(MOD_ID);
  }

  public static void load() {
    checks();
    files();
    spawnRates.init();
    ArraysPokemons.init();
    sign();
    tasks();
    Features.register();
  }

  private static void checks() {
    Utils.createDirectoryIfNeeded(PATH);
    Utils.createDirectoryIfNeeded(PATH_LANG);
    Utils.createDirectoryIfNeeded(PATH_RANDOM);
    Utils.createDirectoryIfNeeded(PATH_PARTY);
    Utils.createDirectoryIfNeeded(PATH_PARTY_LANG);
    Utils.createDirectoryIfNeeded(PATH_PARTY_DATA);
    Utils.createDirectoryIfNeeded(PATH_REWARDS_DATA);
    Utils.createDirectoryIfNeeded(PATH_BREED);
    Utils.createDirectoryIfNeeded(PATH_BREED_DATA);
  }


  private static void files() {
    config.init();
    language.init();
    shopLang.init();
    breedconfig.init();
    poolItems.init();
    poolPokemons.init();
    poolMoney.init();
    partyConfig.init();
    partyLang.init();
    BossConfig.init();
    shopConfig.init(PATH_SHOP, MOD_ID, PATH_SHOPS);
    DatabaseClientFactory.createDatabaseClient(config.getDatabase());
  }

  private static void sign() {
    info(MOD_NAME, "1.1.3", "CobbleUtils");
    LOGGER.info("§e| §6Pokemons size: " + isActive(CobbleUtils.config.isRandomsize()));
    LOGGER.info("§e| §6Fossil: " + isActive(CobbleUtils.config.isFossil()));
    LOGGER.info("§e| §6Random item: §aImplemented");
    LOGGER.info("§e| §6Random money: §aImplemented");
    LOGGER.info("§e| §6Random pokemon: §aImplemented");
    LOGGER.info("§e| §6Shop: " + isActive(CobbleUtils.config.isShops()));
    LOGGER.info("§e| §6Party: " + isActive(CobbleUtils.config.isParty()));
    LOGGER.info("§e| §6Storage Rewards: " + isActive(CobbleUtils.config.isStorageRewards()));
    LOGGER.info("§e| §6Pokerus: " + isActive(CobbleUtils.config.getPokerus().isActive()));
    LOGGER.info("§e| §6Breeding: " + isActive(CobbleUtils.breedconfig.isActive()));
    LOGGER.info("§e| §6Bosses: " + isActive(CobbleUtils.config.isBoss()));
    LOGGER.info("§e| §6Supported economies: Impactor, BlanketEconomy, CobbleDollars and Vault");
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
    Utils.removeFiles(PATH_PARTY_DATA);

    CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> {
      CommandTree.register(dispatcher, registry);
      CommandsParty.register(dispatcher, registry);
    });

    LifecycleEvent.SERVER_STARTED.register(server -> {
      load();
      CustomPokemonProperty.Companion.register(MinIvsPropertyType.getInstance());
      CustomPokemonProperty.Companion.register(SizePropertyType.getInstance());
      CustomPokemonProperty.Companion.register(ScalePropertyType.getInstance());
      if (CobbleUtils.breedconfig.isActive())
        CustomPokemonProperty.Companion.register(BreedablePropertyType.getInstance());
      if (CobbleUtils.config.getPokerus().isActive())
        CustomPokemonProperty.Companion.register(PokerusPropertyType.getInstance());
    });

    LifecycleEvent.SERVER_STOPPING.register(server -> {
      scheduledTasks.forEach(task -> task.cancel(true));
      scheduledTasks.clear();
      ArraysPokemons.all.clear();
      CreatePartyEvent.CREATE_PARTY_EVENT.clear();
      DeletePartyEvent.DELETE_PARTY_EVENT.clear();
      scheduler.shutdownNow(); // Apaga los hilos activos

      try {
        // Espera un tiempo para asegurarse que los hilos se cierren correctamente
        if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {  // Extiende el tiempo a 5 segundos
          LOGGER.warn("El scheduler no pudo cerrarse a tiempo, forzando la interrupción");
          scheduler.shutdownNow();
        }
      } catch (InterruptedException ex) {
        LOGGER.error("El apagado del scheduler fue interrumpido" + ex);
        scheduler.shutdownNow();
        Thread.currentThread().interrupt();  // Restablecer el estado de interrupción
      }
    });


    LifecycleEvent.SERVER_LEVEL_LOAD.register(level -> server = level.getServer());

    PlayerEvent.PLAYER_JOIN.register(player -> {


      //Rewards
      RewardsData rewardsData = rewardsManager.getRewardsData().computeIfAbsent(
        player.getUuid(),
        uuid -> new RewardsData(player.getGameProfile().getName(), player.getUuid())
      );
      rewardsData.init();
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
      // Remove unnecesary data from rewards
      if (config.isStorageRewards()) {
        rewardsManager.getRewardsData().remove(player.getUuid());
      }
    });


    InteractionEvent.RIGHT_CLICK_ITEM.register(ItemRightClickEvents::register);


    // ? Add the event for fishing a pokemon
    FeaturesRegister.register();

    PartyPlaceholder.register();
  }

  private static void tasks() {
    for (ScheduledFuture<?> task : scheduledTasks) {
      task.cancel(true);
    }
    scheduledTasks.clear();

    ScheduledFuture<?> alertreward =
      scheduler.scheduleAtFixedRate(() -> server.getPlayerManager().getPlayerList().forEach(player -> {
        RewardsData rewardsData = rewardsManager.getRewardsData().get(player.getUuid());
        if (RewardsUtils.hasRewards(player)) {
          int amount = rewardsData.getAmount();
          PlayerUtils.sendMessage(player,
            language.getMessageHaveRewards()
              .replace("%amount%", String.valueOf(amount)),
            CobbleUtils.language.getPrefixStorageRewards());
        }
      }), 0, CobbleUtils.config.getAlertreward(), TimeUnit.MINUTES);

    ScheduledFuture<?> fixSize =
      scheduler.scheduleAtFixedRate(() -> server.getPlayerManager().getPlayerList().forEach(
        player -> {
          Cobblemon.INSTANCE.getStorage().getParty(player).forEach(ScaleEvent::solveScale);
          Cobblemon.INSTANCE.getStorage().getPC(player).forEach(ScaleEvent::solveScale);
        }
      ), 0, 30, TimeUnit.MINUTES);


    scheduledTasks.add(alertreward);
    scheduledTasks.add(fixSize);

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
