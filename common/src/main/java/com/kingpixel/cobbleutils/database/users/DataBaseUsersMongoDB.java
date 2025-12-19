package com.kingpixel.cobbleutils.database.users;

import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.database.users.models.Storage;
import com.kingpixel.cobbleutils.util.Utils;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
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
  private MongoClient mongoClient;
  private MongoDatabase database;
  private MongoCollection<Document> collectionUser;

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
    if (mongoClient != null) {
      mongoClient.close();
    }
  }

  @Override
  public @Nullable UserModel findUserByUUID(@NotNull UUID uuid) {
    UserModel userModel = super.findUserByUUID(uuid);
    if (userModel != null) return userModel; // si está en la cache,
    Document document = collectionUser.find(Filters.eq("playerUUID", uuid.toString()))
      .first();
    if (document != null) {
      userModel = Utils.newWithoutSpacingGson().fromJson(document.toJson(), UserModel.class);
      return userModel;
    }
    return null;
  }

  @Override
  public @Nullable UserModel findUserByName(@NotNull String name) {
    UserModel userModel = super.findUserByName(name);
    if (userModel != null) return userModel; // si está en la cache,
    Document document = collectionUser.find(Filters.eq("playerName", name))
      .first();
    if (document != null) {
      userModel = Utils.newWithoutSpacingGson().fromJson(document.toJson(), UserModel.class);
      return userModel;
    }
    return null;
  }

  @Override
  public void saveOrUpdateUser(UserModel user) {
    if (user == null) return;

    try {
      // 1️⃣ Convertir UserModel a Document
      String json = Utils.newWithoutSpacingGson().toJson(user, UserModel.class);
      Document document = Utils.newWithoutSpacingGson().fromJson(json, Document.class);
      document.remove("storageList"); // campo que no queremos guardar

      // 2️⃣ Actualizar solo los campos existentes o crear si no existe
      collectionUser.updateOne(
        Filters.eq("playerUUID", user.getPlayerUUID().toString()),
        new Document("$set", document),
        new UpdateOptions().upsert(true)
      );

    } catch (Exception e) {
      System.err.println("❌ Failed to save or update user: " + user);
      e.printStackTrace();
    }
  }


  @Override
  public List<UserModel> getAllUsers() {
    List<UserModel> userList = new ArrayList<>();
    for (Document doc : collectionUser.find()) {
      try {
        UserModel user = Utils.newWithoutSpacingGson().fromJson(doc.toJson(), UserModel.class);
        if (user != null) {
          userList.add(user);
        }
      } catch (Exception e) {
        System.err.println("Failed to parse user document: " + doc.toJson());
        e.printStackTrace();
      }
    }
    return userList;
  }

  @Override
  public List<UserModel> getUsersInactiveSince(long millis) {
    Instant thresholdInstant = Instant.now().minus(millis, ChronoUnit.MILLIS);
    String thresholdIso = DateTimeFormatter.ISO_INSTANT.format(thresholdInstant); // Ej: "2025-08-20T15:30:00Z"

    List<UserModel> inactiveUsers = new ArrayList<>();
    var docs = collectionUser.find(Filters.gte("lastLogin", thresholdIso));
    for (Document doc : docs) {
      try {
        UserModel user = Utils.newWithoutSpacingGson().fromJson(doc.toJson(), UserModel.class);
        if (user != null) {
          inactiveUsers.add(user);
        }
      } catch (Exception e) {
        System.err.println("Failed to parse user document: " + doc.toJson());
        e.printStackTrace();
      }
    }

    return inactiveUsers;
  }

  @Override
  public void addStorage(Storage storage, UUID playerUUID) {
    collectionUser.updateOne(
      Filters.eq("playerUUID", playerUUID.toString()),
      new Document("$push", new Document("storageList", storage.toDocument()))
    );
    UserModel user = findUserByUUID(playerUUID);
    if (user == null) return;
    user.getStorageList().add(storage);
  }

  @Override
  public Storage removeStorage(Storage storage, UUID playerUUID) {
    UUID id = storage.getId();
    if (id == null) return null;
    collectionUser.updateOne(
      Filters.eq("playerUUID", playerUUID.toString()),
      new Document("$pull", new Document("storageList", new Document("id", id.toString())))
    );
    UserModel user = findUserByUUID(playerUUID);
    if (user == null) return null;
    return user.removeStorage(id);
  }

  @Override public List<UUID> getOnlinePlayers() {
    var filter = Filters.eq("isOnline", true);
    return collectionUser.find(filter)
      .map(doc -> {
        try {
          String uuidStr = doc.getString("playerUUID");
          if (uuidStr != null) {
            return UUID.fromString(uuidStr);
          }
        } catch (Exception e) {
          System.err.println("Failed to parse online player document: " + doc.toJson());
          e.printStackTrace();
        }
        return null;
      })
      .into(new ArrayList<>());
  }


}
