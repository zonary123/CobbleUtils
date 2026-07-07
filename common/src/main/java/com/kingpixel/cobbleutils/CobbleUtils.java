package com.kingpixel.cobbleutils;

import com.cobblemon.mod.common.api.properties.CustomPokemonProperty;
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
import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.events.ItemRightClickEvents;
import com.kingpixel.cobbleutils.tasks.RegistryTasks;
import com.kingpixel.cobbleutils.util.CobbleUtilsBridgeGTS;
import com.kingpixel.cobbleutils.util.SpawnRates;
import com.kingpixel.cobbleutils.util.UtilsLogger;
import com.kingpixel.cobbleutils.util.async.AsyncContext;
import com.kingpixel.cobbleutils.util.async.UtilsAsync;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main initializer and lifecycle management hub for the CobbleUtils platform.
 *
 * <p>Coordinates database mapping handshakes, redis subscription telemetry hooks,
 * asynchronous thread execution queues, and clean resource shutdowns during the
 * server lifecycle transition phases.</p>
 */
public class CobbleUtils {
  public static final String MOD_ID = "cobbleutils";
  public static final String MOD_NAME = "CobbleUtils";
  public static final String PATH = getPathMod().toString();
  public static final String PATH_LANG = getPathMod().resolve("lang").toString();

  @Deprecated(forRemoval = true)
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

  /**
   * The central, optimized multi-threaded context managing asynchronous operations for CobbleUtils.
   */
  public static final AsyncContext ASYNC = UtilsAsync.createContext(MOD_ID, MOD_NAME, 1, 4);
  public static Lang language = new Lang();
  public static RedisManager redisManager;

  /**
   * Primary bootstrapper invoked by the mod loader framework entrypoint.
   */
  public static void init() {
    try {
      Class.forName("org.bson.conversions.Bson");
    } catch (ClassNotFoundException e) {
      LOGGER_RAW.warn("Bson library class not found, MongoDB features might be limited.", e);
    }
    try {
      server = (MinecraftServer) FabricLoader.getInstance().getGameInstance();
    } catch (Exception e) {
      LOGGER_RAW.error("Failed to fetch MinecraftServer instance from FabricLoader", e);
    }
    tasks();
    events();
  }

