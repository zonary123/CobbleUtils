package com.kingpixel.cobbleutils.database.users;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.JsonObject;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.api.RewardsApi;
import com.kingpixel.cobbleutils.database.users.models.Storage;
import com.kingpixel.cobbleutils.util.redis.RedisManager;
import com.kingpixel.cobbleutils.util.redis.handlers.RedisUserCacheHandler;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Carlos Varas Alonso - 27/08/2025 15:11
 */
public abstract class DataBaseUsers {
  public static final String FIELD_UUID = "playerUUID";
  public static final String FIELD_STORAGE = "storageList";
  public static final String FIELD_IS_ONLINE = "isOnline";

  public static final Cache<UUID, UserModel> USERS = Caffeine.newBuilder()
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .maximumSize(500)
    .build();

  public abstract void connect(DataBaseConfig config);

  public abstract void disconnect();

  /**
   * Unique instance ID for this server process. Used to identify the origin
   * of Redis messages so a server ignores its own broadcasts.
   * Unlike config.getServer(), this is always unique even if admins forget
   * to change the default server name.
   */
  public static final String INSTANCE_ID = UUID.randomUUID().toString();

  public void invalidateUser(@NotNull UUID uuid) {
    USERS.invalidate(uuid);
    publishUserInvalidation(uuid);
  }

  private void publishUserInvalidation(@NotNull UUID uuid) {
    try {
      if (!CobbleUtils.config.isRedisMessaging()) return;
      RedisManager mgr = CobbleUtils.redisManager;
      if (mgr == null || !mgr.getConnected().get()) return;

      JsonObject json = new JsonObject();
      json.addProperty("action", "invalidate");
      json.addProperty("uuid", uuid.toString());
      json.addProperty("origin", INSTANCE_ID);
      mgr.publish(RedisUserCacheHandler.IDENTIFIER, json);
    } catch (Exception e) {
      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER_RAW.error("Failed to publish user invalidation for: {}", uuid, e);
      }
    }
  }

  @Nullable
  public UserModel getUser(@NotNull UUID uuid) {
    return USERS.getIfPresent(uuid);
  }

  @Nullable
  public UserModel findUser(@NotNull UUID uuid) {
    UserModel user = getUser(uuid);
    if (user != null) return user;
    user = findUserByUUID(uuid);
    if (user != null) {
      USERS.put(uuid, user);
    }
    return user;
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

  /**
   * Saves the user only if it has been modified (dirty flag).
   * Uses atomic compare-and-set to prevent double-save races between threads.
   *
   * @return true if the user was saved, false if skipped (not dirty or already being saved).
   */
  public boolean saveIfDirty(UserModel user) {
    if (user == null) return false;
    if (!user.clearDirty()) return false;
    saveOrUpdateUser(user);
    return true;
  }

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
    UserModel user = findUser(playerUUID);
    if (itemChance.getItem().startsWith("id:")) {
      ItemChance idChance = RewardsApi.getReward(itemChance.getItem().substring(3));
      if (idChance != null) {
        itemChance = idChance;
      } else {
        CobbleUtils.LOGGER_RAW.error("Reward with id {} not found!", itemChance.getItem());
        return false;
      }
    }
    return user != null && user.isAvailableReward(itemChance);
  }

  public void claimReward(ServerPlayerEntity player, ItemChance itemChance) {
    claimReward(player.getUuid(), itemChance);
  }

  public void claimReward(UUID playerUUID, ItemChance itemChance) {
    UserModel user = findUser(playerUUID);
    if (user == null) return;
    if (itemChance.getItem().startsWith("id:")) {
      ItemChance idChance = RewardsApi.getReward(itemChance.getItem().substring(3));
      if (idChance != null) {
        itemChance = idChance;
      }
    }
    user.claimReward(itemChance);
    saveIfDirty(user);
    invalidateUser(playerUUID);
  }

  /**
   * Batch claim — single DB save + single Redis invalidation for multiple rewards.
   */
  public void claimRewardsBatch(UUID playerUUID, List<ItemChance> rewards) {
    if (rewards == null || rewards.isEmpty()) return;
    UserModel user = findUser(playerUUID);
    if (user == null) {
      user = new UserModel(playerUUID);
    }
    for (ItemChance reward : rewards) {
      if (reward == null) continue;
      ItemChance resolved = reward;
      if (resolved.getItem().startsWith("id:")) {
        ItemChance idChance = RewardsApi.getReward(resolved.getItem().substring(3));
        if (idChance != null) resolved = idChance;
      }
      user.claimReward(resolved);
    }
    saveIfDirty(user);
    invalidateUser(playerUUID);
  }

  /**
   * Combined claim + storage add — single user fetch, single save, single invalidation.
   * Used for offline players where rewards go to storage.
   */
  public void claimRewardsAndAddStorage(UUID playerUUID, List<ItemChance> rewards, List<Storage> storages) {
    if (playerUUID == null) return;
    UserModel user = findUser(playerUUID);
    if (user == null) {
      user = new UserModel(playerUUID);
    }
    if (rewards != null) {
      for (ItemChance reward : rewards) {
        if (reward == null) continue;
        ItemChance resolved = reward;
        if (resolved.getItem().startsWith("id:")) {
          ItemChance idChance = RewardsApi.getReward(resolved.getItem().substring(3));
          if (idChance != null) resolved = idChance;
        }
        user.claimReward(resolved);
      }
    }
    if (storages != null) {
      user.addStorage(storages);
    }
    saveIfDirty(user);
    invalidateUser(playerUUID);
  }

  public void removeIfNecessary(UUID uuid) {
    invalidateUser(uuid);
  }

  public abstract void disconnected(ServerPlayerEntity player);

  public abstract void addStorage(Storage storage, UUID playerUUID);

  public abstract void addStorage(List<Storage> storage, UUID playerUUID);

  @Nullable
  public abstract Storage removeStorage(Storage storage, UUID playerUUID);

  /**
   * Batch remove — single DB operation + single invalidation for multiple storages.
   * Subclasses should override for more efficient implementations.
   */
  public void removeStorageBatch(List<Storage> storages, UUID playerUUID) {
    if (storages == null || storages.isEmpty() || playerUUID == null) return;
    UserModel user = findUser(playerUUID);
    if (user == null) return;
    for (Storage storage : storages) {
      user.removeStorage(storage.getId());
    }
    saveIfDirty(user);
    invalidateUser(playerUUID);
  }

  public abstract List<UUID> getOnlinePlayers();
}
