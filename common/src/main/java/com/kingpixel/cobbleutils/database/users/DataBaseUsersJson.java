package com.kingpixel.cobbleutils.database.users;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.database.users.models.Storage;
import com.kingpixel.cobbleutils.util.UtilsFile;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 27/08/2025 15:11
 */
public class DataBaseUsersJson extends DataBaseUsers {
  private static final String PATH_USERS = CobbleUtils.PATH + "/users/";

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
    File file = new File(PATH_USERS + uuid + ".json");
    if (!file.exists())
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
      return userModel; // si está en la cache,
    File folder = new File(PATH_USERS);
    File[] files = folder.listFiles();
    if (files == null)
      return null;
    return Arrays.stream(files)
        .parallel() // procesa en paralelo;
        .map(file -> {
          UserModel user = readUserFile(file);
          if (user != null && name.equalsIgnoreCase(user.getPlayerName()))
            return user;
          return null;
        })
        .filter(Objects::nonNull) // elimina los nulls
        .findFirst()
        .orElse(null);
  }

  /**
   * Save or update a user in the JSON database.
   *
   * @param user The user model to save.
   */
  @Override
  public void saveOrUpdateUser(UserModel user) {
    if (user == null || user.getPlayerUUID() == null)
      return;
    File folder = new File(PATH_USERS);
    if (!folder.exists())
      folder.mkdirs();
    File file = new File(PATH_USERS + user.getPlayerUUID() + ".json");
    UtilsFile.writeAsync(file.toPath(), user);
  }

  @Override
  public List<UserModel> getAllUsers() {
    File folder = new File(PATH_USERS);
    File[] files = folder.listFiles();
    if (files == null)
      return List.of();
    return Arrays.stream(files)
        .parallel() // procesa en paralelo;
        .map(this::readUserFile)
        .filter(Objects::nonNull) // elimina los nulls
        .toList();
  }

  @Override
  public List<UserModel> getUsersInactiveSince(long millis) {
    File folder = new File(PATH_USERS);
    File[] files = folder.listFiles((dir, name) -> name.endsWith(".json")); // solo JSON
    if (files == null || files.length == 0)
      return List.of();

    long currentTime = System.currentTimeMillis();

    return Arrays.stream(files)
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
    saveOrUpdateUser(user);
  }

  @Override
  public void addStorage(Storage storage, UUID playerUUID) {
    if (storage == null || playerUUID == null)
      return;
    UserModel user = findUserByUUID(playerUUID);
    if (user == null)
      return;
    user.addStorage(storage);
  }

  @Override
  public void addStorage(List<Storage> storage, UUID playerUUID) {
    if (storage == null || playerUUID == null)
      return;
    UserModel user = findUserByUUID(playerUUID);
    if (user == null)
      return;
    user.addStorage(storage);
  }

  @Override
  public Storage removeStorage(Storage storage, UUID playerUUID) {
    if (storage == null || playerUUID == null)
      return null;
    UserModel user = findUserByUUID(playerUUID);
    if (user == null)
      return null;
    return user.removeStorage(storage.getId());
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
   * @param file The file to read.
   * @return The user model or null if an error occurred.
   */
  private UserModel readUserFile(File file) {
    try {
      UserModel user = UtilsFile.read(file.toPath(), UserModel.class);
      if (user != null)
        user.fix();
      return user;
    } catch (IOException e) {
      CobbleUtils.LOGGER_RAW.error("Failed to read user file: " + file.getName(), e);
    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.error("Failed to parse user file: " + file.getName(), e);
    }
    return null;
  }

  // Método que determina si un usuario es inactivo
  private boolean isInactive(UserModel user, long currentTime, long threshold) {
    if (user == null)
      return false;
    Instant lastLogin = user.getLastLogin();
    return lastLogin == null || (currentTime - lastLogin.toEpochMilli() <= threshold);
  }

}
