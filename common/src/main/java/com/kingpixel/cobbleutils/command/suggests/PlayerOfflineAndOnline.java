package com.kingpixel.cobbleutils.command.suggests;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.model.DurationValue;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.database.DataBaseFactory;
import com.kingpixel.cobbleutils.database.users.UserModel;
import com.kingpixel.cobbleutils.mixins.UserCacheMixin;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import lombok.Data;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.UserCache;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * @author Carlos Varas Alonso
 */
@Data
public class PlayerOfflineAndOnline {
  private long lastUpdate = 0;
  private final long updateInterval = TimeUnit.MINUTES.toMillis(5);
  private final List<String> playerNames = new CopyOnWriteArrayList<>();
  private final List<UUID> playerUUIDs = new CopyOnWriteArrayList<>();
  private final List<String> playerUUIDsString = new CopyOnWriteArrayList<>();

  private List<String> getPlayerUUIDsAsString() {
    refreshIfNeeded();
    if (playerUUIDsString.size() != playerUUIDs.size()) {
      playerUUIDsString.clear();
      for (UUID uuid : playerUUIDs) {
        playerUUIDsString.add(uuid.toString());
      }
    }
    return playerUUIDsString;
  }

  /**
   * Obtiene los nombres cacheados de los jugadores.
   */
  public List<String> getPlayerNames() {
    refreshIfNeeded();
    return playerNames;
  }

  /**
   * Obtiene los UUID cacheados de los jugadores.
   */
  public List<UUID> getPlayerUUIDs() {
    refreshIfNeeded();
    return playerUUIDs;
  }


  /**
   * Obtain the player UUIDs from the UserCache.
   */
  public Set<UUID> getPlayerUUIDsUserCache() {
    UserCache userCache = CobbleUtils.server.getUserCache();
    if (userCache == null) return Collections.emptySet();
    return ((UserCacheMixin) userCache).byUuid().keySet();
  }

  /**
   * Obtain the player names from the UserCache.
   */
  public Set<String> getPlayerNamesUserCache() {
    UserCache userCache = CobbleUtils.server.getUserCache();
    if (userCache == null) return Collections.emptySet();
    return ((UserCacheMixin) userCache).byName().keySet();
  }

