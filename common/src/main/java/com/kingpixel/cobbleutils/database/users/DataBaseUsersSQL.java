package com.kingpixel.cobbleutils.database.users;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.Model.DataBaseType;
import com.kingpixel.cobbleutils.database.users.models.Storage;
import com.kingpixel.cobbleutils.util.UtilsFile;
import com.kingpixel.cobbleutils.util.sql.SQLManager;
import com.kingpixel.cobbleutils.util.sql.SQLService;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * SQL-backed implementation of {@link DataBaseUsers}.
 * <p>
 * Supports MySQL, MariaDB, SQLite, and H2 through a shared {@link SQLManager}
 * obtained via {@link SQLService}. Stores the full {@link UserModel} as
 * serialized JSON in a {@code data} column for simplicity and consistency
 * with the MongoDB implementation.
 *
 * <h3>Table schema</h3>
 * <pre>{@code
 * CREATE TABLE IF NOT EXISTS users (
 *   playerUUID    VARCHAR(36)  PRIMARY KEY,
 *   playerName    VARCHAR(255),
 *   data          TEXT         NOT NULL,
 *   isOnline      TINYINT(1)   DEFAULT 0,
 *   lastLogin     VARCHAR(64),
 *   disconnectTime VARCHAR(64)
 * )
 * }</pre>
 *
 * @author Carlos Varas Alonso
 */
public class DataBaseUsersSQL extends DataBaseUsers {

  private SQLManager sqlManager;
  private DataBaseType type;
  private DataBaseConfig config;

  /**
   * Initialize the SQL database connection.
   *
   * @param config The database configuration.
   */
  @Override
  public void connect(DataBaseConfig config) {
    this.config = config;
    this.type = config.getType();
    this.sqlManager = SQLService.getOrCreateManager(config);
    createTables();
  }

  /**
   * Close the SQL database connection.
   */
  @Override
  public void disconnect() {
    if (config != null) {
      SQLService.releaseManager(config);
    }
    sqlManager = null;
    config = null;
  }

  /**
   * Creates the {@code users} table if it does not already exist.
   */
  private void createTables() {
    sqlManager.execute("""
      CREATE TABLE IF NOT EXISTS users (
        playerUUID     VARCHAR(36)  PRIMARY KEY,
        playerName     VARCHAR(255),
        data           TEXT         NOT NULL,
        isOnline       TINYINT(1)   DEFAULT 0,
        lastLogin      VARCHAR(64),
        disconnectTime VARCHAR(64)
      )
      """);
  }

  // ─── CRUD ─────────────────────────────────────────────────────────────────

  @Override
  public @Nullable UserModel findUserByUUID(@NotNull UUID uuid) {
    return sqlManager.query(
      "SELECT data FROM users WHERE playerUUID = ?",
      rs -> {
        if (!rs.next()) return null;
        UserModel user = UtilsFile.getGson().fromJson(rs.getString("data"), UserModel.class);
        if (user != null) {
          user.fix();
        }
        return user;
      },
      uuid.toString()
    );
  }

  public CompletableFuture<UserModel> findUserByUUIDAsync(@NotNull UUID uuid) {
    return sqlManager.queryAsync(
      "SELECT data FROM users WHERE playerUUID = ?",
      rs -> {
        if (!rs.next()) return null;
        UserModel user = UtilsFile.getGson().fromJson(rs.getString("data"), UserModel.class);
        if (user != null) {
          user.fix();
        }
        return user;
      },
      uuid.toString()
    );
  }

  /**
   * Find a user by name in the SQL database.
   *
   * @param name The name of the player.
   * @return The user model or null if not found.
   */
  @Override
  public @Nullable UserModel findUserByName(@NotNull String name) {
    UserModel cached = super.findUserByName(name);
    if (cached != null) return cached;

    return sqlManager.query(
      "SELECT data FROM users WHERE playerName = ?",
      rs -> {
        if (!rs.next()) return null;
        UserModel user = UtilsFile.getGson().fromJson(rs.getString("data"), UserModel.class);
        if (user != null) user.fix();
        return user;
      },
      name
    );
  }

  /**
   * Save or update a user in the SQL database.
   *
   * @param user The user model to save.
   */
  @Override
  public void saveOrUpdateUser(UserModel user) {
    if (user == null || user.getPlayerUUID() == null) return;

    String data = UtilsFile.getGson().toJson(user, UserModel.class);
    String lastLogin = user.getLastLogin() != null
      ? DateTimeFormatter.ISO_INSTANT.format(user.getLastLogin())
      : null;

    String sql = buildUpsertSql();
    sqlManager.execute(sql,
      user.getPlayerUUID().toString(),
      user.getPlayerName(),
      data,
      user.isOnline() ? 1 : 0,
      lastLogin
    );
  }

