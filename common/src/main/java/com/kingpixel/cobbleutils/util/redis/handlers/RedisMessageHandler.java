package com.kingpixel.cobbleutils.util.redis.handlers;

import com.google.gson.JsonObject;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.messages.HiperMessage;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

public class RedisMessageHandler implements RedisHandler {


  public static final String CHANNEL = "cobbleutils:messages";


  @Override
  public String getIdentifier() {
    return CHANNEL;
  }

  @Override
  public void handle(JsonObject json) {
    handleIncomingMessage(json);
  }

  private static void handleIncomingMessage(JsonObject json) {
    if (CobbleUtils.server == null) return;

    try {
      String type = json.get("type").getAsString();

      if ("hipermessage".equals(type)) {
        HiperMessage.handleRedisMessage(json);
        return;
      }

      String content = json.get("content").getAsString();

      if (json.has("prefix") && !json.get("prefix").getAsString().isEmpty()) {
        content = content.replace("%prefix%", json.get("prefix").getAsString());
      }

      Text formatted = AdventureTranslator.toNative(content);

      switch (type) {

        case "broadcast" -> CobbleUtils.server.getPlayerManager().broadcast(formatted, false);

        case "player" -> {
          if (!json.has("uuid")) return;
          UUID uuid = UUID.fromString(json.get("uuid").getAsString());
          ServerPlayerEntity player =
            CobbleUtils.server.getPlayerManager().getPlayer(uuid);
          if (player != null) player.sendMessage(formatted);
        }

        case "actionbar" -> CobbleUtils.server.getPlayerManager().getPlayerList()
          .forEach(p -> p.sendMessage(formatted, true));

        case "actionbar_player" -> {
          if (!json.has("uuid")) return;
          UUID uuid = UUID.fromString(json.get("uuid").getAsString());
          ServerPlayerEntity player =
            CobbleUtils.server.getPlayerManager().getPlayer(uuid);
          if (player != null) player.sendMessage(formatted, true);
        }

        default -> CobbleUtils.LOGGER_RAW.warn("Unknown Redis message type: " + type);
      }

    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.error("Failed to handle Redis message: ", e);
    }
  }

  // ============================
  // PUBLIC API
  // ============================

  public static void sendBroadcast(String message, String prefix) {
    publish("broadcast", message, null, prefix);
  }

  public static void sendPlayer(UUID playerUUID, String fullMessage, String prefix) {
    publish("player", fullMessage, playerUUID, prefix);
  }

  public static void sendActionBarPlayer(UUID playerUUID, String message, String prefix) {
    publish("actionbar_player", message, playerUUID, prefix);
  }

  public static void sendActionBarBroadcast(String message, String prefix) {
    publish("actionbar", message, null, prefix);
  }


  private static void publish(String type, String content, UUID uuid, String prefix) {
    JsonObject json = new JsonObject();
    json.addProperty("type", type);
    json.addProperty("content", content);

    if (uuid != null) {
      json.addProperty("uuid", uuid.toString());
    }

    if (prefix != null && !prefix.isEmpty()) {
      json.addProperty("prefix", prefix);
    }

    CobbleUtils.redisManager.publish(CHANNEL, json);
  }
}