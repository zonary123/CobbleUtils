package com.kingpixel.cobbleutils.util;

import com.cobblemon.mod.common.battles.BattleRegistry;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DurationValue;
import com.kingpixel.cobbleutils.mixins.UserCacheMixin;
import com.kingpixel.cobbleutils.util.manager.CooldownManager;
import com.kingpixel.cobbleutils.util.redis.handlers.RedisMessageHandler;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.UserCache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Carlos Varas Alonso - 28/06/2024 20:44
 */
public class PlayerUtils {

  /**
   * Method to check if a player is in a battle.
   *
   * @param player The player to check.
   * @return true if the player is in a battle.
   */
  public static boolean isBattle(ServerPlayerEntity player) {
    // 1.7 var battle = BattleRegistry.getBattleByParticipatingPlayer(player);
    var battle = BattleRegistry.getBattleByParticipatingPlayer(player);
    if (battle != null) {
      PlayerUtils.sendMessage(
        player,
        CobbleUtils.language.getMessagearebattle(),
        CobbleUtils.config.getPrefix(),
        TypeMessage.CHAT
      );
      return true;
    }
    return false;
  }

  /**
   * Method to check if a player has a cooldown for a specific menu.
   *
   * @param player   The player to check.
   * @param menu     The menu to check.
   * @param duration The duration value to check.
   * @return true if the player has a cooldown for the specific menu.
   */
  public static boolean isCooldownMenu(ServerPlayerEntity player, String menu, DurationValue duration) {
    return CooldownManager.hasCooldownMenu(player, menu, duration);
  }

  public static boolean hasCooldownCommand(ServerPlayerEntity player, String command, DurationValue duration) {
    return CooldownManager.hasCooldownCommand(player, command, duration);
  }

  /**
   * Method to send a message to a player by UUID with a specific prefix and type of message.
   *
   * @param playerUUID  The UUID of the player to send the message to.
   * @param message     The message to send.
   * @param prefix      The prefix to use in the message.
   * @param typeMessage The type of message to send (CHAT, ACTIONBAR, ACTIONBAR_BROADCAST, BROADCAST).
   */
  public static void sendMessage(UUID playerUUID, String message, String prefix, TypeMessage typeMessage) {
    if (message.isEmpty()) {
      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER_RAW.info("Tried to send an empty message to player UUID: " + playerUUID);
      }
      return;
    }

    String fullMessage = message.replace("%prefix%", prefix);

