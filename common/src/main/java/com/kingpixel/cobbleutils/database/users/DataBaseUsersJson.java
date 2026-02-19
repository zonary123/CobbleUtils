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
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 27/08/2025 15:11
 */
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

  @Override
  public CompletableFuture<@Nullable UserModel> findUserModel(UUID uuid) {
    return CobbleUtils.ASYNC.supply(() -> {
      UserModel userModel = DataBaseUsers.USERS.getIfPresent(uuid);
      if (userModel != null) return userModel;
      try {
        userModel = UtilsFile.read(PATH, UserModel.class);
      } catch (Exception e) {
        return null;
      }
      return userModel;
    });
  }

  @Override
  public CompletableFuture<@Nullable UserModel> findUserModel(String username) {
    return CobbleUtils.ASYNC.supply(() -> {
      var userCache = CobbleUtils.server.getUserCache();
      if (userCache == null) return null;
      var optional = userCache.findByName(username);
      if (optional.isEmpty()) return null;
      UUID uuid = optional.get().getId();
      UserModel userModel = DataBaseUsers.USERS.getIfPresent(uuid);
      if (userModel != null) return userModel;
      try {
        userModel = UtilsFile.read(PATH, UserModel.class);
      } catch (Exception e) {
        return null;
      }
      return userModel;
    });
  }

  @Override
  public CompletableFuture<Set<Storage>> findUserStorage(UUID uuid) {
    return CobbleUtils.ASYNC.supply(() -> {
      UserModel userModel = DataBaseUsers.USERS.getIfPresent(uuid);
      if (userModel != null) return userModel.getStorageList();
      try {
        userModel = UtilsFile.read(PATH, UserModel.class);
      } catch (Exception e) {
        return Collections.emptySet();
      }
      if (userModel == null) return Collections.emptySet();
      return userModel.getStorageList();
    });
  }

  @Override
  public boolean isAvailableReward(ServerPlayerEntity player, ItemChance itemChance) {
    var userModel = getUserModel(player);
    if (userModel == null) return false;
    return userModel.isAvailableReward(itemChance);
  }

  @Override
  public CompletableFuture<Boolean> isAvailableReward(UUID playerUUID, Reward reward) {
    return null;
  }

  @Override
  public CompletableFuture<Void> saveUserModel(UserModel userModel) {
    return CobbleUtils.ASYNC.runAsync(() -> {
      try {
        UtilsFile.write(PATH, userModel);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }


  @Override
  public CompletableFuture<Boolean> addStorage(Storage storage, UUID targetUUID) {
    return CobbleUtils.ASYNC.supply(() -> {
      UserModel userModel = DataBaseUsers.USERS.getIfPresent(targetUUID);
      if (userModel == null) {
        try {
          userModel = UtilsFile.read(PATH, UserModel.class);
        } catch (Exception e) {
          return false;
        }
        if (userModel == null) return false;
      }
      userModel.addStorage(storage);
      userModel.save();
      return true;
    });
  }

  @Override
  public CompletableFuture<Boolean> addStorage(List<Storage> storage, UUID targetUUID) {
    return CobbleUtils.ASYNC.supply(() -> {
      UserModel userModel = DataBaseUsers.USERS.getIfPresent(targetUUID);
      if (userModel == null) {
        try {
          userModel = UtilsFile.read(PATH, UserModel.class);
        } catch (Exception e) {
          return false;
        }
        if (userModel == null) return false;
      }
      userModel.addStorage(storage);
      userModel.save();
      return true;
    });
  }

  @Override
  public CompletableFuture<Boolean> removeStorage(Storage storage, UUID targetUUID) {
    return CobbleUtils.ASYNC.supply(() -> {
      UserModel userModel = DataBaseUsers.USERS.getIfPresent(targetUUID);
      if (userModel == null) {
        try {
          userModel = UtilsFile.read(PATH, UserModel.class);
        } catch (Exception e) {
          return false;
        }
        if (userModel == null) return false;
      }
      userModel.removeStorage(storage.getId());
      userModel.save();
      return true;
    });
  }

  @Override
  public CompletableFuture<Boolean> removeStorage(List<Storage> storage, UUID targetUUID) {
    return CobbleUtils.ASYNC.supply(() -> {
      UserModel userModel = DataBaseUsers.USERS.getIfPresent(targetUUID);
      if (userModel == null) {
        try {
          userModel = UtilsFile.read(PATH, UserModel.class);
        } catch (Exception e) {
          return false;
        }
        if (userModel == null) return false;
      }
      for (Storage s : storage) {
        userModel.removeStorage(s.getId());
      }
      userModel.save();
      return true;
    });
  }

  @Override
  public CompletableFuture<List<UserModel>> findUsersInactiveSince(long millis) {
    return CobbleUtils.ASYNC.supply(() -> {
      List<UserModel> inactiveUsers = new ArrayList<>();
      try {
        var files = UtilsFile.getAllJsonFiles(PATH);
        for (var file : files) {
          try {
            UserModel userModel = UtilsFile.read(file, UserModel.class);
            if (userModel != null && userModel.getLastLogin().toEpochMilli() < millis) {
              inactiveUsers.add(userModel);
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      return inactiveUsers;
    });
  }

  @Override
  public CompletableFuture<List<UUID>> getOnlinePlayers() {
    return CobbleUtils.ASYNC.supply(() -> {
      List<UUID> onlinePlayers = new ArrayList<>();
      var userList = USERS.asMap().values();
      for (var userModel : userList) {
        onlinePlayers.add(userModel.getPlayerUUID());
      }
      return onlinePlayers;
    });
  }


}
