package com.kingpixel.cobbleutils;

import com.cobblemon.mod.common.api.properties.CustomPokemonProperty;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.kingpixel.cobbleutils.Model.properties.LegendaryPropertyType;
import com.kingpixel.cobbleutils.Model.properties.MinIvsPropertyType;
import com.kingpixel.cobbleutils.command.CommandTree;
import com.kingpixel.cobbleutils.command.suggests.CobbleUtilsSuggests;
import com.kingpixel.cobbleutils.config.AdvancedRewardsConfig;
import com.kingpixel.cobbleutils.config.Config;
import com.kingpixel.cobbleutils.config.Lang;
import com.kingpixel.cobbleutils.config.RewardsConfig;
import com.kingpixel.cobbleutils.database.DataBaseFactory;
import com.kingpixel.cobbleutils.database.blocks.ChunkBlockStorageManager;
import com.kingpixel.cobbleutils.database.users.DataBaseUsers;
import com.kingpixel.cobbleutils.database.users.UserModel;
import com.kingpixel.cobbleutils.events.ItemRightClickEvents;
import com.kingpixel.cobbleutils.network.NetworkInitializer;
import com.kingpixel.cobbleutils.tasks.RegistryTasks;
import com.kingpixel.cobbleutils.util.*;
import com.kingpixel.cobbleutils.util.async.AsyncContext;
import com.kingpixel.cobbleutils.util.mongodb.MongoDBService;
import com.kingpixel.cobbleutils.util.redis.RedisManager;
import com.kingpixel.cobbleutils.util.redis.RedisService;
import com.kingpixel.cobbleutils.util.redis.handlers.RedisMessageHandler;
import com.kingpixel.cobbleutils.util.redis.handlers.RedisTeleportHandler;
import com.kingpixel.cobbleutils.util.redis.handlers.RedisUserCacheHandler;
import com.kingpixel.cobbleutils.util.sql.SQLService;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.injectables.targets.ArchitecturyTarget;
import dev.architectury.platform.Platform;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.*;

public class CobbleUtils {
  public static final String MOD_ID = "cobbleutils";
  public static final String MOD_NAME = "CobbleUtils";
  public static final String PATH = Platform.getConfigFolder().resolve("cobbleutils").toString();
  public static final String PATH_LANG = Path.of(PATH).resolve("lang").toString();
  public static final String PATH_BREED = Path.of(PATH).resolve("breed").toString();
  public static final String PATH_BREED_DATA = Path.of(PATH_BREED).resolve("data").toString();
  public static final UtilsLogger LOGGER = new UtilsLogger();
  public static final Logger LOGGER_RAW = UtilsLogger.getLogger(MOD_NAME);

  @Setter
  @Getter
  private static @Nullable String serverName = null;
  public static CommandRegistryAccess commandRegistryAccess;
  public static MinecraftServer server;
  public static Config config = new Config();
  public static RewardsConfig rewardsConfig = new RewardsConfig();
  public static AdvancedRewardsConfig advancedRewardsConfig = new AdvancedRewardsConfig();
  public static SpawnRates spawnRates = new SpawnRates();
  // Lang
  public static final AsyncContext ASYNC = com.kingpixel.cobbleutils.util.async.UtilsAsync.createContext(MOD_ID,
    MOD_NAME, 1, 1);
  public static Lang language = new Lang();
  public static RedisManager redisManager;
  private static final ExecutorService EXECUTOR_COBBLEUTILS = Executors.newFixedThreadPool(1, new ThreadFactoryBuilder()
    .setDaemon(true)
    .setNameFormat("CobbleUtils Executor-%d")
    .build());
  public static final ScheduledExecutorService SCHEDULER_COBBLEUTILS = Executors.newScheduledThreadPool(1,
    new ThreadFactoryBuilder()
      .setDaemon(true)
      .setNameFormat("CobbleUtils Scheduled Executor-%d")
      .build());

