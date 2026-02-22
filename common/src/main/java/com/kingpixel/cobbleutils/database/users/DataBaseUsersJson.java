package com.kingpixel.cobbleutils.database.users;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.Model.rewards.Reward;
import com.kingpixel.cobbleutils.database.users.models.Storage;
import com.kingpixel.cobbleutils.util.UtilsFile;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class DataBaseUsersJson extends DataBaseUsers {

  private static final Path PATH = CobbleUtils.getPathMod().resolve("users");

  @Override
  public void connect(DataBaseConfig config) {
    CobbleUtils.LOGGER.info("DataBaseUsersJson connected");
  }

  @Override
  public void disconnect() {
    saveAll().join();
    CobbleUtils.LOGGER.info("DataBaseUsersJson disconnected");
  }

  private Path getUserPath(UUID uuid) {
    return PATH.resolve(uuid + ".json");
  }

  private @Nullable UserModel loadFromFile(UUID uuid) {
    try {
      return UtilsFile.read(getUserPath(uuid), UserModel.class);
    } catch (Exception e) {
      return null;
    }
  }

  private @Nullable UserModel getOrLoad(UUID uuid) {
    UserModel cached = DataBaseUsers.USERS.getIfPresent(uuid);
    if (cached != null) return cached;
    return loadFromFile(uuid);
  }

  @Override
  public CompletableFuture<@Nullable UserModel> findUserModel(UUID uuid) {
    return CobbleUtils.ASYNC.supply(() -> getOrLoad(uuid));
  }

  @Override
  public CompletableFuture<@Nullable UserModel> findUserModel(String username) {
    return CobbleUtils.ASYNC.supply(() -> {
        var cache = CobbleUtils.server.getUserCache();
        if (cache == null) return null;
        return cache.findByName(username)
          .map(entry -> entry.getId())
          .orElse(null);
      })
      .thenCompose(uuid -> uuid == null
        ? CompletableFuture.completedFuture(null)
        : findUserModel(uuid));
  }

  @Override
  public CompletableFuture<Set<Storage>> findUserStorage(UUID uuid) {
    return findUserModel(uuid)
      .thenApply(model -> model != null ? model.getStorageList() : Collections.emptySet());
  }

  @Override
  public boolean isAvailableReward(ServerPlayerEntity player, ItemChance itemChance) {
    UserModel model = getUserModel(player);
    return model != null && model.isAvailableReward(itemChance);
  }

  @Override
  public CompletableFuture<Boolean> isAvailableReward(UUID playerUUID, Reward reward) {
    if (!Boolean.TRUE.equals(reward.getUnique()) || reward.getIdentifier() == null)
      return CompletableFuture.completedFuture(true);

    UserModel cached = DataBaseUsers.USERS.getIfPresent(playerUUID);
    if (cached != null)
      return CompletableFuture.completedFuture(cached.isAvailableReward(reward));

    return findUserModel(playerUUID).thenApply(model -> {
      if (model == null) return false;
      boolean result = model.isAvailableReward(reward);
      if (result) model.save();
      return result;
    });
  }

  @Override
  public CompletableFuture<Void> saveUserModel(UserModel userModel) {
    return CobbleUtils.ASYNC.runAsync(() -> {
      try {
        UtilsFile.write(getUserPath(userModel.getPlayerUUID()), userModel);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  private CompletableFuture<Boolean> modifyStorage(UUID uuid, Consumer<UserModel> action) {
    return CobbleUtils.ASYNC.supply(() -> {
      UserModel model = getOrLoad(uuid);
      if (model == null) return false;
      action.accept(model);
      model.save();
      return true;
    });
  }

  @Override
  public CompletableFuture<Boolean> addStorage(Storage storage, UUID targetUUID) {
    return modifyStorage(targetUUID, model -> model.addStorage(storage));
  }

  @Override
  public CompletableFuture<Boolean> addStorage(List<Storage> storage, UUID targetUUID) {
    return modifyStorage(targetUUID, model -> model.addStorage(storage));
  }

  @Override
  public CompletableFuture<Boolean> removeStorage(Storage storage, UUID targetUUID) {
    return modifyStorage(targetUUID, model -> model.removeStorage(storage.getId()));
  }

  @Override
  public CompletableFuture<Boolean> removeStorage(List<Storage> storage, UUID targetUUID) {
    return modifyStorage(targetUUID, model -> {
      for (Storage s : storage) {
        model.removeStorage(s.getId());
      }
    });
  }

  @Override
  public CompletableFuture<List<UserModel>> findUsersActiveBetween(Instant from, Instant to) {
    return CobbleUtils.ASYNC.supply(() -> USERS.asMap().values().stream()
      .filter(user ->
        user.getLastLogin() != null &&
          !user.getLastLogin().isBefore(from) &&
          !user.getLastLogin().isAfter(to))
      .toList());
  }

  @Override
  public CompletableFuture<List<UUID>> getOnlinePlayers() {
    return CobbleUtils.ASYNC.supply(() ->
      USERS.asMap().values().stream()
        .map(UserModel::getPlayerUUID)
        .toList()
    );
  }
}