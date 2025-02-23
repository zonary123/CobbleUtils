package com.kingpixel.cobbleutils.util;

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

/**
 * @author Carlos Varas Alonso - 28/06/2024 20:44
 */
public class PlayerUtils {
  // Comando
  @Deprecated
  public static void sendMessage(ServerPlayerEntity player, String message) {
    if (!message.isEmpty()) {
      player.sendMessage(AdventureTranslator.toNativeWithOutPrefix(message, player));
    }
  }

  @Deprecated
  public static void sendMessage(ServerPlayerEntity player, String message, String prefix) {
    if (message != null && !message.isEmpty()) {
      player.sendMessage(AdventureTranslator.toNative(message, prefix, player));
    }
  }

  @Deprecated
  public static void sendMessage(ServerPlayerEntity player, String message, String prefix, boolean broadcast) {
    if (broadcast) {
      broadcast(message, prefix);
    } else {
      sendMessage(player, message, prefix);
    }
  }

  public static void sendMessage(ServerPlayerEntity player, String message, String prefix, TypeMessage typeMessage) {
    switch (typeMessage) {
      case CHAT -> sendMessage(player, message, prefix);
      case ACTIONBAR -> player.sendMessage(AdventureTranslator.toNative(message, prefix, player), true);
      case BROADCAST -> broadcast(message, prefix);
    }
  }

  public static void broadcast(String message) {
    if (!message.isEmpty()) {
      CobbleUtils.server.getPlayerManager().getPlayerList().forEach(player -> sendMessage(player, message));
    }
  }

  public static void broadcast(String message, String prefix) {
    if (!message.isEmpty()) {
      CobbleUtils.server.getPlayerManager().getPlayerList().forEach(player -> sendMessage(player, message, prefix));
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

  public static String getCooldown(Date date) {
    if (date == null) {
      return CobbleUtils.language.getNocooldown();
    }
    long time = date.getTime() - new Date().getTime();
    long seconds = time / 1000;
    long minutes = seconds / 60;
    long hours = minutes / 60;
    long days = hours / 24;

    long remainingHours = hours % 24;
    long remainingMinutes = minutes % 60;
    long remainingSeconds = seconds % 60;

    StringBuilder result = new StringBuilder();

    if (days > 0) {
      String dayString = days != 1 ? CobbleUtils.language.getDays().replace("%s", String.valueOf(days))
        : CobbleUtils.language.getDay().replace("%s", String.valueOf(days));
      result.append(dayString);
    }
    if (remainingHours > 0) {
      String hourString = remainingHours != 1 ? CobbleUtils.language.getHours().replace("%s",
        String.valueOf(remainingHours))
        : CobbleUtils.language.getHour().replace("%s", String.valueOf(remainingHours));
      result.append(hourString);
    }
    if (remainingMinutes > 0) {
      String minuteString = remainingMinutes != 1 ? CobbleUtils.language.getMinutes().replace("%s",
        String.valueOf(remainingMinutes))
        : CobbleUtils.language.getMinute().replace("%s",
        String.valueOf(remainingMinutes));
      result.append(minuteString);
    }
    if (remainingSeconds > 0) {
      String secondString = remainingSeconds != 1 ? CobbleUtils.language.getSeconds().replace("%s",
        String.valueOf(remainingSeconds))
        : CobbleUtils.language.getSecond().replace("%s",
        String.valueOf(remainingSeconds));
      result.append(secondString);
    }
    if (result.isEmpty())
      return CobbleUtils.language.getNocooldown();

    return result.toString().trim();
  }

  public static ItemStack getHeadItem(ServerPlayerEntity player) {
    if (player != null) {
      ItemStack itemStack = Items.PLAYER_HEAD.getDefaultStack();
      itemStack.set(DataComponentTypes.PROFILE, new ProfileComponent(player.getGameProfile()));
      return itemStack;
    }
    return Utils.parseItemId("minecraft:player_head");
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
