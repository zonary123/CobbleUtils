package com.kingpixel.cobbleutils.util;

import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Carlos Varas Alonso - 10/06/2024 21:09
 */
public class CobbleUtilities {

  public static String getNameItem(String id) {
    ItemStack itemStack = Utils.parseItemId(id);
    Item item = itemStack.getItem();
    return item.getName(itemStack).getString();
  }


  /**
   * Convert seconds to time
   *
   * @param totalSeconds The total seconds
   *
   * @return The time in a string
   */
  public static String convertSecondsToTime(int totalSeconds) {
    long seconds = totalSeconds;
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

  public static GooeyButton fillItem() {
    return GooeyButton.builder().display(Utils.parseItemId(CobbleUtils.config.getFill())).build();
  }

  public static boolean executeCommand(ServerPlayerEntity player, String command) {
    return executeCommand(command.replace("%player%", player.getGameProfile().getName()));
  }

  /**
   * Execute a command
   *
   * @param command The command to execute
   *
   * @return If the command was executed successfully
   */
  public static boolean executeCommand(String command) {
    try {
      CommandDispatcher<ServerCommandSource> disparador = CobbleUtils.server.getCommandManager().getDispatcher();
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
   * Get an ItemStack from a string with NBT
   *
   * @param item The item with NBT
   *
   * @return The ItemStack
   */
  public static ItemStack getItem(String item) {
    try {
      ItemStack itemStack = ItemStack.EMPTY;
      NbtComponent nbtComponent = NbtComponent.of(NbtHelper.fromNbtProviderString(item));
      itemStack.set(DataComponentTypes.CUSTOM_DATA, nbtComponent);
      return itemStack;
    } catch (Exception e) {
      CobbleUtils.LOGGER.fatal("Failed to parse item for NBT: " + item);
      CobbleUtils.LOGGER.fatal("Stacktrace: ");
    }
    /*try {
      return ItemStack.fromNbt(NbtHelper.fromNbtProviderString(item));
    } catch (CommandSyntaxException e) {
      CobbleUtils.LOGGER.fatal("Failed to parse item for NBT: " + item);
      CobbleUtils.LOGGER.fatal("Stacktrace: ");
      e.printStackTrace();
    }*/
    return ItemStack.EMPTY;
  }
}
