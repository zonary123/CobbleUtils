package com.kingpixel.cobbleutils.api;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.LuckPermsUtil;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 06/11/2024 23:06
 */
public class PermissionApi {

  /**
   * Check if the player has the permission
   *
   * @param player     The player to check
   * @param permission The permission to check
   * @param level      The level of the permission
   *
   * @return True if the player has the permission
   */
  public static boolean hasPermission(ServerPlayerEntity player, String permission, int level) {
    if (player == null) {
      CobbleUtils.LOGGER.error("Player is null in hasPermission PermissionApi");
      return false;
    }
    return LuckPermsUtil.checkPermission(player.getCommandSource(), level, permission);
  }

  /**
   * Check if the player has the permission
   *
   * @param player      The player to check
   * @param permissions The permissions to check
   * @param level       The level of the permission
   *
   * @return True if the player has the permission
   */
  public static boolean hasPermission(ServerPlayerEntity player, List<String> permissions, int level) {
    if (player == null) {
      CobbleUtils.LOGGER.error("Player is null in hasPermission PermissionApi");
      return false;
    }
    return LuckPermsUtil.checkPermission(player.getCommandSource(), level, permissions);
  }

  /**
   * Check if the source has the permission
   *
   * @param source     The source to check
   * @param permission The permission to check
   * @param level      The level of the permission
   *
   * @return True if the source has the permission
   */
  public static boolean hasPermission(ServerCommandSource source, String permission, int level) {
    if (source == null) {
      CobbleUtils.LOGGER.error("Source is null in hasPermission PermissionApi");
      return false;
    }
    return LuckPermsUtil.checkPermission(source, level, permission);
  }

  /**
   * Check if the source has the permission
   *
   * @param source      The source to check
   * @param permissions The permissions to check
   * @param level       The level of the permission
   *
   * @return True if the source has the permission
   */
  public static boolean hasPermission(ServerCommandSource source, List<String> permissions, int level) {
    if (source == null) return false;
    return LuckPermsUtil.checkPermission(source, level, permissions);
  }
}
