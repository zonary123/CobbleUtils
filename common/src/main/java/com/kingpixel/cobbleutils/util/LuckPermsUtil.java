package com.kingpixel.cobbleutils.util;

import com.kingpixel.cobbleutils.CobbleUtils;
import dev.ftb.mods.ftbranks.api.FTBRanksAPI;
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
    FTB_RANKS,
    NONE
  }

  private static void setup() {
    if (PERMISSION_TYPE != null) return;
    if (haveFabricPermissionsApi()) {
      PERMISSION_TYPE = Permission.FABRIC_PERMISSIONS_API;
      CobbleUtils.LOGGER.info("Fabric permissions detected");
    } else if (haveBukkitPermissionApi()) {
      PERMISSION_TYPE = Permission.BUKKIT_PERMISSION_API;
      CobbleUtils.LOGGER.info("Bukkit permissions detected");
    } else if (getLuckPermsApi() != null) {
      PERMISSION_TYPE = Permission.LUCKPERMS;
      CobbleUtils.LOGGER.info("LuckPerms detected");
    } else if (haveFTBRanksApi()) {
      PERMISSION_TYPE = Permission.FTB_RANKS;
      CobbleUtils.LOGGER.info("FTB Ranks detected");
    } else {
      CobbleUtils.LOGGER.error("No permission system detected");
      PERMISSION_TYPE = Permission.NONE;
    }
  }

  private static boolean haveFTBRanksApi() {
    try {
      return FTBRanksAPI.getInstance() != null;
    } catch (IllegalStateException | NullPointerException | NoClassDefFoundError | NoSuchMethodError e) {
      CobbleUtils.LOGGER.error("Error while trying to get FTB Ranks class");
      return false;
    }
  }

  private static LuckPerms getLuckPermsApi() {
    try {
      CobbleUtils.LOGGER.info("Trying to get LuckPerms provider");
      luckPermsApi = LuckPermsProvider.get();
      return luckPermsApi;
    } catch (IllegalStateException | NullPointerException | NoClassDefFoundError | NoSuchMethodError e) {
      CobbleUtils.LOGGER.error("Error while trying to get LuckPerms provider");
      return null;
    }
  }

  private static boolean haveFabricPermissionsApi() {
    try {
      CobbleUtils.LOGGER.info("Trying to get Permissions class from Fabric");
      return Permissions.class != null;
    } catch (IllegalStateException | NullPointerException | NoClassDefFoundError | NoSuchMethodError e) {
      CobbleUtils.LOGGER.error("Error while trying to get Permissions class from Fabric");
      return false;
    }
  }

  private static boolean haveBukkitPermissionApi() {
    try {
      CobbleUtils.LOGGER.info("Trying to get LuckPerms provider from Bukkit");
      RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
      if (provider != null) {
        luckPermsApi = provider.getProvider();
      }
      if (luckPermsApi == null) {
        CobbleUtils.LOGGER.error("Error while trying to get LuckPerms provider from Bukkit");
        return false;
      }
      return true;
    } catch (IllegalStateException | NullPointerException | NoClassDefFoundError | NoSuchMethodError e) {
      CobbleUtils.LOGGER.error("Error while trying to get LuckPerms provider from Bukkit");
      return false;
    }
  }


  public static boolean checkPermission(ServerCommandSource source, int level, List<String> permissions) {
    setup();

    return switch (PERMISSION_TYPE) {
      case LUCKPERMS, BUKKIT_PERMISSION_API -> checkLuckPermsPermission(source, permissions, level);
      case FABRIC_PERMISSIONS_API -> checkFabricPermissions(source, level, permissions);
      case FTB_RANKS -> {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) yield false;
        for (String permission : permissions) {
          if (permission == null || permission.isEmpty()) yield true;
          if (FTBRanksAPI.getPermissionValue(player, permission).asBooleanOrFalse()) {
            yield true;
          }
        }
        yield source.hasPermissionLevel(level == 4 ? 2 : level);
      }
      default -> source.hasPermissionLevel(level);
    };
  }

  private static boolean checkFabricPermissions(ServerCommandSource source, int level, List<String> permissions) {
    if (permissions == null || permissions.isEmpty()) return true;
    boolean hasPermission = false;
    for (String permission : permissions) {
      if (permission.isEmpty()) return true;
      if (Permissions.require(permission, level).test(source)) {
        hasPermission = true;
      }
    }
    return hasPermission;
  }


  public static boolean checkLuckPermsPermission(ServerCommandSource source, List<String> permissions, int level) {
    setup();
    if (luckPermsApi == null) {
      CobbleUtils.LOGGER.error("LuckPerms not found");
      return false;
    }
    UserManager userManager = luckPermsApi.getUserManager();
    ServerPlayerEntity player = source.getPlayer();
    if (player == null) return source.hasPermissionLevel(level);

    User user = userManager.getUser(player.getUuid());
    if (user == null) {
      CobbleUtils.LOGGER.error("User not found in LuckPerms");
      return false;
    }
    boolean hasPermission = false;
    for (String permission : permissions) {
      if (permission == null || permission.isEmpty()) return true;
      if (user.getCachedData().getPermissionData().checkPermission(permission).asBoolean()) {
        hasPermission = true;
      }
    }
    if (hasPermission) return true;
    return source.hasPermissionLevel(level == 4 ? 2 : level);
  }


  public static boolean checkPermission(ServerCommandSource source, int level, String permission) {
    if (permission == null || permission.isEmpty()) return true;
    return checkPermission(source, level, List.of(permission));
  }

  public static boolean checkPermission(ServerPlayerEntity player, String permission) {
    setup();
    if (permission == null || permission.isEmpty()) return true;
    return switch (PERMISSION_TYPE) {
      case LUCKPERMS, BUKKIT_PERMISSION_API ->
        checkLuckPermsPermission(player.getCommandSource(), List.of(permission), 2);
      case FABRIC_PERMISSIONS_API -> Permissions.require(permission, 2).test(player.getCommandSource());
      case FTB_RANKS -> {
        if (FTBRanksAPI.getPermissionValue(player, permission).asBooleanOrFalse()) {
          yield true;
        }
        yield player.hasPermissionLevel(2);
      }
      default -> player.hasPermissionLevel(2);
    };
  }

  public static Object getMetaData(ServerPlayerEntity player, String permission) {
    return switch (PERMISSION_TYPE) {
      case LUCKPERMS, BUKKIT_PERMISSION_API -> {
        if (luckPermsApi == null) {
          CobbleUtils.LOGGER.error("LuckPerms not found");
          yield 0; // Esto se convierte en Integer por autoboxing
        }
        UserManager userManager = luckPermsApi.getUserManager();
        User user = userManager.getUser(player.getUuid());
        if (user == null) {
          CobbleUtils.LOGGER.error("User not found in LuckPerms");
          yield 0;
        }

        var metaData = user.getCachedData().getMetaData().getMetaValue(permission);

        if (metaData == null) yield null;

        if (CobbleUtils.config.isDebug()) {
          CobbleUtils.LOGGER.info("MetaData for permission " + permission + " is " + metaData);
        }

        // Intentar detectar tipo
        try {
          if (metaData.contains(".")) {
            yield Double.parseDouble(metaData); // retorna Double
          } else {
            yield Integer.parseInt(metaData); // retorna Integer
          }
        } catch (NumberFormatException e) {
          yield metaData; // retorna String si no es un número
        }
      }
      default -> null;
    };
  }
}
