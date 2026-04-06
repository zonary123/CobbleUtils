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

  @Override
  public void connect(DataBaseConfig config) {
    this.type = config.getType();
    this.sqlManager = SQLService.getOrCreateManager(config);
    createTables();
  }

  @Override
  public void disconnect() {
    // SQLService manages the lifecycle — no-op
  }

  // ─── Schema ───────────────────────────────────────────────────────────────

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
    UserModel cached = DataBaseUsers.USERS.getIfPresent(uuid);
    if (cached != null) return cached;

    return sqlManager.query(
      "SELECT data FROM users WHERE playerUUID = ?",
      rs -> {
        if (!rs.next()) return null;
        UserModel user = UtilsFile.getGson().fromJson(rs.getString("data"), UserModel.class);
        if (user != null) user.fix();
        return user;
      },
      uuid.toString()
    );
  }

  @Override
  public @Nullable UserModel findUserByName(@NotNull String name) {
    UserModel cached = super.findUserByName(name);
    if (cached != null) return cached;

    return sqlManager.query(
      "SELECT data FROM users WHERE playerName = ?",
      rs -> rs.next()
        ? UtilsFile.getGson().fromJson(rs.getString("data"), UserModel.class)
        : null,
      name
    );
  }

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

  @Override
  public List<UserModel> getAllUsers() {
    return sqlManager.queryList(
      "SELECT data FROM users",
      rs -> UtilsFile.getGson().fromJson(rs.getString("data"), UserModel.class)
    );
  }

  @Override
  public List<UserModel> getUsersInactiveSince(long millis) {
    Instant threshold = Instant.now().minus(millis, ChronoUnit.MILLIS);
    String thresholdIso = DateTimeFormatter.ISO_INSTANT.format(threshold);
    return sqlManager.queryList(
      "SELECT data FROM users WHERE lastLogin IS NOT NULL AND lastLogin >= ?",
      rs -> UtilsFile.getGson().fromJson(rs.getString("data"), UserModel.class),
      thresholdIso
    );
  }

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
    DataBaseUsers.USERS.invalidate(playerUUID);
  }

  @Override
  public void addStorage(List<Storage> storage, UUID playerUUID) {
    if (storage == null || playerUUID == null) return;
    UserModel user = findUserByUUID(playerUUID);
    if (user == null) return;
    user.addStorage(storage);
    saveOrUpdateUser(user);
    DataBaseUsers.USERS.invalidate(playerUUID);
  }

  @Override
  public @Nullable Storage removeStorage(Storage storage, UUID playerUUID) {
    if (storage == null || playerUUID == null) return null;
    UserModel user = findUserByUUID(playerUUID);
    if (user == null) return null;
    Storage removed = user.removeStorage(storage.getId());
    if (removed != null) {
      saveOrUpdateUser(user);
      DataBaseUsers.USERS.invalidate(playerUUID);
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

