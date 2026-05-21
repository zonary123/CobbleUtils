package com.kingpixel.cobbleutils.util.sql;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central service that manages shared {@link SQLManager} instances.
 * <p>
 * When multiple mods connect to the same SQL database (same type + URL),
 * this service ensures they share a single HikariCP connection pool,
 * reducing resource usage on both the game server and the database.
 *
 * <h3>Supported databases</h3>
 * <ul>
 *   <li>MySQL</li>
 *   <li>MariaDB</li>
 *   <li>SQLite</li>
 *   <li>H2</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * DataBaseConfig cfg = new DataBaseConfig(DataBaseType.MYSQL, "mydb", "localhost:3306", "root", "pass");
 * SQLManager mgr = SQLService.getOrCreateManager(cfg);
 *
 * mgr.execute("CREATE TABLE IF NOT EXISTS test (id INT PRIMARY KEY)");
 * String result = mgr.query("SELECT * FROM test WHERE id = ?", rs -> {
 *   return rs.next() ? rs.getString(1) : null;
 * }, 1);
 * }</pre>
 *
 * <h3>Shutdown</h3>
 * Call {@link #shutdown()} during server stop to close all pools:
 * <pre>{@code
 * LifecycleEvent.SERVER_STOPPING.register(server -> SQLService.shutdown());
 * }</pre>
 *
 * @see SQLManager
 * @see DataBaseConfig
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SQLService {

  /**
   * Map of active SQL managers keyed by {@code "TYPE:URL"}.
   * Ensures a single HikariCP pool per unique database endpoint.
   */
  private static final Map<String, SQLManager> MANAGERS = new ConcurrentHashMap<>();

  /**
   * Retrieves an existing {@link SQLManager} for the given database endpoint,
   * or creates and initializes a new one if none exists yet.
   * <p>
   * This method is thread-safe. The returned manager is fully initialized
   * and ready to execute queries.
   *
   * @param config The database configuration with type, URL, database, user, and password.
   *
   * @return A shared, initialized {@link SQLManager} instance.
   */
  public static SQLManager getOrCreateManager(DataBaseConfig config) {
    Objects.requireNonNull(config, "DataBaseConfig cannot be null");
    String connectionKey = SQLManager.buildConnectionKey(config);

    return MANAGERS.compute(connectionKey, (key, existing) -> {
      if (existing != null && existing.isAlive()) {
        return existing;
      }

      if (existing != null) {
        CobbleUtils.LOGGER_RAW.warn("Replacing stale SQL connection pool for {}", config.getType());
        closeQuietly(existing);
      }

      CobbleUtils.LOGGER_RAW.info("Creating new SQL connection pool for {}", config.getType());
      SQLManager manager = new SQLManager(config);
      manager.init();
      return manager;
    });
  }

  private static void closeQuietly(SQLManager manager) {
    try {
      manager.close();
    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.warn("Error while closing stale SQL manager", e);
    }
  }

  /**
   * Gracefully closes all active SQL connection pools and clears the manager pool.
   * <p>
   * This should be called once during server shutdown to ensure all
   * HikariCP pools are properly closed and resources are released.
   */
  public static void shutdown() {
    CobbleUtils.LOGGER_RAW.info("Shutting down all SQL connection pools...");
    if (MANAGERS.isEmpty()) {
      CobbleUtils.LOGGER_RAW.info("No active SQL connection pools found.");
      return;
    }
    MANAGERS.values().forEach(SQLManager::close);
    MANAGERS.clear();
    CobbleUtils.LOGGER_RAW.info("All SQL connection pools shut down successfully.");
  }

  public static int getActiveConnections() {
    return MANAGERS.size();
  }
}
