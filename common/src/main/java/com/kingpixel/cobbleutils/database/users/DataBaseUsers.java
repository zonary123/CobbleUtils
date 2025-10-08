package com.kingpixel.cobbleutils.database.users;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 27/08/2025 15:11
 */
public abstract class DataBaseUsers {
  public static final Map<UUID, UserModel> users = new HashMap<>();

  public abstract void connect(DataBaseConfig config);

  public abstract void disconnect();

  @Nullable
  public UserModel findUserByUUID(@NotNull UUID uuid) {
    return users.get(uuid);
  }

  @Nullable
  public UserModel findUserByName(@NotNull String name) {
    var userCache = CobbleUtils.server.getUserCache();
    if (userCache == null) return null;
    var gameProfile = userCache.findByName(name);
    return gameProfile.map(profile -> findUserByUUID(profile.getId())).orElse(null);
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

  @Nullable
  public ServerPlayerEntity getPlayerOfflineOrOnline(UUID playerUUID) {
    // Obtener perfil del jugador del argumento del comando
    var userCache = CobbleUtils.server.getUserCache();
    if (userCache == null) return null;
    var minecraftServer = CobbleUtils.server;
    if (minecraftServer == null) return null;
    var gameProfileOpt = userCache.getByUuid(playerUUID);
    if (gameProfileOpt.isEmpty()) return null;
    GameProfile requestedProfile = gameProfileOpt.get();
    ServerPlayerEntity requestedPlayer = minecraftServer.getPlayerManager().getPlayer(requestedProfile.getName());

    // Si el jugador está online
    if (requestedPlayer != null) {
      return requestedPlayer;
    }

    // Crear jugador temporal
    requestedPlayer = new ServerPlayerEntity(
      minecraftServer,
      minecraftServer.getOverworld(),
      requestedProfile,
      SyncedClientOptions.createDefault()
    );

    // Intentar cargar datos del jugador offline
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
        CobbleUtils.LOGGER.error("Reward with id " + itemChance.getItem() + " not found!");
        return false;
      }
    }
    return user != null && user.isAvailableReward(itemChance);
  }

  public void removeIfNecessary(UUID uuid) {
    users.remove(uuid);
  }

  public abstract void addStorage(Storage storage, UUID playerUUID);

  @Nullable
  public abstract Storage removeStorage(Storage storage, UUID playerUUID);
}
