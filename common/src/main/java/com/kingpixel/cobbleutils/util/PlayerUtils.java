package com.kingpixel.cobbleutils.util;

import com.cobblemon.mod.common.Cobblemon;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.kingpixel.cobbleutils.CobbleUtils;
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

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Carlos Varas Alonso - 28/06/2024 20:44
 */
public class PlayerUtils {

  public static ExecutorService MESSAGE_EXECUTOR = Executors.newFixedThreadPool(4, new ThreadFactoryBuilder()
    .setDaemon(true)
    .setNameFormat("CobbleUtils Message - %d")
    .build());

  public static boolean isBattle(ServerPlayerEntity player) {
    var battle = Cobblemon.INSTANCE.getBattleRegistry().getBattleByParticipatingPlayer(player);
    return battle != null;
  }

  public static void sendMessage(UUID playerUUID, String message, String prefix, TypeMessage typeMessage) {
    if (message.isEmpty()) return;

    String fullMessage = message.replace("%prefix%", prefix);

    if (CobbleUtils.config.isRedisMessaging()) {
      CompletableFuture.runAsync(() -> {
          switch (typeMessage) {
            case CHAT -> RedisManager.sendMessage(playerUUID, fullMessage, prefix);
            case ACTIONBAR -> RedisManager.sendActionBarMessage(playerUUID, fullMessage, prefix);
            case ACTIONBAR_BROADCAST -> RedisManager.sendActionBarMessage(fullMessage, prefix);
            case BROADCAST -> RedisManager.sendMessage(fullMessage, prefix);
          }
        }, RedisManager.EXECUTOR_REDIS)
        .exceptionally(e -> {
          e.printStackTrace();
          return null;
        });
    } else {
      ServerPlayerEntity player = CobbleUtils.server.getPlayerManager().getPlayer(playerUUID);
      if (player != null) {
        sendMessage(player, message, prefix, typeMessage);
      }
    }
  }

