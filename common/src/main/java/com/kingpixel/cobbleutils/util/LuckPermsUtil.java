package com.kingpixel.cobbleutils.util;

import com.kingpixel.cobbleutils.CobbleUtils;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.List;

public abstract class LuckPermsUtil {

  private static Permission PERMISSION_TYPE;

  private static LuckPerms luckPermsApi;

  private enum Permission {
    LUCKPERMS,
    BUKKIT_PERMISSION_API,
    FABRIC_PERMISSIONS_API,
    FORGE_PERMISSIONS_API,
    NONE
  }

  private static void setup() {
    if (PERMISSION_TYPE != null) return;
    if (haveBukkitPermissionApi()) {
      PERMISSION_TYPE = Permission.BUKKIT_PERMISSION_API;
      CobbleUtils.LOGGER.info("Bukkit permissions detected");
    } else if (haveFabricPermissionsApi()) {
      PERMISSION_TYPE = Permission.FABRIC_PERMISSIONS_API;
      CobbleUtils.LOGGER.info("Fabric permissions detected");
    } else if (haveForgePermissionApi()) {
      PERMISSION_TYPE = Permission.FORGE_PERMISSIONS_API;
      CobbleUtils.LOGGER.info("Forge permissions detected");
    } else if (getLuckPermsApi() != null) {
      PERMISSION_TYPE = Permission.LUCKPERMS;
      CobbleUtils.LOGGER.info("LuckPerms detected");
    } else {
      CobbleUtils.LOGGER.error("No permission system detected");
      PERMISSION_TYPE = Permission.NONE;
    }
  }

  private static boolean haveBukkitPermissionApi() {
    try {
      RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
      if (provider != null) {
        luckPermsApi = provider.getProvider();
      }
      if (luckPermsApi == null) {
        CobbleUtils.LOGGER.error("Error while trying to get LuckPerms provider from Bukkit");
        return false;
      }
      return true;
    } catch (IllegalStateException | NullPointerException | NoClassDefFoundError e) {
      CobbleUtils.LOGGER.error("Error while trying to get LuckPerms provider from Bukkit");
      return false;
    }
  }

  private static LuckPerms getLuckPermsApi() {
    try {
      return LuckPermsProvider.get();
    } catch (IllegalStateException | NullPointerException | NoClassDefFoundError e) {
      CobbleUtils.LOGGER.error("Error while trying to get LuckPerms provider");
      return null;
    }
  }

  private static boolean haveForgePermissionApi() {
    return false;

  }

  private static boolean haveFabricPermissionsApi() {
    try {
      return Permissions.class != null;
    } catch (NoClassDefFoundError e) {
      CobbleUtils.LOGGER.error("Error while trying to get Permissions class from Fabric");
      return false;
    }
  }

  public static boolean checkPermission(ServerCommandSource source, int level, List<String> permissions) {
    setup();
    ServerPlayerEntity player = source.getPlayer();
    return switch (PERMISSION_TYPE) {
      case LUCKPERMS -> checkLuckPermsPermission(source, permissions, level);
      case FABRIC_PERMISSIONS_API -> checkFabricPermissions(source, level, permissions);
      case BUKKIT_PERMISSION_API -> {
        for (String permission : permissions) {
          if (permission == null || permission.isEmpty()) yield true;
          if (luckPermsApi.getUserManager().getUser(player.getUuid()).getCachedData().getPermissionData().checkPermission(permission).asBoolean())
            yield true;
        }
        yield false;
      }
      default -> source.hasPermissionLevel(level);
    };
  }

  private static boolean checkFabricPermissions(ServerCommandSource source, int level, List<String> permissions) {
    if (permissions == null || permissions.isEmpty()) return true;
    for (String permission : permissions) {
      if (permission.isEmpty()) return true;
      if (Permissions.require(permission, level).test(source)) return true;
    }
    return false;
  }


  public static boolean checkLuckPermsPermission(ServerCommandSource source, List<String> permissions, int level) {
    LuckPerms luckPermsApi = getLuckPermsApi();
    if (luckPermsApi == null) return false;
    UserManager userManager = luckPermsApi.getUserManager();
    ServerPlayerEntity player = source.getPlayer();
    if (player == null) return false;
    User user = userManager.getUser(player.getUuid());
    if (user == null) return false;

    for (String permission : permissions) {
      if (permission == null || permission.isEmpty()) return true;
      return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
    }
    return source.hasPermissionLevel(level);
  }


  public static boolean checkPermission(ServerCommandSource source, int level, String permission) {
    if (permission == null || permission.isEmpty()) return true;
    return checkPermission(source, level, List.of(permission));
  }

  public static boolean checkPermission(ServerPlayerEntity player, String permission) {
    setup();
    if (permission == null || permission.isEmpty()) return true;
    return switch (PERMISSION_TYPE) {
      case LUCKPERMS -> {
        if (player == null) yield false;
        yield checkLuckPermsPermission(player.getCommandSource(), List.of(permission), 4);
      }
      case FABRIC_PERMISSIONS_API -> Permissions.require(permission, 4).test(player.getCommandSource());
      case BUKKIT_PERMISSION_API ->
        luckPermsApi.getUserManager().getUser(player.getUuid()).getCachedData().getPermissionData().checkPermission(permission).asBoolean();
      default -> false;
    };
  }

  public static boolean hasOp(ServerPlayerEntity player) {
    setup();
    return player.hasPermissionLevel(4);
  }
}
