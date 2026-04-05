package com.kingpixel.cobbleutils.util.redis;

import com.kingpixel.cobbleutils.CobbleUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central service that manages shared {@link RedisManager} instances.
 * <p>
 * When multiple mods connect to the same Redis server (same host:port),
 * this service ensures they share a single connection pool and subscriber
 * thread, reducing resource usage on both the game server and the Redis server.
 *
 * <h3>Usage from a mod's config</h3>
 * <pre>{@code
 * // In your mod's config class:
 * private RedisConfig redis = new RedisConfig();
 *
 * // To get the manager:
 * RedisManager mgr = redis.getManager(); // delegates to RedisService
 * mgr.registerHandler(new MyHandler());
 * mgr.publish("my-channel", json);
 * }</pre>
 *
 * <h3>Usage directly</h3>
 * <pre>{@code
 * RedisConfig cfg = new RedisConfig("localhost", 6379, "", "mymod");
 * RedisManager mgr = RedisService.getOrCreateManager(cfg);
 * mgr.registerHandler(new MyHandler());
 * }</pre>
 *
 * <h3>Shutdown</h3>
 * Call {@link #shutdown()} during server stop to close all connections:
 * <pre>{@code
 * LifecycleEvent.SERVER_STOPPING.register(server -> RedisService.shutdown());
 * }</pre>
 *
 * @see RedisConfig
 * @see RedisManager
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RedisService {

  /**
   * Map of active Redis managers keyed by {@code "host:port"}.
   * Ensures a single pool per unique Redis server endpoint.
   */
  private static final Map<String, RedisManager> MANAGERS = new ConcurrentHashMap<>();

  /**
   * Retrieves an existing {@link RedisManager} for the given host:port,
   * or creates and initializes a new one if none exists yet.
   * <p>
   * This method is thread-safe. The returned manager is fully initialized
   * and ready to accept handler registrations and publish calls.
   *
   * @param config The Redis configuration specifying host, port, password, and channel.
   * @return A shared, initialized {@link RedisManager} instance.
   */
  public static RedisManager getOrCreateManager(RedisConfig config) {
    String connectionKey = (config.getHost() + ":" + config.getPort()).toLowerCase();

    return MANAGERS.computeIfAbsent(connectionKey, k -> {
      CobbleUtils.LOGGER.info("Creating new Redis connection for: " + connectionKey);
      RedisManager manager = new RedisManager(config);
      manager.init();
      return manager;
    });
  }

  /**
   * Gracefully closes all active Redis connections and clears the manager pool.
   * <p>
   * This should be called once during server shutdown to ensure all resources
   * (connection pools, subscriber threads, health check executors) are released.
   */
  public static void shutdown() {
    CobbleUtils.LOGGER.info("Shutting down all Redis connections...");
    MANAGERS.values().forEach(RedisManager::close);
    MANAGERS.clear();
  }
}