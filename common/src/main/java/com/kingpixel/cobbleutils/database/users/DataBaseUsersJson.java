package com.kingpixel.cobbleutils.database.users;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.database.users.models.Storage;
import com.kingpixel.cobbleutils.util.Utils;
import net.minecraft.entity.Entity;
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

  @Override
  public void connect(DataBaseConfig config) {
    CobbleUtils.LOGGER.info("DataBaseUsersJson connected");
  }

  @Override
  public void disconnect() {
    CobbleUtils.LOGGER.info("DataBaseUsersJson disconnected");
  }

  @Override
  public UserModel findUserByUUID(@NotNull UUID uuid) {
    UserModel userModel = super.findUserByUUID(uuid);
    if (userModel != null) return userModel; // si está en la cache,
    // si no está en la cache, lo carga del archivo
    File file = Utils.getAbsolutePath(PATH_USERS + uuid + ".json");
    if (!file.exists()) return null;
    userModel = readUserFile(file);
    return userModel;
  }

  @Override
  public UserModel findUserByName(@NotNull String name) {
    UserModel userModel = super.findUserByName(name);
    if (userModel != null) return userModel; // si está en la cache,
    File folder = Utils.getAbsolutePath(PATH_USERS);
    File[] files = folder.listFiles();
    if (files == null) return null;
    return Arrays.stream(files)
      .parallel() // procesa en paralelo;
      .map(file -> {
        UserModel user = readUserFile(file);
        if (user != null && name.equalsIgnoreCase(user.getPlayerName())) return user;
        return null;
      })
      .filter(Objects::nonNull) // elimina los nulls
      .findFirst()
      .orElse(null);
  }

  @Override
  public void saveOrUpdateUser(UserModel user) {
    if (user == null || user.getPlayerUUID() == null) return;
    File folder = Utils.getAbsolutePath(PATH_USERS);
    if (!folder.exists()) folder.mkdirs();
    File file = Utils.getAbsolutePath(PATH_USERS + user.getPlayerUUID() + ".json");
    String data = Utils.newWithoutSpacingGson().toJson(user);
    Utils.writeFileAsync(file, data);
  }

  @Override
  public List<UserModel> getAllUsers() {
    File folder = Utils.getAbsolutePath(PATH_USERS);
    File[] files = folder.listFiles();
    if (files == null) return List.of();
    return Arrays.stream(files)
      .parallel() // procesa en paralelo;
      .map(this::readUserFile)
      .filter(Objects::nonNull) // elimina los nulls
      .toList();
  }

  @Override
  public List<UserModel> getUsersInactiveSince(long millis) {
    File folder = Utils.getAbsolutePath(PATH_USERS);
    File[] files = folder.listFiles((dir, name) -> name.endsWith(".json")); // solo JSON
    if (files == null || files.length == 0) return List.of();

    long currentTime = System.currentTimeMillis();

    return Arrays.stream(files)
      .parallel()
      .map(this::readUserFile)
      .filter(user -> isInactive(user, currentTime, millis))
      .toList();
  }

  @Override
  public void addStorage(Storage storage, UUID playerUUID) {
    if (storage == null || playerUUID == null) return;
    UserModel user = findUserByUUID(playerUUID);
    if (user == null) return;
    user.addStorage(storage);
  }

  @Override
  public Storage removeStorage(Storage storage, UUID playerUUID) {
    if (storage == null || playerUUID == null) return null;
    UserModel user = findUserByUUID(playerUUID);
    if (user == null) return null;
    return user.removeStorage(storage.getId());
  }

  @Override public List<UUID> getOnlinePlayers() {
    return CobbleUtils.server.getPlayerManager().getPlayerList().stream()
      .map(Entity::getUuid)
      .toList();
  }

  // Método para leer un archivo JSON y convertirlo en UserModel
  private UserModel readUserFile(File file) {
    try {
      String data = Utils.readFileSync(file);
      return Utils.newWithoutSpacingGson().fromJson(data, UserModel.class);
    } catch (IOException e) {
      e.printStackTrace(); // debug de lectura
    } catch (Exception e) {
      System.err.println("Failed to parse user file: " + file.getName());
      e.printStackTrace();
    }
    return null;
  }

  // Método que determina si un usuario es inactivo
  private boolean isInactive(UserModel user, long currentTime, long threshold) {
    if (user == null) return false;
    Instant lastLogin = user.getLastLogin();
    return lastLogin == null || (currentTime - lastLogin.toEpochMilli() <= threshold);
  }


}
