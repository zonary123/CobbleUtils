package com.kingpixel.cobbleutils.util.sql;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.Model.DataBaseType;
import com.kingpixel.cobbleutils.util.async.AsyncContext;
import com.kingpixel.cobbleutils.util.async.UtilsAsync;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Instance-based SQL connection manager using HikariCP.
 * <p>
 * Supports MySQL, MariaDB, SQLite, and H2 databases through a shared
 * {@link HikariDataSource} connection pool. Use
 * {@link SQLService#getOrCreateManager(DataBaseConfig)} to obtain an instance
 * and avoid duplicate connection pools.
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>Create: {@code new SQLManager(config)}</li>
 *   <li>Initialize pool: {@link #init()}</li>
 *   <li>Use: {@link #getConnection()}, {@link #execute(String, Object...)},
 *       {@link #query(String, ResultSetMapper, Object...)}</li>
 *   <li>Shutdown: {@link #close()}</li>
 * </ol>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * DataBaseConfig cfg = new DataBaseConfig(DataBaseType.MYSQL, "mydb", "localhost:3306", "root", "pass");
 * SQLManager mgr = SQLService.getOrCreateManager(cfg);
 *
 * // Execute DDL/DML
 * mgr.execute("CREATE TABLE IF NOT EXISTS players (uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(16))");
 * mgr.execute("INSERT INTO players (uuid, name) VALUES (?, ?)", uuid, name);
 *
 * // Query
 * String name = mgr.query("SELECT name FROM players WHERE uuid = ?", rs -> {
 *   return rs.next() ? rs.getString("name") : null;
 * }, uuid);
 *
 * // on shutdown:
 * SQLService.shutdown();
 * }</pre>
 *
 * @see SQLService
 * @see DataBaseConfig
 */
public class SQLManager {

  private static final String SQL_ASYNC_CONTEXT_ID = "cobbleutils-sql";
  private static final String SQL_ASYNC_THREAD_NAME = "SQL-IO";

  private final DataBaseConfig config;

  @Getter
  private volatile HikariDataSource dataSource;

  /**
   * Whether this manager has an active connection pool.
   */
  @Getter
  private final AtomicBoolean connected = new AtomicBoolean(false);

  /**
   * Creates a new manager bound to the given configuration.
   * Call {@link #init()} afterwards to create the connection pool.
   *
   * @param config Connection parameters (type, url, database, user, password).
   */
  public SQLManager(DataBaseConfig config) {
    this.config = config;
  }

  /**
   * Initializes the {@link HikariDataSource} connection pool based on the
   * database type specified in the configuration.
   * <p>
   * Automatically configures the JDBC URL, driver class, and pool settings
   * for MySQL, MariaDB, SQLite, and H2.
   */
  public synchronized void init() {
    try {
      HikariConfig hikariConfig = new HikariConfig();

      String jdbcUrl = buildJdbcUrl();
      hikariConfig.setJdbcUrl(jdbcUrl);
      hikariConfig.setPoolName("CobbleUtils-SQL-" + config.getType());

      switch (config.getType()) {
        case MYSQL, MARIADB -> {
          hikariConfig.setUsername(config.getUser());
          hikariConfig.setPassword(config.getPassword());
          hikariConfig.setMaximumPoolSize(10);
          hikariConfig.setMinimumIdle(2);
        }
        case H2 -> {
          // H2 supports optional auth
          if (config.getUser() != null && !config.getUser().isEmpty()) {
            hikariConfig.setUsername(config.getUser());
            hikariConfig.setPassword(config.getPassword());
          }
          hikariConfig.setMaximumPoolSize(1);
          hikariConfig.setMinimumIdle(1);
        }
        default -> {
          // SQLite: embedded, no auth, single connection
          hikariConfig.setMaximumPoolSize(1);
          hikariConfig.setMinimumIdle(1);
        }
      }

      hikariConfig.setConnectionTimeout(5_000);
      hikariConfig.setIdleTimeout(300_000);
      hikariConfig.setMaxLifetime(600_000);
      // Do NOT set connectionTestQuery for JDBC4-compliant drivers (all modern drivers).
      // HikariCP uses Connection.isValid() automatically.

      dataSource = new HikariDataSource(hikariConfig);

      // Verify connectivity and apply DB-specific setup
      try (Connection conn = dataSource.getConnection()) {
        connected.set(conn.isValid(3));

        // Enable WAL mode for SQLite (better concurrent read performance)
        if (config.getType() == DataBaseType.SQLITE) {
          try (var stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA synchronous=NORMAL");
          }
        }
      }

      CobbleUtils.LOGGER_RAW.info("SQL pool initialized for " + config.getType());

    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.error("Could not initialize SQL pool for " + config.getType() + ": " + e.getMessage());
      throw new RuntimeException("Failed to initialize SQL pool", e);
    }
  }

  /**
   * Runs a blocking SQL operation asynchronously using a shared SQL IO context.
   */
  public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
    Objects.requireNonNull(supplier, "supplier cannot be null");
    return getSqlAsyncContext().supply(supplier);
  }

  /**
   * Runs a blocking SQL operation asynchronously with custom timeout.
   */
  public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier, long timeout, java.util.concurrent.TimeUnit unit) {
    Objects.requireNonNull(supplier, "supplier cannot be null");
    Objects.requireNonNull(unit, "unit cannot be null");
    return getSqlAsyncContext().supply(supplier, timeout, unit);
  }

  /**
   * Runs a void SQL task asynchronously using a shared SQL IO context.
   */
  public CompletableFuture<Void> runAsync(Runnable runnable) {
    Objects.requireNonNull(runnable, "runnable cannot be null");
    return getSqlAsyncContext().runAsync(runnable);
  }

  /**
   * Executes custom SQL logic with a managed connection asynchronously.
   */
  public <T> CompletableFuture<T> withConnectionAsync(SQLConnectionFunction<T> action) {
    Objects.requireNonNull(action, "action cannot be null");
    return supplyAsync(() -> {
      try (Connection connection = getConnection()) {
        return action.apply(connection);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Executes custom SQL logic with a managed connection asynchronously.
   */
  public CompletableFuture<Void> withConnectionAsync(SQLConnectionConsumer action) {
    Objects.requireNonNull(action, "action cannot be null");
    return runAsync(() -> {
      try (Connection connection = getConnection()) {
        action.accept(connection);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Async variant of {@link #execute(String, Object...)}.
   */
  public CompletableFuture<Integer> executeAsync(String sql, Object... params) {
    return supplyAsync(() -> execute(sql, params));
  }

  /**
   * Async variant of {@link #query(String, ResultSetMapper, Object...)}.
   */
  public <T> CompletableFuture<T> queryAsync(String sql, ResultSetMapper<T> mapper, Object... params) {
    return supplyAsync(() -> query(sql, mapper, params));
  }

  /**
   * Async variant of {@link #queryList(String, ResultSetMapper, Object...)}.
   */
  public <T> CompletableFuture<List<T>> queryListAsync(String sql, ResultSetMapper<T> rowMapper, Object... params) {
    return supplyAsync(() -> queryList(sql, rowMapper, params));
  }

  /**
   * Builds the JDBC URL based on the database type and configuration.
   *
   * @return The JDBC connection URL.
   */
  private String buildJdbcUrl() {
    return normalizeJdbcUrl(config);
  }

  static String normalizeJdbcUrl(DataBaseConfig config) {
    Objects.requireNonNull(config, "DataBaseConfig cannot be null");
    return switch (config.getType()) {
      case MYSQL -> normalizeNetworkJdbcUrl(
        config,
        "jdbc:mysql://",
        "useSSL=false",
        "allowPublicKeyRetrieval=true",
        "serverTimezone=UTC",
        "characterEncoding=utf8"
      );
      case MARIADB -> normalizeNetworkJdbcUrl(
        config,
        "jdbc:mariadb://",
        "useSSL=false",
        "characterEncoding=utf8"
      );
      case SQLITE -> normalizeSqliteJdbcUrl(config);
      case H2 -> normalizeH2JdbcUrl(config);
      default -> throw new IllegalArgumentException("Unsupported SQL type: " + config.getType());
    };
  }

  static String buildConnectionKey(DataBaseConfig config) {
    String normalizedUrl = normalizeJdbcUrl(config).toLowerCase(Locale.ROOT);
    String normalizedUser = normalize(config.getUser()).toLowerCase(Locale.ROOT);
    int credentialsHash = Objects.hash(normalizedUser, normalize(config.getPassword()));
    return config.getType() + ":" + normalizedUrl + ":" + Integer.toHexString(credentialsHash);
  }

  private static String normalizeNetworkJdbcUrl(DataBaseConfig config, String jdbcPrefix, String... defaultParams) {
    String url = normalize(config.getUrl());
    String database = normalize(config.getDatabase());

    String jdbcUrl;
    if (startsWithIgnoreCase(url, jdbcPrefix)) {
      jdbcUrl = url;
    } else {
      if (url.isBlank()) {
        throw new IllegalArgumentException("Database URL cannot be empty for " + config.getType());
      }
      String base = stripTrailingSlash(url);
      if (!database.isBlank() && !hasPathSegment(base)) {
        base = base + "/" + database;
      }
      jdbcUrl = jdbcPrefix + base;
    }

    return appendMissingQueryParams(jdbcUrl, defaultParams);
  }

  private static String normalizeSqliteJdbcUrl(DataBaseConfig config) {
    String url = normalize(config.getUrl());
    if (url.isBlank()) {
      url = normalize(config.getDatabase());
      if (url.isBlank()) {
        throw new IllegalArgumentException("SQLite URL cannot be empty");
      }
      if (!url.contains(".") && !url.endsWith("/") && !url.endsWith("\\")) {
        url = url + ".db";
      }
    }
    return startsWithIgnoreCase(url, "jdbc:sqlite:") ? url : "jdbc:sqlite:" + url;
  }

  private static String normalizeH2JdbcUrl(DataBaseConfig config) {
    String url = normalize(config.getUrl());
    if (url.isBlank()) {
      url = normalize(config.getDatabase());
      if (url.isBlank()) {
        throw new IllegalArgumentException("H2 URL cannot be empty");
      }
    }

    String jdbcUrl = startsWithIgnoreCase(url, "jdbc:h2:") ? url : "jdbc:h2:" + url;
    return appendMissingH2Options(jdbcUrl, "MODE=MySQL", "NON_KEYWORDS=VALUE");
  }

  private static String appendMissingQueryParams(String jdbcUrl, String... defaultParams) {
    if (defaultParams.length == 0) return jdbcUrl;

    StringBuilder result = new StringBuilder(jdbcUrl);
    Set<String> existingKeys = new HashSet<>();

    int queryIndex = jdbcUrl.indexOf('?');
    if (queryIndex >= 0 && queryIndex + 1 < jdbcUrl.length()) {
      String query = jdbcUrl.substring(queryIndex + 1);
      for (String part : query.split("&")) {
        if (part.isBlank()) continue;
        int eq = part.indexOf('=');
        String key = (eq >= 0 ? part.substring(0, eq) : part).toLowerCase(Locale.ROOT);
        existingKeys.add(key);
      }
    }

    boolean hasQuery = queryIndex >= 0;
    for (String param : defaultParams) {
      int eq = param.indexOf('=');
      String key = (eq >= 0 ? param.substring(0, eq) : param).toLowerCase(Locale.ROOT);
      if (existingKeys.contains(key)) continue;
      result.append(hasQuery ? '&' : '?').append(param);
      hasQuery = true;
    }

    return result.toString();
  }

  private static String appendMissingH2Options(String jdbcUrl, String... options) {
    String normalizedUpper = jdbcUrl.toUpperCase(Locale.ROOT);
    StringBuilder result = new StringBuilder(jdbcUrl);
    for (String option : options) {
      if (normalizedUpper.contains(option.toUpperCase(Locale.ROOT))) continue;
      if (result.charAt(result.length() - 1) != ';') {
        result.append(';');
      }
      result.append(option);
    }
    return result.toString();
  }

  private static boolean hasPathSegment(String url) {
    int schemeIndex = url.indexOf("://");
    String withoutScheme = schemeIndex >= 0 ? url.substring(schemeIndex + 3) : url;
    int queryIndex = withoutScheme.indexOf('?');
    String withoutQuery = queryIndex >= 0 ? withoutScheme.substring(0, queryIndex) : withoutScheme;
    return withoutQuery.contains("/");
  }

  private static String stripTrailingSlash(String value) {
    if (value.endsWith("/")) {
      return value.substring(0, value.length() - 1);
    }
    return value;
  }

  private static boolean startsWithIgnoreCase(String value, String prefix) {
    return value.regionMatches(true, 0, prefix, 0, prefix.length());
  }

  private static String normalize(String value) {
    return value == null ? "" : value.trim();
  }

  private static AsyncContext getSqlAsyncContext() {
    return UtilsAsync.createContext(SQL_ASYNC_CONTEXT_ID, SQL_ASYNC_THREAD_NAME, 2, 4);
  }

  /**
   * Obtains a connection from the HikariCP pool.
   * <p>
   * <b>Important:</b> Always close the connection after use (use try-with-resources).
   *
   * <h4>Example</h4>
   * <pre>{@code
   * try (Connection conn = mgr.getConnection()) {
   *   // use connection...
   * }
   * }</pre>
   *
   * @return A pooled {@link Connection}.
   * @throws SQLException if the pool is closed or a connection cannot be obtained.
   */
  public Connection getConnection() throws SQLException {
    HikariDataSource ds = this.dataSource;
    if (ds == null || ds.isClosed()) {
      throw new SQLException("SQL connection pool is not available.");
    }
    return ds.getConnection();
  }

  /**
   * Executes an update statement (INSERT, UPDATE, DELETE, CREATE, etc.).
   *
   * <h4>Example</h4>
   * <pre>{@code
   * // Create table
   * mgr.execute("CREATE TABLE IF NOT EXISTS players (uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(16))");
   *
   * // Insert
   * mgr.execute("INSERT INTO players (uuid, name) VALUES (?, ?)", uuid, name);
   *
   * // Update
   * int rows = mgr.execute("UPDATE players SET name = ? WHERE uuid = ?", newName, uuid);
   *
   * // Delete
   * mgr.execute("DELETE FROM players WHERE uuid = ?", uuid);
   * }</pre>
   *
   * @param sql    The SQL statement with {@code ?} placeholders.
   * @param params The parameters to bind to the statement.
   * @return The number of affected rows, or {@code -1} on error.
   */
  public int execute(String sql, Object... params) {
    try (Connection conn = getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      bindParams(stmt, params);
      return stmt.executeUpdate();
    } catch (SQLException e) {
      CobbleUtils.LOGGER_RAW.error("SQL execute error: " + sql + " -> " + e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Executes a batch of statements with different parameters.
   * <p>
   * Useful for bulk inserts or updates. All operations run in a single
   * transaction for better performance and atomicity.
   *
   * <h4>Example</h4>
   * <pre>{@code
   * List<Object[]> paramsList = new ArrayList<>();
   * paramsList.add(new Object[]{ "uuid-1", "Player1" });
   * paramsList.add(new Object[]{ "uuid-2", "Player2" });
   * paramsList.add(new Object[]{ "uuid-3", "Player3" });
   *
   * int total = mgr.executeBatch("INSERT INTO players (uuid, name) VALUES (?, ?)", paramsList);
   * }</pre>
   *
   * @param sql        The SQL statement with {@code ?} placeholders.
   * @param paramsList A list of parameter arrays, one per batch entry.
   * @return The total number of affected rows, or {@code -1} on error.
   */
  public int executeBatch(String sql, List<Object[]> paramsList) {
    try (Connection conn = getConnection()) {
      conn.setAutoCommit(false);
      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        for (Object[] params : paramsList) {
          bindParams(stmt, params);
          stmt.addBatch();
        }
        int[] results = stmt.executeBatch();
        conn.commit();
        int total = 0;
        for (int r : results) {
          if (r > 0) total += r;
        }
        return total;
      } catch (SQLException e) {
        // Rollback on error to avoid leaving the connection in a dirty state
        try {
          conn.rollback();
        } catch (SQLException rollbackEx) {
          CobbleUtils.LOGGER_RAW.error("SQL rollback error: " + rollbackEx.getMessage());
        }
        CobbleUtils.LOGGER_RAW.error("SQL batch error: " + e.getMessage());
        throw new RuntimeException(e);
      } finally {
        // Always restore auto-commit
        try {
          conn.setAutoCommit(true);
        } catch (SQLException ignored) {
        }
      }
    } catch (SQLException e) {
      CobbleUtils.LOGGER_RAW.error("SQL batch connection error: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Executes a query and maps the {@link ResultSet} to a single result.
   *
   * <h4>Example</h4>
   * <pre>{@code
   * // Find one player by UUID
   * String name = mgr.query(
   *   "SELECT name FROM players WHERE uuid = ?",
   *   rs -> rs.next() ? rs.getString("name") : null,
   *   uuid
   * );
   *
   * // Count rows
   * Integer count = mgr.query(
   *   "SELECT COUNT(*) FROM players",
   *   rs -> rs.next() ? rs.getInt(1) : 0
   * );
   *
   * // Check if exists
   * Boolean exists = mgr.query(
   *   "SELECT 1 FROM players WHERE uuid = ?",
   *   ResultSet::next,
   *   uuid
   * );
   * }</pre>
   *
   * @param sql    The SQL query with {@code ?} placeholders.
   * @param mapper A function that maps the ResultSet to the desired type.
   * @param params The parameters to bind to the query.
   * @param <T>    The return type.
   * @return The mapped result, or {@code null} on error.
   */
  public <T> T query(String sql, ResultSetMapper<T> mapper, Object... params) {
    try (Connection conn = getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      bindParams(stmt, params);
      try (ResultSet rs = stmt.executeQuery()) {
        return mapper.map(rs);
      }
    } catch (SQLException e) {
      CobbleUtils.LOGGER_RAW.error("SQL query error: " + sql + " -> " + e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Executes a query and maps each row to a list of results.
   *
   * <h4>Example</h4>
   * <pre>{@code
   * // Get all player names
   * List<String> names = mgr.queryList(
   *   "SELECT name FROM players",
   *   rs -> rs.getString("name")
   * );
   *
   * // Get players with a filter
   * List<String> online = mgr.queryList(
   *   "SELECT name FROM players WHERE online = ?",
   *   rs -> rs.getString("name"),
   *   true
   * );
   *
   * // Map to custom objects
   * List<PlayerData> players = mgr.queryList(
   *   "SELECT uuid, name FROM players",
   *   rs -> new PlayerData(rs.getString("uuid"), rs.getString("name"))
   * );
   * }</pre>
   *
   * @param sql       The SQL query with {@code ?} placeholders.
   * @param rowMapper A function that maps a single row to an object.
   * @param params    The parameters to bind to the query.
   * @param <T>       The element type.
   * @return A list of mapped results, or an empty list on error.
   */
  public <T> List<T> queryList(String sql, ResultSetMapper<T> rowMapper, Object... params) {
    try (Connection conn = getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      bindParams(stmt, params);
      try (ResultSet rs = stmt.executeQuery()) {
        List<T> results = new ArrayList<>();
        while (rs.next()) {
          results.add(rowMapper.map(rs));
        }
        return results;
      }
    } catch (SQLException e) {
      CobbleUtils.LOGGER_RAW.error("SQL queryList error: " + e.getMessage());
      return List.of();
    }
  }

  /**
   * Binds parameters to a {@link PreparedStatement}.
   *
   * @param stmt   The statement to bind parameters to.
   * @param params The parameters to bind.
   * @throws SQLException if a parameter cannot be bound.
   */
  private void bindParams(PreparedStatement stmt, Object... params) throws SQLException {
    for (int i = 0; i < params.length; i++) {
      stmt.setObject(i + 1, params[i]);
    }
  }

  /**
   * Checks if the connection pool is alive.
   *
   * <h4>Example</h4>
   * <pre>{@code
   * if (mgr.isAlive()) {
   *   // pool is active
   * }
   * }</pre>
   *
   * @return {@code true} if the pool is active and a connection can be obtained.
   */
  public boolean isAlive() {
    try (Connection conn = getConnection()) {
      boolean valid = conn.isValid(3);
      connected.set(valid);
      return valid;
    } catch (Exception e) {
      connected.set(false);
      return false;
    }
  }

  /**
   * Gracefully closes the HikariCP connection pool and releases all resources.
   */
  public void close() {
    connected.set(false);
    HikariDataSource ds = this.dataSource;
    if (ds != null && !ds.isClosed()) {
      try {
        ds.close();
      } catch (Exception e) {
        CobbleUtils.LOGGER_RAW.error("Error closing SQL connection pool: " + e.getMessage());
      }
    }
  }

  /**
   * Functional interface for mapping a {@link ResultSet} to a result type.
   *
   * @param <T> The return type.
   */
  @FunctionalInterface
  public interface ResultSetMapper<T> {
    /**
     * Maps the given ResultSet to a result.
     *
     * @param rs The ResultSet to map.
     * @return The mapped result.
     * @throws SQLException if an error occurs while reading the ResultSet.
     */
    T map(ResultSet rs) throws SQLException;
  }

  @FunctionalInterface
  public interface SQLConnectionFunction<T> {
    T apply(Connection connection) throws SQLException;
  }

  @FunctionalInterface
  public interface SQLConnectionConsumer {
    void accept(Connection connection) throws SQLException;
  }
}