  /**
   * Verifica si hace falta refrescar la lista de jugadores.
   */
  public void refreshIfNeeded() {
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastUpdate > updateInterval) {
      lastUpdate = currentTime;
      loadAsync();
    }
  }

  /**
   * Carga los jugadores inactivos de forma asíncrona.
   */
  private void loadAsync() {
    Instant now = Instant.now().plus(24, TimeUnit.DAYS.toChronoUnit());
    Instant from = now.minusMillis(DurationValue.parse("1y").toMillis());
    DataBaseFactory.dataBaseUsers.findUsersActiveBetween(from, now)
      .whenComplete((userModels, throwable) -> {
        if (userModels == null || userModels.isEmpty()) {
          CobbleUtils.LOGGER.info("No inactive users found to suggest.");
          return;
        }
        List<String> names = new ArrayList<>();
        List<UUID> uuids = new ArrayList<>();

        for (UserModel user : userModels) {
          names.add(user.getPlayerName());
          uuids.add(user.getPlayerUUID());
        }

        playerNames.clear();
        playerNames.addAll(names);

        playerUUIDs.clear();
        playerUUIDs.addAll(uuids);
      });
  }

  /**
   * Sugiere nombres de jugadores.
   */
  public RequiredArgumentBuilder<ServerCommandSource, String> suggestPlayerName(
    String literal, List<String> permissions, int level) {
    return CommandManager.argument(literal, StringArgumentType.string())
      .requires(source -> PermissionApi.hasPermission(source, permissions, level))
      .suggests((context, builder) -> CommandSource.suggestMatching(getPlayerNames(), builder));
  }

  /**
   * Sugiere UUIDs de jugadores.
   */
  public RequiredArgumentBuilder<ServerCommandSource, UUID> suggestPlayerUUID(
    String literal, List<String> permissions, int level) {
    return CommandManager.argument(literal, UuidArgumentType.uuid())
      .requires(source -> PermissionApi.hasPermission(source, permissions, level))
      .suggests((context, builder) -> CommandSource.suggestMatching(getPlayerUUIDsAsString(), builder));
  }

  /**
   * Resetea la caché.
   */
  public void reset() {
    lastUpdate = 0;
    playerNames.clear();
    playerUUIDs.clear();
  }

  /**
   * Obtiene un jugador (online u offline) a partir de un nombre desde el contexto.
   */
  public Optional<UserModel> getUserFromContext(CommandContext<ServerCommandSource> context, String argumentName) {
    String playerName = StringArgumentType.getString(context, argumentName);
    return getUserFromName(playerName);
  }

  /**
   * Obtiene un jugador offline desde su nombre (de caché o DB).
   */
  public Optional<UserModel> getUserFromName(String playerName) {
    try {
      UserModel user = DataBaseFactory.dataBaseUsers.findUserModel(playerName).join();
      return Optional.ofNullable(user);
    } catch (Exception ex) {
      CobbleUtils.LOGGER.error("Error fetching user by name: " + playerName);
      return Optional.empty();
    }
  }

  public Optional<DataResultPlayer> getPlayerFromContext(CommandContext<ServerCommandSource> context, String argumentName) {
    String playerName = StringArgumentType.getString(context, argumentName);
    return getPlayer(playerName);
  }

  /**
   * Obtiene el UUID de un jugador (online u offline) a partir de su nombre.
   */
  private static final UUID NIL_UUID = new UUID(0L, 0L);

  public @Nullable UUID getPlayerUUIDWithName(String playerName) {
    UserCache userCache = CobbleUtils.server.getUserCache();
    UUID uuid = null;

    if (userCache != null) {
      var optional = userCache.findByName(playerName);
      if (optional.isPresent()) {
        uuid = optional.get().getId();
      }
    }

    if (uuid != null && uuid.equals(NIL_UUID)) {
      uuid = null;
    }

    if (uuid == null) {
      var userModel = DataBaseFactory.dataBaseUsers.findUserModel(playerName).join();
      if (userModel != null) {
        uuid = userModel.getPlayerUUID();
      }
    }

    return uuid;
  }

  /**
   * Obtiene el nombre de un jugador (online u offline) a partir de su UUID.
   */
  public @Nullable String getPlayerNameWithUUID(UUID uuid) {
    UserCache userCache = CobbleUtils.server.getUserCache();
    String name;
    if (userCache != null) {
      var optional = userCache.getByUuid(uuid);
      name = optional.map(GameProfile::getName).orElse(null);
    } else {
      name = null;
    }
    if (name == null) {
      var userModel = DataBaseFactory.dataBaseUsers.findUserModel(uuid).join();
      name = userModel != null ? userModel.getPlayerName() : null;
    }
    return name;
  }

  /**
   * Obtiene un jugador online directamente desde un String.
   */
  public Optional<DataResultPlayer> getPlayer(String playerName) {
    var online = CobbleUtils.server.getPlayerManager().getPlayer(playerName);
    var userModel = DataBaseFactory.dataBaseUsers.findUserModel(playerName).join();
    ServerPlayerEntity offlineOrOnlineUser = getPlayerOfflineOrOnline(playerName);
    return Optional.ofNullable(offlineOrOnlineUser == null ? null : new DataResultPlayer(
        online != null,
        userModel,
        offlineOrOnlineUser
      )
    );
  }

  public Optional<DataResultPlayer> getPlayer(UUID playerUUID) {
    var online = CobbleUtils.server.getPlayerManager().getPlayer(playerUUID);
    var userModel = DataBaseFactory.dataBaseUsers.findUserModel(playerUUID).join();
    var offlineOrOnlineUser = getPlayerOfflineOrOnline(playerUUID);
    return Optional.ofNullable(offlineOrOnlineUser == null ? null : new DataResultPlayer(
        online != null,
        userModel,
        offlineOrOnlineUser
      )
    );
  }

  private ServerPlayerEntity getPlayerOfflineOrOnline(String playerName) {
    Optional<GameProfile> optionalGameProfile = CobbleUtils.server.getUserCache().findByName(playerName);
    if (optionalGameProfile.isEmpty()) {
      var userModel = DataBaseFactory.dataBaseUsers.findUserModel(playerName).join();
      if (userModel == null) return null;
      optionalGameProfile = CobbleUtils.server.getUserCache().getByUuid(userModel.getPlayerUUID());
    }
    return optionalGameProfile.map(gameProfile -> CobbleUtils.server.getPlayerManager().getPlayer(gameProfile.getId())).orElse(null);
  }

  private ServerPlayerEntity getPlayerOfflineOrOnline(@NotNull UUID playerUUID) {
    ServerPlayerEntity online = CobbleUtils.server.getPlayerManager().getPlayer(playerUUID);
    if (online != null) return online;

    String playerName = getPlayerNameWithUUID(playerUUID);
    if (playerName == null) return null;

    return CobbleUtils.server.getPlayerManager().getPlayer(playerName);
  }


  public record DataResultPlayer(boolean isOnline, UserModel user, ServerPlayerEntity player) {
  }
}
