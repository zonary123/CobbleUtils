package com.kingpixel.cobbleutils.database.users;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.database.users.models.Storage;
import com.kingpixel.cobbleutils.util.UtilsFile;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 27/08/2025 15:11
 */
public class DataBaseUsersJson extends DataBaseUsers {
  private static final Path USERS_FOLDER = CobbleUtils.getPathMod().resolve("users");

  /**
   * Initialize the JSON database connection.
   *
   * @param config The database configuration.
   */
  @Override
  public void connect(DataBaseConfig config) {
    CobbleUtils.LOGGER_RAW.info("DataBaseUsersJson connected");
  }

  /**
   * Close the JSON database connection.
   */
  @Override
  public void disconnect() {
    CobbleUtils.LOGGER_RAW.info("DataBaseUsersJson disconnected");
  }

  @Override
  public UserModel findUserByUUID(@NotNull UUID uuid) {
    Path file = USERS_FOLDER.resolve(uuid + ".json");
    if (!Files.exists(file))
      return null;
    return readUserFile(file);
  }

  /**
   * Find a user by name in the JSON database.
   *
   * @param name The name of the player.
   * @return The user model or null if not found.
   */
  @Override
  public UserModel findUserByName(@NotNull String name) {
    UserModel userModel = super.findUserByName(name);
    if (userModel != null)
      return userModel;
    List<Path> files = UtilsFile.getAllJsonFiles(USERS_FOLDER);
    return files.stream()
        .parallel()
        .map(this::readUserFile)
        .filter(user -> user != null && name.equalsIgnoreCase(user.getPlayerName()))
        .findFirst()
        .orElse(null);
  }

  /**
   * Save or update a user in the JSON database.
   *
   * @param user The user model to save.
   */
  @Override
  public void save(UserModel user) {
    if (user == null || user.getPlayerUUID() == null)
      return;
    try {
      Files.createDirectories(USERS_FOLDER);
      Path file = USERS_FOLDER.resolve(user.getPlayerUUID() + ".json");
      UtilsFile.write(file, user);
    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.error("Failed to save user file: {}", user.getPlayerUUID(), e);
    }
  }

  @Override
  public List<UserModel> getAllUsers() {
    List<Path> files = UtilsFile.getAllJsonFiles(USERS_FOLDER);
    return files.stream()
        .parallel()
        .map(this::readUserFile)
        .filter(Objects::nonNull)
        .toList();
  }

  @Override
  public List<UserModel> getUsersInactiveSince(long millis) {
    List<Path> files = UtilsFile.getAllJsonFiles(USERS_FOLDER);
    if (files.isEmpty())
      return List.of();

    long currentTime = System.currentTimeMillis();

    return files.stream()
        .parallel()
        .map(this::readUserFile)
        .filter(user -> isInactive(user, currentTime, millis))
        .toList();
  }

  @Override
  public void disconnected(ServerPlayerEntity player) {
    if (player == null)
      return;
    UserModel user = findUserByUUID(player.getUuid());
    if (user == null)
      return;
    user.disconnect();
    save(user);
  }

  @Override
  public void addStorage(Storage storage, UUID playerUUID) {
    if (storage == null || playerUUID == null)
      return;
    UserModel user = findUserByUUID(playerUUID);
    if (user == null)
      return;
    user.addStorage(storage);
    save(user);
    invalidateUser(playerUUID);
  }

  @Override
  public void addStorage(List<Storage> storage, UUID playerUUID) {
    if (storage == null || playerUUID == null)
      return;
    UserModel user = findUserByUUID(playerUUID);
    if (user == null)
      return;
    user.addStorage(storage);
    save(user);
    invalidateUser(playerUUID);
  }

  @Override
  public Storage removeStorage(Storage storage, UUID playerUUID) {
    if (storage == null || playerUUID == null)
      return null;
    UserModel user = findUserByUUID(playerUUID);
    if (user == null)
      return null;
    Storage removed = user.removeStorage(storage.getId());
    if (removed != null) {
      save(user);
      invalidateUser(playerUUID);
    }
    return removed;
  }

  @Override
  public List<UUID> getOnlinePlayers() {
    return CobbleUtils.server.getPlayerManager().getPlayerList().stream()
        .map(Entity::getUuid)
        .toList();
  }

  /**
   * Read a user file and return a UserModel.
   *
   * @param file The path to read.
   * @return The user model or null if an error occurred.
   */
  private UserModel readUserFile(Path file) {
    try {
      UserModel user = UtilsFile.read(file, UserModel.class);
      if (user != null)
        user.fix();
      return user;
    } catch (IOException e) {
      CobbleUtils.LOGGER_RAW.error("Failed to read user file: {}", file.getFileName(), e);
    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.error("Failed to parse user file: {}", file.getFileName(), e);
    }
    return null;
  }

  // Método que determina si un usuario es inactivo
  private boolean isInactive(UserModel user, long currentTime, long threshold) {
    if (user == null)
      return false;
    Instant lastLogin = user.getLastLogin();
    return lastLogin == null || (currentTime - lastLogin.toEpochMilli() >= threshold);
  }

}