  /**
   * Triggers file deployments, dependency validations, and external API hooks.
   */
  public static void load() {
    files();
    sign();
    try {
      if (config.isGtsSupport()) {
        new CobbleUtilsBridgeGTS();
      }
    } catch (NoClassDefFoundError | NoSuchMethodError | Exception e) {
      LOGGER_RAW.error("Error while trying to initialize GtsEconomyProvider: " + e.getMessage());
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

  /**
   * Prints structural diagnostics and support index data out onto the active server console stream.
   *
   * @param identifier Target mod bundle tracking ID.
   * @param version    Fallback version token string.
   * @param github     The root path token matching the upstream GitHub project branch destination.
   */
  public static void info(String identifier, String version, String github) {
    String finalVersion = version;
    String finalName = identifier;
    String authors = "Zonary123";
    if ("fabric".equals(ArchitecturyTarget.getCurrentTarget())) {
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
    LOGGER_RAW.info("§e| §6Donate: §9https://ko-fi.com/zonary123");
    LOGGER_RAW.info("§e+-------------------------------+");
  }

  /**
   * Binds logical handlers onto the global application processing network pipelines.
   */
  private static void events() {
    files();
    try {
      if (config.isRedisMessaging()) {
        redisManager = config.getRedis().getManager();
        redisManager.registerHandler(new RedisMessageHandler());
        redisManager.registerHandler(new RedisTeleportHandler());
        redisManager.registerHandler(new RedisUserCacheHandler());
      }
    } catch (NoClassDefFoundError | NoSuchMethodError | Exception e) {
      LOGGER_RAW.error("Error while trying to initialize RedisManager: " + e.getMessage());
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
      try {
        // Safe staging: serializes active blocks data into memory buffers before the worlds start to unload.
        ChunkBlockStorageManager.shutdownAsync();
      } catch (Exception e) {
        LOGGER_RAW.error("Error staging ChunkBlockStorageManager data: ", e);
      }
    });

    LifecycleEvent.SERVER_STOPPED.register(server -> {
      LOGGER_RAW.info("[SERVER_STOPPED] Initiating global shutdown sequence...");

      try {
        LOGGER_RAW.info("Flushing and closing shared multi-mod thread pools (UtilsAsync)...");
        com.kingpixel.cobbleutils.util.async.UtilsAsync.shutdownAll();
      } catch (Exception e) {
        LOGGER_RAW.error("Error shutting down global UtilsAsync: ", e);
      }

      LOGGER_RAW.info("Closing core database connections...");
      try {
        RedisService.shutdown();
      } catch (Exception ignored) {
      }

      try {
        MongoDBService.shutdown();
      } catch (Exception ignored) {
      }

      try {
        SQLService.shutdown();
      } catch (Exception ignored) {
      }

      CobbleUtils.LOGGER_RAW.info("CobbleUtils backup and database services closed successfully.");
    });

    PlayerEvent.PLAYER_JOIN.register((player) -> runAsync(() -> {
      try {
        UserModel user = DataBaseFactory.users().findUserByUUID(player.getUuid());
        if (user == null) {
          user = new UserModel(player);
        }
        user.connect(player);
        user.fix();
        DataBaseFactory.users().save(user);
        DataBaseUsers.USERS.put(player.getUuid(), user);
      } catch (Exception e) {
        LOGGER_RAW.error("Failed to process PLAYER_JOIN asynchronously for " + player.getName().getString(), e);
      }
    }));

    PlayerEvent.PLAYER_QUIT.register((player) -> {
      com.kingpixel.cobbleutils.Model.Animations.core.AnimationQueue.clearQueue(player.getUuid());
      runAsync(() -> {
        try {
          DataBaseFactory.users().disconnected(player);
          DataBaseFactory.users().removeIfNecessary(player.getUuid());
        } catch (Exception e) {
          LOGGER_RAW.error("Failed to process PLAYER_QUIT asynchronously for " + player.getName().getString(), e);
        }
      });
    });

    CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> {
      CustomPokemonProperty.Companion.register(MinIvsPropertyType.INSTANCE);
      CustomPokemonProperty.Companion.register(LegendaryPropertyType.INSTANCE);
      CommandTree.register(dispatcher, registry);
      commandRegistryAccess = registry;
    });

    InteractionEvent.RIGHT_CLICK_ITEM.register(ItemRightClickEvents::register);
    CobbleUtilsEvents.register();
  }

  /**
   * Shuts down an ExecutorService instance cleanly, allowing a brief grace period before forcing termination.
   *
   * @param executor Target structural ThreadPool executor demanding final decommissioning.
   */
  public static void shutdownAndAwait(ExecutorService executor) {
    if (executor == null || executor.isShutdown()) {
      return;
    }
    executor.shutdown();
    try {
      if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
        LOGGER_RAW.warn("An executor did not terminate within 3 seconds, forcing hard shutdown...");
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      LOGGER_RAW.error("Executor shutdown process was interrupted, forcing immediate shutdown.");
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Dispatches a task to execute natively on the default background worker pool thread context.
   *
   * @param runnable Abstract functional interface housing executable instructions.
   *
   * @return A CompletableFuture object tracking completion properties of the task.
   */
  public static CompletableFuture<Void> runAsync(Runnable runnable) {
    return ASYNC.runAsync(runnable);
  }

  /**
   * Dispatches a task onto the context worker pool pipeline (Maintains backward framework signature compatibility).
   *
   * @param runnable Abstract functional interface housing executable instructions.
   * @param executor Context parameters mapped to alternate pool frameworks.
   *
   * @return A CompletableFuture tracking task evaluation.
   */
  public static CompletableFuture<Void> runAsync(Runnable runnable, ExecutorService executor) {
    return ASYNC.runAsync(runnable);
  }

  /**
   * Returns the canonical configuration path matching the primary gaming instance environment.
   *
   * @return System configuration directory Path.
   */
  public static Path getPath() {
    return Platform.getConfigFolder();
  }

  /**
   * Returns the sub-allocated structural file config storage space dedicated exclusively to this mod ID namespace.
   *
   * @return Core mod config base directory Path.
   */
  public static Path getPathMod() {
    return Platform.getConfigFolder().resolve(MOD_ID);
  }
}