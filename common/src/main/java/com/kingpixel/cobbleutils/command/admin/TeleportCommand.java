package com.kingpixel.cobbleutils.command.admin;

import com.kingpixel.cobbleutils.Model.Location;
import com.kingpixel.cobbleutils.network.ProxyPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class TeleportCommand {

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                              LiteralArgumentBuilder<ServerCommandSource> base) {

    dispatcher.register(
      base.then(
        CommandManager.literal("teleport")
          .requires(source -> source.hasPermissionLevel(2))
          .then(
            CommandManager.argument("player", StringArgumentType.string())
              .then(
                CommandManager.argument("server", StringArgumentType.string())
                  .executes(TeleportCommand::executeSimple)
                  .then(
                    CommandManager.argument("world", StringArgumentType.string())
                      .then(
                        CommandManager.argument("x", DoubleArgumentType.doubleArg())
                          .then(
                            CommandManager.argument("y", DoubleArgumentType.doubleArg())
                              .then(
                                CommandManager.argument("z", DoubleArgumentType.doubleArg())
                                  .then(
                                    CommandManager.argument("yaw", DoubleArgumentType.doubleArg())
                                      .then(
                                        CommandManager.argument("pitch", DoubleArgumentType.doubleArg())
                                          .executes(TeleportCommand::executeFull)
                                      )
                                  )
                              )
                          )
                      )
                  )
              )
          )
      )
    );
  }

  private static int executeSimple(CommandContext<ServerCommandSource> context) {

    String playerName = StringArgumentType.getString(context, "player");
    String server = StringArgumentType.getString(context, "server");

    ServerPlayerEntity player = getPlayer(context, playerName);
    if (player == null) return 0;

    ProxyPacket.sendServer(player, server);
    return 1;
  }

  private static int executeFull(CommandContext<ServerCommandSource> context) {

    String playerName = StringArgumentType.getString(context, "player");
    String world = StringArgumentType.getString(context, "world");
    String server = StringArgumentType.getString(context, "server");

    ServerPlayerEntity player = getPlayer(context, playerName);
    if (player == null) return 0;

    Location location = new Location();
    location.setServer(server);
    location.setWorld(world);

    location.setX(DoubleArgumentType.getDouble(context, "x"));
    location.setY(DoubleArgumentType.getDouble(context, "y"));
    location.setZ(DoubleArgumentType.getDouble(context, "z"));

    location.setYaw((float) DoubleArgumentType.getDouble(context, "yaw"));
    location.setPitch((float) DoubleArgumentType.getDouble(context, "pitch"));

    location.teleportTo(player);

    return 1;
  }

  private static ServerPlayerEntity getPlayer(
    CommandContext<ServerCommandSource> context,
    String name) {

    ServerPlayerEntity player =
      context.getSource().getServer()
        .getPlayerManager()
        .getPlayer(name);

    if (player == null) {
      context.getSource().sendError(
        Text.literal("Player " + name + " not found")
      );
    }

    return player;
  }
}