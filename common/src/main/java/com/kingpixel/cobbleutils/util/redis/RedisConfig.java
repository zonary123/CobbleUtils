package com.kingpixel.cobbleutils.util.redis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Serializable configuration model for Redis connections.
 * <p>
 * This class is designed to be embedded in any mod's configuration file
 * and serialized/deserialized via GSON. Each mod can define its own
 * {@code RedisConfig} instance with independent channel namespaces,
 * while the underlying connection pool is shared through {@link RedisService}.
 *
 * <h3>Usage in a mod config</h3>
 * <pre>{@code
 * public class MyModConfig {
 *   private RedisConfig redis = new RedisConfig();
 *
 *   public void onEnable() {
 *     RedisManager mgr = redis.getManager();
 *     mgr.registerHandler(new MyHandler());
 *   }
 * }
 * }</pre>
 *
 * <h3>Programmatic usage</h3>
 * <pre>{@code
 * RedisConfig cfg = new RedisConfig("10.0.0.5", 6379, "secret", "mymod-channel");
 * RedisManager mgr = cfg.getManager();
 * }</pre>
 *
 * @see RedisManager
 * @see RedisService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisConfig {

  /**
   * Redis server hostname or IP address.
   * Defaults to {@code "localhost"}.
   */
  private String host = "localhost";

  /**
   * Redis server port.
   * Defaults to {@code 6379} (standard Redis port).
   */
  private int port = 6379;

  /**
   * Redis server password for authentication.
   * Leave empty ({@code ""}) if the server does not require authentication.
   */
  private String password = "";

  /**
   * Base channel prefix used as a namespace for Pub/Sub messaging.
   * <p>
   * All channels registered through this config's manager will be prefixed
   * with this value, e.g. {@code "cobbleutils-messaging:my-handler-id"}.
   * Using different channel values per mod prevents message collisions.
   */
  private String channel = "cobbleutils-messaging";

  /**
   * Maximum number of connections in the Jedis connection pool.
   * Defaults to {@code 16}. Increase for high-throughput scenarios.
   */
  private int maxConnections = 16;

  /**
   * Maximum time in milliseconds to wait for a connection from the pool
   * before throwing an exception. Defaults to {@code 5000} (5 seconds).
   */
  private int timeout = 5000;

  /**
   * Convenience constructor for the most common connection parameters.
   * Uses default values for {@code maxConnections} and {@code timeout}.
   *
   * @param host     The Redis server hostname or IP.
   * @param port     The Redis server port.
   * @param password The authentication password (empty string for none).
   * @param channel  The base channel namespace.
   */
  public RedisConfig(String host, int port, String password, String channel) {
    this.host = host;
    this.port = port;
    this.password = password;
    this.channel = channel;
  }

  /**
   * Returns the {@link RedisManager} associated with this configuration.
   * <p>
   * Delegates to {@link RedisService#getOrCreateManager(RedisConfig)}, which
   * ensures that configs pointing to the same {@code host:port} share a single
   * connection pool and subscriber thread.
   *
   * @return An initialized, thread-safe {@link RedisManager} instance.
   */
  public RedisManager getManager() {
    return RedisService.getOrCreateManager(this);
  }
}