  public static void init() {
    try {
      Class.forName("org.bson.conversions.Bson");
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
    try {
      server = (MinecraftServer) FabricLoader.getInstance().getGameInstance();
    } catch (Exception e) {
      e.printStackTrace();
    }
    tasks();
    events();
    NetworkInitializer.register();
  }

  public static void load() {
    files();
    sign();
    try {
      if (config.isGtsSupport()) {
        new CobbleUtilsBridgeGTS();
      }
    } catch (NoClassDefFoundError | NoSuchMethodError | Exception ignored) {
      LOGGER_RAW.error("Error while trying to get GtsEconomyProvider");
    }
  }

  private static void tasks() {
    RegistryTasks.register();
  }

  private static void files() {
    config.init();
    language.init();
    rewardsConfig.init();
    advancedRewardsConfig.init();
    DataBaseFactory.init(config.getDatabase());
  }

  private static void sign() {
    info(MOD_ID, "1.1.4", "CobbleUtils");
    LOGGER_RAW.info(
      "§e| §6Supported economies: Impactor, BlanketEconomy, CobbleDollars, SDMEconomy, PebbleEconomy and Vault");
    LOGGER_RAW.info("§e+-------------------------------+");
  }

  public static void info(String identifier, String github) {
    info(identifier, null, github);
  }

  public static void info(String identifier, String version, String github) {
    String finalVersion = version;
    String finalName = identifier;
    String authors = "Zonary123";
    if (ArchitecturyTarget.getCurrentTarget().equals("fabric")) {
      ModContainer mod = FabricLoader.getInstance().getAllMods()
        .stream()
        .filter(m -> m.getMetadata().getId().equals(identifier) ||
          m.getMetadata().getName().equals(identifier))
        .findFirst()
        .orElse(null);
      if (mod != null) {
        finalVersion = mod.getMetadata().getVersion().getFriendlyString();
        finalName = mod.getMetadata().getName();
        authors = String.join(", ", mod.getMetadata().getAuthors().stream().map(Person::getName).toList());
      }
    }
    LOGGER_RAW.info("§e+-------------------------------+");
    LOGGER_RAW.info("§e| §6" + finalName);
    LOGGER_RAW.info("§e+-------------------------------+");
    LOGGER_RAW.info("§e| §6Version: §e" + finalVersion);
    LOGGER_RAW.info("§e| §6Author: §e" + authors);
    LOGGER_RAW.info("§e| §6Website: §9https://github.com/Zonary123/" + github);
    LOGGER_RAW.info("§e| §6Discord: §9https://discord.com/invite/fKNc7FnXpa");
    LOGGER_RAW.info("§e| §6Support: §9https://github.com/Zonary123/" + github + "/issues");
    LOGGER_RAW.info("§e| §dDonate: §9https://ko-fi.com/zonary123");
    LOGGER_RAW.info("§e+-------------------------------+");
  }

  private static void events() {
    files();
    try {
      if (config.isRedisMessaging()) {
        redisManager = config.getRedis().getManager();
        redisManager.registerHandler(new RedisMessageHandler());
        redisManager.registerHandler(new RedisTeleportHandler());
        redisManager.registerHandler(new RedisUserCacheHandler());
      }
    } catch (NoClassDefFoundError | NoSuchMethodError | Exception ignored) {
      LOGGER_RAW.error("Error while trying to initialize RedisManager");
    }

    LifecycleEvent.SERVER_STARTING.register(server -> {
      CobbleUtils.server = server;
      ChunkBlockStorageManager.init(server);
    });

    LifecycleEvent.SERVER_STARTED.register(server -> {
      spawnRates.init();
      load();
      CobbleUtilsSuggests.SUGGESTS_PLAYER_OFFLINE_AND_ONLINE.refreshIfNeeded();
    });

    LifecycleEvent.SERVER_STOPPING.register(server1 -> {
      ChunkBlockStorageManager.shutdownAsync().join();
      com.kingpixel.cobbleutils.util.async.UtilsAsync.shutdownAll();
      shutdownAndAwait(EXECUTOR_COBBLEUTILS);
      shutdownAndAwait(Utils.IO_EXECUTOR);
      shutdownAndAwait(SCHEDULER_COBBLEUTILS);
    });

    LifecycleEvent.SERVER_STOPPED.register(server -> {
      RedisService.shutdown();
      MongoDBService.shutdown();
      SQLService.shutdown();
    });

    PlayerEvent.PLAYER_JOIN.register((player) -> {
      runAsync(() -> {
        UserModel user = DataBaseFactory.dataBaseUsers.findUserByUUID(player.getUuid());
        if (user == null)
          user = new UserModel(player);
        user.connect(player);
        user.fix();
        DataBaseFactory.dataBaseUsers.saveOrUpdateUser(user);
        DataBaseUsers.USERS.put(player.getUuid(), user);
      });
    });

    PlayerEvent.PLAYER_QUIT.register((player) -> runAsync(() -> {
      DataBaseFactory.dataBaseUsers.disconnected(player);
      DataBaseFactory.dataBaseUsers.removeIfNecessary(player.getUuid());
    }));

    CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> {
      CustomPokemonProperty.Companion.register(MinIvsPropertyType.INSTANCE);
      CustomPokemonProperty.Companion.register(LegendaryPropertyType.INSTANCE);
      CommandTree.register(dispatcher, registry);
      commandRegistryAccess = registry;
    });

    InteractionEvent.RIGHT_CLICK_ITEM.register(ItemRightClickEvents::register);
  }

  public static void shutdownAndAwait(ExecutorService executor) {
    if (executor == null || executor.isShutdown()) {
      return;
    }
    executor.shutdown();
    try {
      if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
        LOGGER_RAW.warn("Executor did not terminate in time, forcing shutdown...");
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      LOGGER_RAW.error("Executor shutdown was interrupted");
      e.printStackTrace();
      executor.shutdownNow();
    }
  }

  /**
   * Run async with CobbleUtils executor
   *
   * @param runnable
   * @return CompletableFuture<Void>
   */
  public static CompletableFuture<Void> runAsync(Runnable runnable) {
    return runAsync(runnable, EXECUTOR_COBBLEUTILS);
  }

  /**
   * Run async with custom executor
   *
   * @param runnable
   * @param executor
   * @return CompletableFuture<Void>
   */
  public static CompletableFuture<Void> runAsync(Runnable runnable, ExecutorService executor) {
    return UtilsAsync.runAsync(runnable, executor);
  }

  public static Path getPath() {
    return Platform.getConfigFolder();
  }

  public static Path getPathMod() {
    return Platform.getConfigFolder().resolve(MOD_ID);
  }

}
