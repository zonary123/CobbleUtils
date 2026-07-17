package com.kingpixel.cobbleutils.util.redis.handlers;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.JsonObject;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.Location;

import java.time.Duration;
import java.util.UUID;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.server.world.ServerWorld;
import java.util.List;
import java.util.ArrayList;

public class RedisTeleportHandler implements RedisHandler {

  public static final String CHANNEL = "cobbleutils:teleport";

  public static final Cache<UUID, Location> LOCATION_CACHE =
    Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofSeconds(5))
      .maximumSize(1000)
      .build();

  @Override
  public String getIdentifier() {
    return CHANNEL;
  }

  @Override
  public void handle(JsonObject json) {

    try {
      if (json == null) return;

      if (json.has("type")) {
        String type = json.get("type").getAsString();
        if ("request_worlds".equals(type)) {
          publishWorlds();
          return;
        } else if ("worlds_info".equals(type)) {
          if (!json.has("server") || !json.has("worlds")) return;
          String senderServer = json.get("server").getAsString();
          JsonArray worldsArray = json.getAsJsonArray("worlds");
          List<String> worldsList = new ArrayList<>();
          for (JsonElement el : worldsArray) {
            worldsList.add(el.getAsString());
          }
          CobbleUtils.getServerWorlds().put(senderServer, worldsList);
          return;
        }
      }

      if (!isValid(json)) return;

      String targetServer = json.get("server").getAsString();

      if (CobbleUtils.getServerName() != null &&
        !CobbleUtils.getServerName().equals(targetServer)) {
        return;
      }

      UUID playerUUID = UUID.fromString(json.get("uuid").getAsString());

      JsonObject loc = json.getAsJsonObject("location");

      Location location = new Location();
      location.setWorld(loc.get("world").getAsString());
      location.setX(loc.get("x").getAsDouble());
      location.setY(loc.get("y").getAsDouble());
      location.setZ(loc.get("z").getAsDouble());
      location.setYaw(loc.get("yaw").getAsFloat());
      location.setPitch(loc.get("pitch").getAsFloat());
      location.setServer(targetServer);

      LOCATION_CACHE.put(playerUUID, location);

      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER_RAW.info(
          "Teleport stored for player " + playerUUID +
            " to " + location.getWorld() +
            " (" + location.getX() + ", " +
            location.getY() + ", " +
            location.getZ() + ")"
        );
      }

    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.error("Failed to handle teleport message");
      e.printStackTrace();
    }
  }

  private boolean isValid(JsonObject json) {
    if (json == null) return false;

    if (!json.has("uuid") || !json.has("location") || !json.has("server"))
      return false;

    JsonObject loc = json.getAsJsonObject("location");

    return loc.has("world") &&
      loc.has("x") &&
      loc.has("y") &&
      loc.has("z") &&
      loc.has("yaw") &&
      loc.has("pitch");
  }

  public static void publishWorlds() {
    if (CobbleUtils.redisManager == null || CobbleUtils.getServerName() == null) return;
    try {
      JsonObject json = new JsonObject();
      json.addProperty("type", "worlds_info");
      json.addProperty("server", CobbleUtils.getServerName());

      JsonArray worldsArray = new JsonArray();
      if (CobbleUtils.server != null) {
        for (ServerWorld world : CobbleUtils.server.getWorlds()) {
          worldsArray.add(world.getRegistryKey().getValue().toString());
        }
      }
      json.add("worlds", worldsArray);

      CobbleUtils.redisManager.publish(CHANNEL, json);
    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.error("Failed to publish worlds: " + e.getMessage());
    }
  }

  public static void requestWorlds() {
    if (CobbleUtils.redisManager == null) return;
    try {
      JsonObject json = new JsonObject();
      json.addProperty("type", "request_worlds");
      CobbleUtils.redisManager.publish(CHANNEL, json);
    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.error("Failed to request worlds: " + e.getMessage());
    }
  }
}