package com.kingpixel.cobbleutils.command.admin;

import com.kingpixel.cobbleutils.Model.Location;
import com.kingpixel.cobbleutils.network.ProxyPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class TeleportCommand {

  private static final SuggestionProvider<ServerCommandSource> SUGGEST_PLAYERS =
    (context, builder) -> {
      for (ServerPlayerEntity player : context.getSource().getServer().getPlayerManager().getPlayerList()) {
        builder.suggest(player.getName().getString());
      }
      return builder.buildFuture();
    };

  private static final SuggestionProvider<ServerCommandSource> SUGGEST_WORLDS =
    (context, builder) -> {
      MinecraftServer server = context.getSource().getServer();
      for (net.minecraft.server.world.ServerWorld world : server.getWorlds()) {
        builder.suggest(world.getRegistryKey().getValue().toString());
      }
      return builder.buildFuture();
    };

  private static final SuggestionProvider<ServerCommandSource> SUGGEST_SERVERS =
    (context, builder) -> {
      builder.suggest("default");
      builder.suggest(com.kingpixel.cobbleutils.CobbleUtils.getServerName());
      return builder.buildFuture();
    };

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                              LiteralArgumentBuilder<ServerCommandSource> base) {

    dispatcher.register(
      base.then(
        CommandManager.literal("teleport")
          .requires(source -> source.hasPermissionLevel(2))
          .then(
            CommandManager.argument("player", StringArgumentType.string())
              .suggests(SUGGEST_PLAYERS)
              .then(
                CommandManager.literal("server")
                  .then(
                    CommandManager.argument("server", StringArgumentType.string())
                      .suggests(SUGGEST_SERVERS)
                      .executes(TeleportCommand::executeSimple)
                      .then(
                        CommandManager.argument("world", StringArgumentType.string())
                          .suggests(SUGGEST_WORLDS)
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
              .then(
                CommandManager.literal("local")
                  .then(
                    CommandManager.argument("world", StringArgumentType.string())
                      .suggests(SUGGEST_WORLDS)
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
                                          .executes(TeleportCommand::executeLocal)
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
    try {
      String playerName = StringArgumentType.getString(context, "player");
      String server = StringArgumentType.getString(context, "server");

      ServerPlayerEntity player = getPlayer(context, playerName);
      if (player == null) return 0;

      ProxyPacket.sendServer(player, server);
      return 1;
    } catch (Exception e) {
      context.getSource().sendError(Text.literal("Error executing teleport command: " + e.getMessage()));
      e.printStackTrace();
      return 0;
    }
  }

  private static int executeLocal(CommandContext<ServerCommandSource> context) {
    try {
      String playerName = StringArgumentType.getString(context, "player");
      String world = StringArgumentType.getString(context, "world");

      ServerPlayerEntity player = getPlayer(context, playerName);
      if (player == null) return 0;

      Location location = new Location();
      location.setServer(com.kingpixel.cobbleutils.CobbleUtils.getServerName());
      location.setWorld(world);
      location.setX(DoubleArgumentType.getDouble(context, "x"));
      location.setY(DoubleArgumentType.getDouble(context, "y"));
      location.setZ(DoubleArgumentType.getDouble(context, "z"));
      location.setYaw((float) DoubleArgumentType.getDouble(context, "yaw"));
      location.setPitch((float) DoubleArgumentType.getDouble(context, "pitch"));

      location.teleportTo(player);
      return 1;
    } catch (Exception e) {
      context.getSource().sendError(Text.literal("Error executing teleport command: " + e.getMessage()));
      e.printStackTrace();
      return 0;
    }
  }

  private static int executeFull(CommandContext<ServerCommandSource> context) {
    try {
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
    } catch (Exception e) {
      context.getSource().sendError(Text.literal("Error executing teleport command: " + e.getMessage()));
      e.printStackTrace();
      return 0;
    }
  }

  @Nullable
  private static ServerPlayerEntity getPlayer(CommandContext<ServerCommandSource> context, String name) {
    try {
      ServerPlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(name);
      if (player == null) {
        context.getSource().sendError(Text.literal("Player not found: " + name));
        return null;
      }
      return player;
    } catch (Exception e) {
      context.getSource().sendError(Text.literal("Error finding player: " + e.getMessage()));
      e.printStackTrace();
      return null;
    }
  }
}