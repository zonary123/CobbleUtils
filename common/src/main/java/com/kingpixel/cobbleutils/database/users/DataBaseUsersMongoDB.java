package com.kingpixel.cobbleutils.database.users;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.database.users.models.Storage;
import com.kingpixel.cobbleutils.util.UtilsFile;
import com.kingpixel.cobbleutils.util.mongodb.MongoDBManager;
import com.kingpixel.cobbleutils.util.mongodb.MongoDBService;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 27/08/2025 15:11
 */
public class DataBaseUsersMongoDB extends DataBaseUsers {
  private MongoCollection<Document> collectionUser;

  @Override
  public void connect(DataBaseConfig config) {
    MongoDBManager mongoDBManager = MongoDBService.getOrCreateManager(config);
    collectionUser = mongoDBManager.getCollection("users");
  }

  @Override
  public void disconnect() {
    // MongoDBService manages the lifecycle — no-op
  }

  @Override
  public @Nullable UserModel findUserByUUID(@NotNull UUID uuid) {
    try {
      UserModel userModel = DataBaseUsers.USERS.getIfPresent(uuid);
      if (userModel != null) return userModel;
      Document document = collectionUser.find(Filters.eq(DataBaseUsers.FIELD_UUID, uuid.toString())).first();
      if (document == null) return null;
      userModel = UtilsFile.getGson().fromJson(document.toJson(), UserModel.class);
      userModel.fix();
      return userModel;
    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.error("Failed to find user by UUID: {}", uuid, e);
      return null;
    }
  }

  @Override
  public @Nullable UserModel findUserByName(@NotNull String name) {
    try {
      UserModel userModel = super.findUserByName(name);
      if (userModel != null) return userModel;
      Document document = collectionUser.find(Filters.eq("playerName", name)).first();
      return document != null
        ? UtilsFile.getGson().fromJson(document.toJson(), UserModel.class)
        : null;
    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.error("Failed to find user by name: {}", name, e);
      return null;
    }
  }

  @Override
  public void saveOrUpdateUser(UserModel user) {
    if (user == null) return;
    try {
      String json = UtilsFile.getGson().toJson(user, UserModel.class);
      Document document = UtilsFile.getGson().fromJson(json, Document.class);
      document.remove(DataBaseUsers.FIELD_STORAGE);
      collectionUser.updateOne(
        Filters.eq(DataBaseUsers.FIELD_UUID, user.getPlayerUUID().toString()),
        new Document("$set", document),
        new UpdateOptions().upsert(true)
      );
    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.error("Failed to save or update user: {}", user.getPlayerUUID(), e);
    }
  }

  @Override
  public List<UserModel> getAllUsers() {
    List<UserModel> userList = new ArrayList<>();
    for (Document doc : collectionUser.find()) {
      try {
        UserModel user = UtilsFile.getGson().fromJson(doc.toJson(), UserModel.class);
        if (user != null) userList.add(user);
      } catch (Exception e) {
        CobbleUtils.LOGGER_RAW.error("Failed to parse user document", e);
      }
    }
    return userList;
  }

  @Override
  public List<UserModel> getUsersInactiveSince(long millis) {
    Instant thresholdInstant = Instant.now().minus(millis, ChronoUnit.MILLIS);
    String thresholdIso = DateTimeFormatter.ISO_INSTANT.format(thresholdInstant);
    List<UserModel> inactiveUsers = new ArrayList<>();
    for (Document doc : collectionUser.find(Filters.gte("lastLogin", thresholdIso))) {
      try {
        UserModel user = UtilsFile.getGson().fromJson(doc.toJson(), UserModel.class);
        if (user != null) inactiveUsers.add(user);
      } catch (Exception e) {
        CobbleUtils.LOGGER_RAW.error("Failed to parse user document", e);
      }
    }
    return inactiveUsers;
  }

  @Override
  public void disconnected(ServerPlayerEntity player) {
    UUID playerUUID = player.getUuid();
    try {
      collectionUser.updateOne(
        Filters.eq(DataBaseUsers.FIELD_UUID, playerUUID.toString()),
        new Document("$set", new Document(DataBaseUsers.FIELD_IS_ONLINE, false)
          .append("disconnectTime", DateTimeFormatter.ISO_INSTANT.format(Instant.now())))
      );
    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.error("Failed to update disconnect for: {}", playerUUID, e);
    }
  }

  @Override
  public void addStorage(Storage storage, UUID playerUUID) {
    try {
      collectionUser.updateOne(
        Filters.eq(DataBaseUsers.FIELD_UUID, playerUUID.toString()),
        new Document("$push", new Document(DataBaseUsers.FIELD_STORAGE, storage.toDocument()))
      );
      DataBaseUsers.USERS.invalidate(playerUUID);
    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.error("Failed to add storage for: {}", playerUUID, e);
    }
  }

  @Override
  public void addStorage(List<Storage> storage, UUID playerUUID) {
    try {
      List<Document> storageDocs = new ArrayList<>();
      for (Storage s : storage) storageDocs.add(s.toDocument());
      collectionUser.updateOne(
        Filters.eq(DataBaseUsers.FIELD_UUID, playerUUID.toString()),
        new Document("$push", new Document(DataBaseUsers.FIELD_STORAGE, new Document("$each", storageDocs)))
      );
      DataBaseUsers.USERS.invalidate(playerUUID);
    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.error("Failed to add storage list for: {}", playerUUID, e);
    }
  }

  @Override
  public Storage removeStorage(Storage storage, UUID playerUUID) {
    UUID id = storage.getId();
    if (id == null) return null;
    try {
      collectionUser.updateOne(
        Filters.eq(DataBaseUsers.FIELD_UUID, playerUUID.toString()),
        new Document("$pull", new Document(DataBaseUsers.FIELD_STORAGE, new Document("id", id.toString())))
      );
      UserModel user = findUserByUUID(playerUUID);
      if (user == null) return null;
      return user.removeStorage(id);
    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.error("Failed to remove storage for: {}", playerUUID, e);
      return null;
    }
  }

  @Override
  public List<UUID> getOnlinePlayers() {
    List<UUID> onlinePlayers = new ArrayList<>();
    try {
      collectionUser.find(Filters.eq(DataBaseUsers.FIELD_IS_ONLINE, true))
        .forEach(doc -> {
          try {
            String uuidStr = doc.getString(DataBaseUsers.FIELD_UUID);
            if (uuidStr != null) onlinePlayers.add(UUID.fromString(uuidStr));
          } catch (Exception e) {
            CobbleUtils.LOGGER_RAW.error("Failed to parse online player", e);
          }
        });
    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.error("Failed to get online players", e);
    }
    return onlinePlayers;
  }
}
