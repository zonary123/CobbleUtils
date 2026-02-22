package com.kingpixel.cobbleutils.command.admin;

import com.kingpixel.cobbleutils.Model.Location;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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
                CommandManager.argument("world", StringArgumentType.string())
                  .then(coordinateArguments())
              )
          )
      )
    );
  }

  private static com.mojang.brigadier.builder.ArgumentBuilder<ServerCommandSource, ?> coordinateArguments() {
    return CommandManager.argument("x", DoubleArgumentType.doubleArg())
      .then(
        CommandManager.argument("y", DoubleArgumentType.doubleArg())
          .then(
            CommandManager.argument("z", DoubleArgumentType.doubleArg())
              .then(
                CommandManager.argument("server", StringArgumentType.string())
                  .executes(context -> execute(context, false))
                  .then(
                    CommandManager.argument("yaw", DoubleArgumentType.doubleArg())
                      .then(
                        CommandManager.argument("pitch", DoubleArgumentType.doubleArg())
                          .executes(context -> execute(context, true))
                      )
                  )
              )
          )
      );
  }

  private static int execute(
    com.mojang.brigadier.context.CommandContext<ServerCommandSource> context,
    boolean hasRotation) {

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

    if (hasRotation) {
      location.setYaw((float) DoubleArgumentType.getDouble(context, "yaw"));
      location.setPitch((float) DoubleArgumentType.getDouble(context, "pitch"));
    } else {
      location.setYaw(player.getYaw());
      location.setPitch(player.getPitch());
    }

    location.teleportTo(player);

    return 1;
  }

  private static ServerPlayerEntity getPlayer(
    com.mojang.brigadier.context.CommandContext<ServerCommandSource> context,
    String name) {

    ServerPlayerEntity player =
      context.getSource().getServer()
        .getPlayerManager()
        .getPlayer(name);

    if (player == null) {
      context.getSource().sendError(
        Text.literal("Jugador no encontrado: " + name)
      );
    }

    return player;
  }
}