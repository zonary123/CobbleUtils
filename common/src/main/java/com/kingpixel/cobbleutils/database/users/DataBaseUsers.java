package com.kingpixel.cobbleutils.database.users;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.Model.rewards.Reward;
import com.kingpixel.cobbleutils.database.users.models.Storage;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 27/08/2025 15:11
 */
public abstract class DataBaseUsers {
  public static final Cache<UUID, UserModel> USERS = Caffeine.newBuilder()
    .build();

  public abstract void connect(DataBaseConfig config);

  public abstract void disconnect();

  public @Nullable UserModel getUserModel(ServerPlayerEntity player) {
    return getUserModel(player.getUuid());
  }

  public @Nullable UserModel getUserModel(UUID uuid) {
    return USERS.getIfPresent(uuid);
  }

  public abstract CompletableFuture<@Nullable UserModel> findUserModel(UUID uuid);

  public abstract CompletableFuture<@Nullable UserModel> findUserModel(String username);

  public abstract CompletableFuture<@Nullable Set<Storage>> findUserStorage(UUID uuid);

  public abstract CompletableFuture<Void> saveUserModel(UserModel userModel);

  public CompletableFuture<Void> saveAll() {
    var users = DataBaseUsers.USERS.asMap().values();
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    for (UserModel user : users) {
      futures.add(user.save());
    }
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
  }

  public abstract boolean isAvailableReward(ServerPlayerEntity player, ItemChance itemChance);

  public abstract CompletableFuture<Boolean> isAvailableReward(UUID playerUUID, Reward reward);


  public abstract CompletableFuture<Boolean> addStorage(Storage storage, UUID targetUUID);

  public abstract CompletableFuture<Boolean> addStorage(List<Storage> storage, UUID targetUUID);

  public abstract CompletableFuture<Boolean> removeStorage(Storage storage, UUID targetUUID);

  public abstract CompletableFuture<Boolean> removeStorage(List<Storage> storage, UUID targetUUID);

  public abstract CompletableFuture<List<UserModel>> findUsersActiveBetween(Instant from, Instant to);

  public abstract CompletableFuture<List<UUID>> getOnlinePlayers();
}
