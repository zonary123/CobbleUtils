package com.kingpixel.cobbleutils.database.users;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.api.RewardsApi;
import com.kingpixel.cobbleutils.database.users.models.Storage;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 27/08/2025 15:11
 */
public abstract class DataBaseUsers {
  public static final String FIELD_UUID = "playerUUID";
  public static final String FIELD_STORAGE = "storageList";
  public static final String FIELD_IS_ONLINE = "isOnline";

  public static final Cache<UUID, UserModel> USERS = Caffeine.newBuilder()
    .build();

  public abstract void connect(DataBaseConfig config);

  public abstract void disconnect();

  /**
   * Get a user from the cache.
   *
   * @param uuid The UUID of the player.
   * @return The user model or null if not found.
   */
  @Nullable
  public UserModel getUser(@NotNull UUID uuid) {
    return USERS.getIfPresent(uuid);
  }

  /**
   * Find a user, first in the cache and then in the database.
   * No automatic caching should occur here to avoid memory leaks.
   *
   * @param uuid The UUID of the player.
   * @return The user model or null if not found.
   */
  @Nullable
  public UserModel findUser(@NotNull UUID uuid) {
    UserModel user = getUser(uuid);
    if (user != null) return user;
    return findUserByUUID(uuid);
  }

  @Nullable
  public abstract UserModel findUserByUUID(@NotNull UUID uuid);

  @Nullable
  public UserModel findUserByName(@NotNull String name) {
    var userCache = CobbleUtils.server.getUserCache();
    if (userCache == null) return null;
    var gameProfile = userCache.findByName(name);
    return gameProfile.map(profile -> findUser(profile.getId())).orElse(null);
  }

  public abstract void saveOrUpdateUser(UserModel user);

  public abstract List<UserModel> getAllUsers();

  public abstract List<UserModel> getUsersInactiveSince(long millis);

  @Nullable
  public ServerPlayerEntity getPlayerOfflineOrOnline(String playerName) {
    ServerPlayerEntity player = CobbleUtils.server.getPlayerManager().getPlayer(playerName);
    if (player != null) return player;

    var user = findUserByName(playerName);
    if (user != null) {
      return getPlayerOfflineOrOnline(user.getPlayerUUID());
    }

    return null;
  }

  /**
   * Get a ServerPlayerEntity for a player by UUID, whether they are online or offline.
   *
   * @param playerUUID The UUID of the player.
   * @return The player entity or null if not found.
   */
  @Nullable
  public ServerPlayerEntity getPlayerOfflineOrOnline(UUID playerUUID) {
    var userCache = CobbleUtils.server.getUserCache();
    if (userCache == null) return null;
    var minecraftServer = CobbleUtils.server;
    if (minecraftServer == null) return null;
    var gameProfileOpt = userCache.getByUuid(playerUUID);
    if (gameProfileOpt.isEmpty()) return null;
    GameProfile requestedProfile = gameProfileOpt.get();
    ServerPlayerEntity requestedPlayer = minecraftServer.getPlayerManager().getPlayer(requestedProfile.getName());

    if (requestedPlayer != null) {
      return requestedPlayer;
    }

    requestedPlayer = new ServerPlayerEntity(
      minecraftServer,
      minecraftServer.getOverworld(),
      requestedProfile,
      SyncedClientOptions.createDefault()
    );

    var readViewOpt = minecraftServer.getPlayerManager()
      .loadPlayerData(requestedPlayer);

    if (readViewOpt.isPresent()) {
      requestedPlayer.readNbt(readViewOpt.get());
    }

    return requestedPlayer;
  }


  public boolean isAvailableReward(ServerPlayerEntity player, ItemChance itemChance) {
    return isAvailableReward(player.getUuid(), itemChance);
  }

  public boolean isAvailableReward(UUID playerUUID, ItemChance itemChance) {
    UserModel user = findUserByUUID(playerUUID);
    if (itemChance.getItem().startsWith("id:")) {
      ItemChance idChance = RewardsApi.getReward(itemChance.getItem().substring(3));
      if (idChance != null) {
        itemChance = idChance;
      } else {
        CobbleUtils.LOGGER_RAW.error("Reward with id " + itemChance.getItem() + " not found!");
        return false;
      }
    }
    return user != null && user.isAvailableReward(itemChance);
  }

  public void removeIfNecessary(UUID uuid) {
    USERS.invalidate(uuid);
  }

  public abstract void disconnected(ServerPlayerEntity player);

  public abstract void addStorage(Storage storage, UUID playerUUID);

  public abstract void addStorage(List<Storage> storage, UUID playerUUID);

  @Nullable
  public abstract Storage removeStorage(Storage storage, UUID playerUUID);

  public abstract List<UUID> getOnlinePlayers();
}