  public CompletableFuture<Void> saveOrUpdateUserAsync(UserModel user) {
    if (user == null || user.getPlayerUUID() == null) {
      return CompletableFuture.completedFuture(null);
    }

    String data = UtilsFile.getGson().toJson(user, UserModel.class);
    String lastLogin = user.getLastLogin() != null
      ? DateTimeFormatter.ISO_INSTANT.format(user.getLastLogin())
      : null;

    String sql = buildUpsertSql();
    return sqlManager.executeAsync(
      sql,
      user.getPlayerUUID().toString(),
      user.getPlayerName(),
      data,
      user.isOnline() ? 1 : 0,
      lastLogin
    ).thenAccept(ignored -> {
    });
  }

  /**
   * Builds the upsert SQL based on the database type.
   * SQLite uses {@code INSERT OR REPLACE}, MySQL/MariaDB/H2 use {@code ON DUPLICATE KEY UPDATE}.
   */
  private String buildUpsertSql() {
    if (type == DataBaseType.SQLITE) {
      return "INSERT OR REPLACE INTO users (playerUUID, playerName, data, isOnline, lastLogin) VALUES (?, ?, ?, ?, ?)";
    }
    return "INSERT INTO users (playerUUID, playerName, data, isOnline, lastLogin) VALUES (?, ?, ?, ?, ?)" +
      " ON DUPLICATE KEY UPDATE playerName = VALUES(playerName), data = VALUES(data)," +
      " isOnline = VALUES(isOnline), lastLogin = VALUES(lastLogin)";
  }

  /**
   * Get all users in the SQL database.
   *
   * @return A list of all user models.
   */
  @Override
  public List<UserModel> getAllUsers() {
    return sqlManager.queryList(
      "SELECT data FROM users",
      rs -> {
        UserModel user = UtilsFile.getGson().fromJson(rs.getString("data"), UserModel.class);
        if (user != null) user.fix();
        return user;
      }
    );
  }

  /**
   * Get users who have been inactive since a certain duration.
   *
   * @param millis The duration in milliseconds.
   * @return A list of inactive user models.
   */
  @Override
  public List<UserModel> getUsersInactiveSince(long millis) {
    Instant threshold = Instant.now().minus(millis, ChronoUnit.MILLIS);
    String thresholdIso = DateTimeFormatter.ISO_INSTANT.format(threshold);
    return sqlManager.queryList(
      "SELECT data FROM users WHERE lastLogin IS NOT NULL AND lastLogin <= ?",
      rs -> {
        UserModel user = UtilsFile.getGson().fromJson(rs.getString("data"), UserModel.class);
        if (user != null) user.fix();
        return user;
      },
      thresholdIso
    );
  }

  /**
   * Update the user state for disconnection in the SQL database.
   *
   * @param player The online player entity.
   */
  @Override
  public void disconnected(ServerPlayerEntity player) {
    if (player == null) return;
    String now = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    sqlManager.execute(
      "UPDATE users SET isOnline = 0, disconnectTime = ? WHERE playerUUID = ?",
      now,
      player.getUuid().toString()
    );
  }

  // ─── Storage ──────────────────────────────────────────────────────────────

  @Override
  public void addStorage(Storage storage, UUID playerUUID) {
    if (storage == null || playerUUID == null) return;
    UserModel user = findUserByUUID(playerUUID);
    if (user == null) return;
    user.addStorage(storage);
    saveOrUpdateUser(user);
    invalidateUser(playerUUID);
  }

  public CompletableFuture<Void> addStorageAsync(Storage storage, UUID playerUUID) {
    if (storage == null || playerUUID == null) {
      return CompletableFuture.completedFuture(null);
    }
    return findUserByUUIDAsync(playerUUID).thenCompose(user -> {
      if (user == null) {
        return CompletableFuture.completedFuture(null);
      }
      user.addStorage(storage);
      return saveOrUpdateUserAsync(user).thenRun(() -> invalidateUser(playerUUID));
    });
  }

  @Override
  public void addStorage(List<Storage> storage, UUID playerUUID) {
    if (storage == null || playerUUID == null) return;
    UserModel user = findUserByUUID(playerUUID);
    if (user == null) return;
    user.addStorage(storage);
    saveOrUpdateUser(user);
    invalidateUser(playerUUID);
  }

  @Override
  public @Nullable Storage removeStorage(Storage storage, UUID playerUUID) {
    if (storage == null || playerUUID == null) return null;
    UserModel user = findUserByUUID(playerUUID);
    if (user == null) return null;
    Storage removed = user.removeStorage(storage.getId());
    if (removed != null) {
      saveOrUpdateUser(user);
      invalidateUser(playerUUID);
    }
    return removed;
  }

  // ─── Online players ───────────────────────────────────────────────────────

  @Override
  public List<UUID> getOnlinePlayers() {
    return sqlManager.queryList(
      "SELECT playerUUID FROM users WHERE isOnline = 1",
      rs -> {
        try {
          return UUID.fromString(rs.getString("playerUUID"));
        } catch (Exception e) {
          CobbleUtils.LOGGER_RAW.error("Failed to parse online player UUID: " + e.getMessage());
          return null;
        }
      }
    ).stream().filter(Objects::nonNull).toList();
  }
}

