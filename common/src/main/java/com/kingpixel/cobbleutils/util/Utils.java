package com.kingpixel.cobbleutils.util;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.redis.handlers.RedisMessageHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public abstract class Utils {
  public static ThreadLocalRandom getRandom() {
    return ThreadLocalRandom.current();
  }

  public static boolean isPlaceholder() {
    try {
      Class.forName("eu.pb4.placeholders.api.Placeholders");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  public static void broadcastMessage(String message) {
    if (CobbleUtils.config.isRedisMessaging()) {
      RedisMessageHandler.sendBroadcast(message);
    } else {
      MinecraftServer server = CobbleUtils.server;
      ArrayList<ServerPlayerEntity> players = new ArrayList<>(server.getPlayerManager().getPlayerList());
      for (ServerPlayerEntity pl : players) {
        pl.sendMessage(AdventureTranslator.toNative(message));
      }
    }
  }

  public static void broadcastMessage(Text message) {
    if (CobbleUtils.config.isRedisMessaging()) {
      // Convert Text to String and send via Redis
      String textAsString = message.getString();
      RedisMessageHandler.sendBroadcast(textAsString);
    } else {
      MinecraftServer server = CobbleUtils.server;
      ArrayList<ServerPlayerEntity> players = new ArrayList<>(server.getPlayerManager().getPlayerList());
      for (ServerPlayerEntity pl : players) {
        pl.sendMessage(message);
      }
    }
  }

  public static void broadcastMessage(String message, String prefix) {
    if (CobbleUtils.config.isRedisMessaging()) {
      RedisMessageHandler.sendBroadcast(message.replace("%prefix%", prefix));
    } else {
      var text = AdventureTranslator.toNative(message, prefix);
      MinecraftServer server = CobbleUtils.server;
      ArrayList<ServerPlayerEntity> players = new ArrayList<>(server.getPlayerManager().getPlayerList());
      for (ServerPlayerEntity pl : players) {
        pl.sendMessage(text);
      }
    }
  }

}