    if (CobbleUtils.config.isRedisMessaging()) {
      switch (typeMessage) {
        case CHAT -> RedisMessageHandler.sendPlayer(playerUUID, fullMessage, prefix);
        case ACTIONBAR -> RedisMessageHandler.sendActionBarPlayer(playerUUID, fullMessage, prefix);
        case ACTIONBAR_BROADCAST -> RedisMessageHandler.sendActionBarBroadcast(fullMessage, prefix);
        case BROADCAST -> RedisMessageHandler.sendBroadcast(fullMessage, prefix);
      }
    } else {
      if (CobbleUtils.server == null) return;
      ServerPlayerEntity player = CobbleUtils.server.getPlayerManager().getPlayer(playerUUID);
      if (player != null) {
        sendMessage(player, message, prefix, typeMessage);
      }
    }
  }

  /**
   * Method to send a message to a player without a prefix.
   *
   * @param player  The player to send the message to.
   * @param message The message to send.
   */
  @Deprecated
  public static void sendMessage(ServerPlayerEntity player, String message) {
    if (message.isEmpty()) return;
    if (player == null) return;
    player.sendMessage(AdventureTranslator.toNativeWithOutPrefix(message, player));
  }

  /**
   * Method to send a message to a player with a specific prefix.
   *
   * @param player  The player to send the message to.
   * @param message The message to send.
   * @param prefix  The prefix to use in the message.
   */
  @Deprecated
  public static void sendMessage(ServerPlayerEntity player, String message, String prefix) {
    if (message.isEmpty()) return;
    if (player == null) return;
    player.sendMessage(AdventureTranslator.toNative(message, prefix, player));
  }

  /**
   * Method to send a message to a player with a specific prefix and type of message.
   *
   * @param player      The player to send the message to.
   * @param message     The message to send.
   * @param prefix      The prefix to use in the message.
   * @param typeMessage The type of message to send (CHAT, ACTIONBAR, ACTIONBAR_BROADCAST, BROADCAST).
   */
  public static void sendMessage(ServerPlayerEntity player, String message, String prefix, TypeMessage typeMessage) {
    if (message.isEmpty()) {
      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER_RAW.info("Tried to send an empty message to player: " +
          (player != null ? player.getGameProfile().getName() : "null"));
      }
      return;
    }
    CompletableFuture.runAsync(() -> {
        if (CobbleUtils.config.isRedisMessaging()) {
          if (player == null) return;
          sendMessage(player.getUuid(), message, prefix, typeMessage);
          return;
        }

        switch (typeMessage) {
          case CHAT -> sendMessage(player, message, prefix);
          case ACTIONBAR -> player.sendMessage(AdventureTranslator.toNative(message, prefix, player), true);
          case ACTIONBAR_BROADCAST -> {
            if (CobbleUtils.server == null) return;
            var text = AdventureTranslator.toNative(message, prefix);
            var playerList = CobbleUtils.server.getPlayerManager().getPlayerList();
            for (ServerPlayerEntity serverPlayerEntity : playerList) {
              serverPlayerEntity.sendMessage(text, true);
            }
          }
          case BROADCAST -> broadcast(message, prefix);
        }
      }, CobbleUtils.ASYNC.getExecutor())
      .orTimeout(30, TimeUnit.SECONDS)
      .exceptionally(e -> {
        e.printStackTrace();
        return null;
      });
  }

  @Deprecated
  public static void broadcast(String message) {
    if (!message.isEmpty()) {
      if (CobbleUtils.config.isRedisMessaging()) {
        RedisMessageHandler.sendBroadcast(message, "");
      } else {
        if (CobbleUtils.server == null) return;
        var playerList = CobbleUtils.server.getPlayerManager().getPlayerList();
        playerList.forEach(player -> sendMessage(player, message));
      }
    }
  }

  @Deprecated
  public static void broadcast(String message, String prefix) {
    if (!message.isEmpty()) {
      if (CobbleUtils.config.isRedisMessaging()) {
        RedisMessageHandler.sendBroadcast(message, prefix);
      } else {
        var text = AdventureTranslator.toNative(message, prefix);
        if (CobbleUtils.server == null) return;
        var playerList = CobbleUtils.server.getPlayerManager().getPlayerList();
        playerList.forEach(player -> player.sendMessage(text));
      }
    }
  }

  /**
   * Method to get the cooldown based on the player's permissions.
   *
   * @param cooldowns       The cooldowns to check.
   * @param defaultCooldown The default cooldown.
   * @param player          The player to check.
   * @return The cooldown.
   */
  public static int getCooldown(Map<String, Integer> cooldowns, int defaultCooldown, ServerPlayerEntity player) {
    int cooldown = defaultCooldown;
    var entries = cooldowns.entrySet();
    for (Map.Entry<String, Integer> entry : entries) {
      if (entry.getValue() < cooldown) {
        if (player != null && LuckPermsUtil.checkPermission(player, entry.getKey())) {
          cooldown = entry.getValue();
        }
      }
    }
    return cooldown;
  }

  public static long getCooldown(Map<String, DurationValue> cooldowns, DurationValue defaultCooldown, ServerPlayerEntity player) {
    long cooldown = defaultCooldown.toMillis();
    var entries = cooldowns.entrySet();
    for (Map.Entry<String, DurationValue> entry : entries) {
      if (entry.getValue().toMillis() > cooldown) continue;
      if (player != null && LuckPermsUtil.checkPermission(player, entry.getKey())) {
        cooldown = entry.getValue().toMillis();
      }
    }
    return cooldown;
  }

  @Deprecated(forRemoval = true, since = "1.1.3")
  public static String getCooldown(Date date) {
    if (date == null) return CobbleUtils.language.getNocooldown();
    return getCooldown(date.getTime());
  }

  private static final Cache<Long, String> cooldownCache = Caffeine.newBuilder()
    .maximumSize(10000)
    .expireAfterAccess(5, TimeUnit.MINUTES)
    .build();

  /**
   * Method to get the cooldown in a human-readable format.
   *
   * @param timestamp The timestamp to check.
   * @return The cooldown in a human-readable format.
   */
  public static String getCooldown(long timestamp) {
    long timeMillis = timestamp - System.currentTimeMillis();
    if (timeMillis <= 0) return CobbleUtils.language.getNocooldown();

    long timeSeconds = timeMillis / 1000;
    return cooldownCache.get(timeSeconds, t -> {
      long days = t / (60 * 60 * 24);
      long hours = (t / (60 * 60)) % 24;
      long minutes = (t / 60) % 60;
      long seconds = t % 60;

      StringBuilder result = new StringBuilder();
      if (days > 0)
        result.append((days == 1 ? CobbleUtils.language.getDay() : CobbleUtils.language.getDays())
          .replace("%s", Long.toString(days)));
      if (hours > 0)
        result.append((hours == 1 ? CobbleUtils.language.getHour() : CobbleUtils.language.getHours())
          .replace("%s", Long.toString(hours)));
      if (minutes > 0)
        result.append((minutes == 1 ? CobbleUtils.language.getMinute() : CobbleUtils.language.getMinutes())
          .replace("%s", Long.toString(minutes)));
      if (seconds > 0)
        result.append((seconds == 1 ? CobbleUtils.language.getSecond() : CobbleUtils.language.getSeconds())
          .replace("%s", Long.toString(seconds)));

      return result.isEmpty() ? CobbleUtils.language.getNocooldown() : result.toString().trim();
    });
  }

  /**
   * Method to get the head item of a player by UUID.
   *
   * @param playerUUID The UUID of the player.
   * @return The head item of the player.
   */
  public static ItemStack getHeadItem(UUID playerUUID) {
    if (playerUUID == null) return ItemUtils.parseItemId("minecraft:player_head");
    var userCache = CobbleUtils.server.getUserCache();
    if (userCache == null) return ItemUtils.parseItemId("minecraft:player_head");
    if (!((UserCacheMixin) userCache).byUuid().containsKey(playerUUID))
      return ItemUtils.parseItemId("minecraft:player_head");
    var gameProfile = userCache.getByUuid(playerUUID);
    if (gameProfile.isPresent()) {
      ItemStack itemStack = Items.PLAYER_HEAD.getDefaultStack();
      itemStack.set(DataComponentTypes.PROFILE, new ProfileComponent(gameProfile.get()));
      return itemStack;
    }
    return ItemUtils.parseItemId("minecraft:player_head");
  }

  /**
   * Method to get the head item of a player.
   *
   * @param player The player.
   * @return The head item of the player.
   */
  public static ItemStack getHeadItem(ServerPlayerEntity player) {
    if (player == null) return ItemUtils.parseItemId("minecraft:player_head");
    return getHeadItem(player.getUuid());
  }

  public static ItemStack getHeadItem(String player, int amount) {
    ItemStack itemStack = Items.PLAYER_HEAD.getDefaultStack();
    UserCache userCache = CobbleUtils.server.getUserCache();
    if (userCache == null) return itemStack;
    Optional<GameProfile> optionalGameProfile = userCache.findByName(player);
    var profile = optionalGameProfile.orElseGet(() -> new GameProfile(UUID.randomUUID(), player));
    itemStack.set(DataComponentTypes.PROFILE, new ProfileComponent(profile));
    itemStack.setCount(amount);
    return itemStack;
  }

  /**
   * Method to check if a cooldown is active.
   *
   * @param cooldown The cooldown to check.
   * @return true if the cooldown is active.
   */
  public static boolean isCooldown(Date cooldown) {
    if (cooldown == null) return false;
    return new Date().before(cooldown);
  }

  /**
   * Method to check if a cooldown is active.
   *
   * @param cooldown The cooldown to check.
   * @return true if the cooldown is active.
   */
  public static boolean isCooldown(Long cooldown) {
    if (cooldown == null) return false;
    return isCooldown(new Date(cooldown));
  }

  /**
   * Execute a command
   *
   * @param command The command to execute
   * @return If the command was executed successfully
   */
  public static boolean executeCommand(String command, ServerPlayerEntity player) {
    command = command.replace("%player%", player.getGameProfile().getName());
    CommandDispatcher<ServerCommandSource> disparador = CobbleUtils.server.getCommandManager().getDispatcher();
    ServerCommandSource serverSource = CobbleUtils.server.getCommandSource();
    ParseResults<ServerCommandSource> parse = disparador.parse(command, serverSource);
    String finalCommand = command;
    CobbleUtils.server.execute(() -> {
      try {
        disparador.execute(parse);
      } catch (CommandSyntaxException e) {
        System.err.println("Error to execute command: " + finalCommand);
        e.printStackTrace();
      }
    });
    return true;
  }

  /**
   * Method to cast a PlayerEntity to a ServerPlayerEntity.
   *
   * @param player The player to cast.
   * @return The ServerPlayerEntity.
   */
  public static ServerPlayerEntity castPlayer(PlayerEntity player) {
    try {
      return player instanceof ServerPlayerEntity ? (ServerPlayerEntity) player : null;
    } catch (ClassCastException e) {
      return CobbleUtils.server.getPlayerManager().getPlayer(player.getUuid());
    }
  }

  private static final OkHttpClient client = new OkHttpClient();

  /**
   * Method to get the GameProfile of a player by name using Mojang API.
   *
   * @param name The name of the player.
   * @return The GameProfile of the player or null if not found.
   */
  public static GameProfile getGameProfile(String name) {
    Request request = new Request.Builder()
      .url("https://api.mojang.com/users/profiles/minecraft/" + name)
      .build();

    try (Response response = client.newCall(request).execute()) {
      if (response.isSuccessful() && response.body() != null) {
        String jsonStr = response.body().string();
        JSONObject json = new JSONObject(jsonStr);

        if (json.has("id") && json.has("name")) {
          String id = json.getString("id");
          String playerName = json.getString("name");

          // Convertir Mojang UUID (sin guiones) a formato estándar
          UUID uuid = UUID.fromString(id.replaceFirst(
            "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
            "$1-$2-$3-$4-$5"
          ));

          return new GameProfile(uuid, playerName);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }
}