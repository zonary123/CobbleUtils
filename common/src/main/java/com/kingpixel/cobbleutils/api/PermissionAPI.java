package com.kingpixel.cobbleutils.api;

import com.kingpixel.cobbleutils.util.LuckPermsUtil;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 06/11/2024 23:06
 */
public class PermissionAPI {

  /**
   * Check if the player has the permission
   *
   * @param player     The player to check
   * @param permission The permission to check
   * @param level      The level of the permission
   * @return True if the player has the permission
   */
  public static boolean hasPermission(ServerPlayerEntity player, String permission, int level) {
    if (player == null) return false;
    return LuckPermsUtil.checkPermission(player, level, permission);
  }

  /**
   * Check if the player has the permission
   *
   * @param player      The player to check
   * @param permissions The permissions to check
   * @param level       The level of the permission
   * @return True if the player has the permission
   */
  public static boolean hasPermission(ServerPlayerEntity player, List<String> permissions, int level) {
    if (player == null) return false;
    return LuckPermsUtil.checkPermission(player, level, permissions);
  }

  public static boolean hasPermission(UUID playerUUID, String permission, int level) {
    return LuckPermsUtil.checkPermission(playerUUID, level, permission);
  }

  /**
   * Check if the source has the permission
   *
   * @param source     The source to check
   * @param permission The permission to check
   * @param level      The level of the permission
   * @return True if the source has the permission
   */
  public static boolean hasPermission(ServerCommandSource source, String permission, int level) {
    if (source == null) return false;
    return LuckPermsUtil.checkPermission(source, level, permission);
  }

  /**
   * Check if the source has the permission
   *
   * @param source      The source to check
   * @param permissions The permissions to check
   * @param level       The level of the permission
   * @return True if the source has the permission
   */
  public static boolean hasPermission(ServerCommandSource source, List<String> permissions, int level) {
    if (source == null) return false;
    return LuckPermsUtil.checkPermission(source, level, permissions);
  }

  /**
   * Get the data of the player
   *
   * @param player     The player to get the data
   * @param permission The permission to get the data
   * @return The data of the player
   */
  public static Object getMetaData(ServerPlayerEntity player, String permission) {
    return LuckPermsUtil.getMetaData(player, permission);
  }
}
