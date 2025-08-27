package com.kingpixel.cobbleutils.command.admin;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.command.suggests.CobbleUtilsSuggests;
import com.kingpixel.cobbleutils.database.DataBaseFactory;
import com.kingpixel.cobbleutils.database.users.UserModel;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 07/11/2024 4:18
 */
public class UserInfoCommand {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
    dispatcher.register(
      CommandManager.literal("cobbleutils")
        .requires(source -> PermissionApi.hasPermission(source, "cobbleutils.admin", 4))
        .then(
          CommandManager.literal("UserInfo")
            .requires(source -> PermissionApi.hasPermission(source, "cobbleutils.admin", 4))
            .then(
              CommandManager.literal("name")
                .then(
                  CobbleUtilsSuggests.SUGGESTS_PLAYER_OFFLINE_AND_ONLINE
                    .suggestPlayerName("target", List.of("cobbleutils.admin", "cobbleutils.command.userinfo"), 2)
                    .executes(context -> {
                      String target = StringArgumentType.getString(context, "target");
                      UserModel user = DataBaseFactory.dataBaseUsers.findUserByName(target);
                      info(target, user, context);
                      return 1;
                    })
                )
            ).then(
              CommandManager.literal("uuid")
                .then(
                  CobbleUtilsSuggests.SUGGESTS_PLAYER_OFFLINE_AND_ONLINE
                    .suggestPlayerUUID("target", List.of("cobbleutils.admin", "cobbleutils.command.userinfo"), 2)
                    .executes(context -> {
                      UUID target = UuidArgumentType.getUuid(context, "target");
                      UserModel user = DataBaseFactory.dataBaseUsers.findUserByUUID(target);
                      info(target.toString(), user, context);
                      return 1;
                    })
                )
            )
        ).then(
          CommandManager.literal("fakeUser")
            .requires(source -> PermissionApi.hasPermission(source, "cobbleutils.admin", 4))
            .then(
              CobbleUtilsSuggests.SUGGESTS_PLAYER_OFFLINE_AND_ONLINE
                .suggestPlayerName("target", List.of("cobbleutils.admin", "cobbleutils.command.userinfo"), 2)
                .executes(context -> {
                  String target = StringArgumentType.getString(context, "target");
                  ServerPlayerEntity preTarget = CobbleUtils.server.getPlayerManager().getPlayer(target);
                  ServerPlayerEntity player = DataBaseFactory.dataBaseUsers.getPlayerOfflineOrOnline(target);
                  if (player == null) {
                    context.getSource().sendMessage(
                      Text.literal("The player " + target + " is not online or does not exist.")
                    );
                    return 0;
                  } else if (preTarget != null) {
                    context.getSource().sendMessage(
                      Text.literal("The player " + target + " is online.")
                    );
                  } else {
                    context.getSource().sendMessage(
                      Text.literal("The player " + target + " is offline.")
                    );
                  }
                  return 1;
                })

            )
        )
    );
  }

  private static void info(String target, UserModel user, CommandContext<ServerCommandSource> context) {
    if (user == null) {
      context.getSource().sendMessage(
        Text.literal("The user " + target + " does not exist in the database.")
      );
    } else {
      context.getSource().sendMessage(
        Text.literal(
          user.getUserInfo()
        )
      );
    }
  }

}
