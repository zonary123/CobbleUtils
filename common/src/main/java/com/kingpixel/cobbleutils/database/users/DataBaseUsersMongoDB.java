package com.kingpixel.cobbleutils.database.users;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.Model.rewards.Reward;
import com.kingpixel.cobbleutils.database.users.models.Storage;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.UserCache;
import org.bson.Document;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class DataBaseUsersMongoDB extends DataBaseUsers {

  private MongoClient mongoClient;
  private MongoDatabase database;
  private MongoCollection<Document> collectionUser;

  private static final String KEY_PLAYER_UUID = "playerUUID";
  private static final String KEY_PLAYER_NAME = "playerName";
  private static final String KEY_STORAGE_LIST = "storageList";
  private static final String KEY_DISCONNECT_TIME = "disconnectTime";
  private static final String KEY_ONLINE = "online";

  @Override
  public void connect(DataBaseConfig config) {
    MongoClientSettings settings = MongoClientSettings.builder()
      .applyConnectionString(new ConnectionString(config.getUrl()))
      .applicationName("CobbleUtils-Users")
      .build();
    mongoClient = MongoClients.create(settings);
    database = mongoClient.getDatabase(config.getDatabase());
    collectionUser = database.getCollection("users");
  }

  @Override
  public void disconnect() {
    saveAll().join();
    if (mongoClient != null) mongoClient.close();
  }

  @Override
  public CompletableFuture<@Nullable UserModel> findUserModel(UUID uuid) {
    final String uuidStr = uuid.toString();
    return CobbleUtils.ASYNC.supply(() -> {
      UserModel cachedUser = getUserModel(uuid);
      if (cachedUser != null) return cachedUser;

      Document doc = collectionUser.find(new Document(KEY_PLAYER_UUID, uuidStr)).first();
      if (doc == null) return null;

      return UserModel.fromDocument(doc);
    });
  }

  @Override
  public CompletableFuture<@Nullable UserModel> findUserModel(String username) {
    return CobbleUtils.ASYNC.supply(() -> {
      UserCache userCache = CobbleUtils.server.getUserCache();
      if (userCache != null) {
        var optProfile = userCache.findByName(username);
        if (optProfile.isPresent()) {
          UUID uuid = optProfile.get().getId();
          UserModel cachedUser = getUserModel(uuid);
          if (cachedUser != null) return cachedUser;
        }
      }

      Document doc = collectionUser.find(new Document(KEY_PLAYER_NAME, username)).first();
      if (doc == null) return null;

      return UserModel.fromDocument(doc);
    });
  }

  @Override
  public CompletableFuture<Set<Storage>> findUserStorage(UUID uuid) {
    final String uuidStr = uuid.toString();
    return CobbleUtils.ASYNC.supply(() -> {
      Document doc = collectionUser
        .find(new Document(KEY_PLAYER_UUID, uuidStr))
        .projection(new Document(KEY_STORAGE_LIST, 1).append("_id", 0))
        .first();

      if (doc == null) return Collections.emptySet();

      List<Document> storageDocs = doc.getList(KEY_STORAGE_LIST, Document.class);
      if (storageDocs == null || storageDocs.isEmpty()) return Collections.emptySet();

      return storageDocs.stream()
        .map(Storage::fromDocument)
        .collect(Collectors.toSet());
    });
  }

  @Override
  public boolean isAvailableReward(ServerPlayerEntity player, ItemChance itemChance) {
    UserModel userModel = getUserModel(player.getUuid());
    if (userModel == null) return false;
    return userModel.isAvailableReward(itemChance);
  }

  public CompletableFuture<Boolean> isAvailableReward(UUID playerUUID, Reward reward) {
    if (!Boolean.TRUE.equals(reward.getUnique()) || reward.getIdentifier() == null)
      return CompletableFuture.completedFuture(true);

    return CobbleUtils.ASYNC.supply(() -> {
      String identifier = reward.getIdentifier();
      Document userDoc = collectionUser.find(new Document("playerUUID", playerUUID.toString()))
        .projection(new Document("rewardsClaimed." + identifier, 1))
        .first();

      int timesClaimed = 0;
      Instant finishCooldown = null;

      if (userDoc != null) {
        Document rewardDoc = (Document) ((Document) userDoc.get("rewardsClaimed")).get(identifier);
        if (rewardDoc != null) {
          timesClaimed = rewardDoc.getInteger("timesClaimed", 0);
          String finishStr = rewardDoc.getString("finishCooldown");
          if (finishStr != null) finishCooldown = Instant.parse(finishStr);
        }
      }

      boolean onCooldown = false;
      if (reward.getCooldown() != null && timesClaimed >= reward.getAmount()) {
        if (finishCooldown != null) {
          Instant nextAvailable = finishCooldown.plusMillis(reward.getCooldown().toMillis());
          onCooldown = Instant.now().isBefore(nextAvailable);
        }
      }

      if (onCooldown) return false;

      timesClaimed++;
      if (timesClaimed >= reward.getAmount() && reward.getCooldown() != null) {
        finishCooldown = Instant.now();
      }

      Document rewardUpdate = new Document("timesClaimed", timesClaimed);
      if (finishCooldown != null) rewardUpdate.append("finishCooldown", finishCooldown.toString());

      collectionUser.updateOne(
        new Document("playerUUID", playerUUID.toString()),
        new Document("$set", new Document("rewardsClaimed." + identifier, rewardUpdate)),
        new UpdateOptions().upsert(true)
      );

      return true;
    });
  }

  @Override
  public CompletableFuture<Void> saveUserModel(UserModel userModel) {
    return CobbleUtils.ASYNC.runAsync(() -> {
      final String uuidStr = userModel.getPlayerUUID().toString();
      final Document filter = new Document(KEY_PLAYER_UUID, uuidStr);

      final Document update = new Document("$set", userModel.toDocument());

      collectionUser.updateOne(
        filter,
        update,
        new UpdateOptions().upsert(true)
      );
    });
  }


  @Override
  public CompletableFuture<Boolean> addStorage(Storage storage, UUID targetUUID) {
    final String uuidStr = targetUUID.toString();
    return CobbleUtils.ASYNC.supply(() -> {
      Document filter = new Document(KEY_PLAYER_UUID, uuidStr);
      Document update = new Document("$addToSet", new Document(KEY_STORAGE_LIST, storage.toDocument()));
      var result = collectionUser.updateOne(filter, update);
      return result.getModifiedCount() > 0;
    });
  }

  @Override
  public CompletableFuture<Boolean> addStorage(List<Storage> storage, UUID targetUUID) {
    final String uuidStr = targetUUID.toString();
    return CobbleUtils.ASYNC.supply(() -> {
      Document filter = new Document(KEY_PLAYER_UUID, uuidStr);
      List<Document> storageDocs = storage.stream()
        .map(Storage::toDocument)
        .toList();
      Document update = new Document("$addToSet", new Document(KEY_STORAGE_LIST, new Document("$each", storageDocs)));
      var result = collectionUser.updateOne(filter, update);
      return result.getModifiedCount() > 0;
    });
  }

  @Override
  public CompletableFuture<Boolean> removeStorage(Storage storage, UUID targetUUID) {
    final String uuidStr = targetUUID.toString();
    return CobbleUtils.ASYNC.supply(() -> {
      Document filter = new Document(KEY_PLAYER_UUID, uuidStr);
      Document update = new Document("$pull", new Document(KEY_STORAGE_LIST, storage.toDocument()));
      var result = collectionUser.updateOne(filter, update);
      return result.getModifiedCount() > 0;
    });
  }

  @Override
  public CompletableFuture<Boolean> removeStorage(List<Storage> storage, UUID targetUUID) {
    final String uuidStr = targetUUID.toString();
    return CobbleUtils.ASYNC.supply(() -> {
      Document filter = new Document(KEY_PLAYER_UUID, uuidStr);
      List<Document> storageDocs = storage.stream()
        .map(Storage::toDocument)
        .toList();
      Document update = new Document("$pull", new Document(KEY_STORAGE_LIST, new Document("$in", storageDocs)));
      var result = collectionUser.updateOne(filter, update);
      return result.getModifiedCount() > 0;
    });
  }

  @Override
  public CompletableFuture<List<UserModel>> findUsersInactiveSince(long millis) {
    return CobbleUtils.ASYNC.supply(() -> {
      long cutoff = System.currentTimeMillis() - millis;
      List<Document> docs = collectionUser.find()
        .projection(new Document(KEY_PLAYER_UUID, 1)
          .append(KEY_PLAYER_NAME, 1)
          .append(KEY_DISCONNECT_TIME, 1)
          .append("_id", 0))
        .into(new ArrayList<>());

      return docs.stream()
        .filter(doc -> {
          String disconnectStr = doc.getString(KEY_DISCONNECT_TIME);
          if (disconnectStr == null) return false;
          Instant disconnect = Instant.parse(disconnectStr);
          return disconnect.toEpochMilli() < cutoff;
        })
        .map(doc -> {
          UserModel userModel = new UserModel();
          userModel.setPlayerUUID(UUID.fromString(doc.getString(KEY_PLAYER_UUID)));
          userModel.setPlayerName(doc.getString(KEY_PLAYER_NAME));
          String disconnectStr = doc.getString(KEY_DISCONNECT_TIME);
          if (disconnectStr != null) {
            userModel.setLastLogin(Instant.parse(disconnectStr));
          }
          return userModel;
        })
        .toList();
    });
  }

  @Override
  public CompletableFuture<List<UUID>> getOnlinePlayers() {
    return CobbleUtils.ASYNC.supply(() -> {
      List<Document> docs = collectionUser.find(new Document(KEY_ONLINE, true))
        .projection(new Document(KEY_PLAYER_UUID, 1).append("_id", 0))
        .into(new ArrayList<>());

      return docs.stream()
        .map(doc -> UUID.fromString(doc.getString(KEY_PLAYER_UUID)))
        .toList();
    });
  }
}