  // Comando
  @Deprecated
  public static void sendMessage(ServerPlayerEntity player, String message) {
    if (message.isEmpty()) return;
    if (player == null) return;
    player.sendMessage(AdventureTranslator.toNativeWithOutPrefix(message, player));
  }

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
    CompletableFuture.runAsync(() -> {
        if (message.isEmpty()) return;

        if (CobbleUtils.config.isRedisMessaging()) {
          sendMessage(player.getUuid(), message, prefix, typeMessage);
          return;
        }

        switch (typeMessage) {
          case CHAT -> {
            if (player == null) return;
            sendMessage(player, message, prefix);
          }
          case ACTIONBAR -> {
            if (player == null) return;
            player.sendMessage(AdventureTranslator.toNative(message, prefix, player), true);
          }
          case ACTIONBAR_BROADCAST -> {
            var text = AdventureTranslator.toNative(message, prefix);
            for (ServerPlayerEntity serverPlayerEntity : CobbleUtils.server.getPlayerManager().getPlayerList()) {
              serverPlayerEntity.sendMessage(text, true);
            }
          }
          case BROADCAST -> broadcast(message, prefix);
        }
      }, MESSAGE_EXECUTOR)
      .exceptionally(e -> {
        e.printStackTrace();
        return null;
      });
  }

  @Deprecated
  public static void broadcast(String message) {
    if (!message.isEmpty()) {
      if (CobbleUtils.config.isRedisMessaging()) {
        RedisManager.sendMessage(message);
      } else {
        CobbleUtils.server.getPlayerManager().getPlayerList().forEach(player -> sendMessage(player, message));
      }
    }
  }

  @Deprecated
  public static void broadcast(String message, String prefix) {
    if (!message.isEmpty()) {
      if (CobbleUtils.config.isRedisMessaging()) {
        RedisManager.sendMessage(message, prefix);
      } else {
        var text = AdventureTranslator.toNative(message, prefix);
        CobbleUtils.server.getPlayerManager().getPlayerList().forEach(player -> {
          player.sendMessage(text);
        });
      }
    }
  }

  /**
   * Method to get the cooldown based on the player's permissions.
   *
   * @param cooldowns       The cooldowns to check.
   * @param defaultCooldown The default cooldown.
   * @param player          The player to check.
   *
   * @return The cooldown.
   */
  public static int getCooldown(Map<String, Integer> cooldowns, int defaultCooldown, ServerPlayerEntity player) {
    int cooldown = defaultCooldown;
    for (Map.Entry<String, Integer> entry : cooldowns.entrySet()) {
      if (player != null && LuckPermsUtil.checkPermission(player, entry.getKey())) {
        if (entry.getValue() < cooldown) {
          cooldown = entry.getValue();
        }
      }
    }
    return cooldown;
  }

  @Deprecated(forRemoval = true, since = "1.1.3")
  public static String getCooldown(Date date) {
    if (date == null) return CobbleUtils.language.getNocooldown();
    return getCooldown(date.getTime());
  }

  public static String getCooldown(long timestamp) {
    long time = timestamp - System.currentTimeMillis();
    if (time <= 0) return CobbleUtils.language.getNocooldown();

    long[] units = {time / (1000 * 60 * 60 * 24),
      (time / (1000 * 60 * 60)) % 24,
      (time / (1000 * 60)) % 60,
      (time / 1000) % 60};

    String[] singularLabels = {CobbleUtils.language.getDay(), CobbleUtils.language.getHour(),
      CobbleUtils.language.getMinute(), CobbleUtils.language.getSecond()};
    String[] pluralLabels = {CobbleUtils.language.getDays(), CobbleUtils.language.getHours(),
      CobbleUtils.language.getMinutes(), CobbleUtils.language.getSeconds()};

    StringBuilder result = new StringBuilder();
    for (int i = 0; i < units.length; i++) {
      if (units[i] > 0) {
        result.append(units[i] != 1
          ? pluralLabels[i].replace("%s", Long.toString(units[i]))
          : singularLabels[i].replace("%s", Long.toString(units[i])));
      }
    }

    return result.isEmpty() ? CobbleUtils.language.getNocooldown() : result.toString().trim();
  }

  public static ItemStack getHeadItem(UUID playerUUID) {
    var userCache = CobbleUtils.server.getUserCache().getByUuid(playerUUID);
    if (userCache.isPresent()) {
      ItemStack itemStack = Items.PLAYER_HEAD.getDefaultStack();
      itemStack.set(DataComponentTypes.PROFILE, new ProfileComponent(userCache.get()));
      return itemStack;
    }
    return Utils.parseItemId("minecraft:player_head");
  }

  public static ItemStack getHeadItem(ServerPlayerEntity player) {
    return getHeadItem(player.getUuid());
  }

  /**
   * Method to check if a cooldown is active.
   *
   * @param cooldown The cooldown to check.
   *
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
   *
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
   *
   * @return If the command was executed successfully
   */
  public static boolean executeCommand(String command, ServerPlayerEntity player) {
    command = command.replace("%player%", player.getGameProfile().getName());
    CommandDispatcher<ServerCommandSource> disparador = CobbleUtils.server.getCommandManager().getDispatcher();
    try {
      ServerCommandSource serverSource = CobbleUtils.server.getCommandSource();
      ParseResults<ServerCommandSource> parse = disparador.parse(command, serverSource);
      disparador.execute(parse);
      return true;
    } catch (CommandSyntaxException e) {
      System.err.println("Error to execute command: " + command);
      e.printStackTrace();
      return false;
    }
  }

  /**
   * Method to cast a PlayerEntity to a ServerPlayerEntity.
   *
   * @param player The player to cast.
   *
   * @return The ServerPlayerEntity.
   */
  public static ServerPlayerEntity castPlayer(PlayerEntity player) {
    try {
      return player instanceof ServerPlayerEntity ? (ServerPlayerEntity) player : null;
    } catch (ClassCastException e) {
      return CobbleUtils.server.getPlayerManager().getPlayer(player.getUuid());
    }
  }
}