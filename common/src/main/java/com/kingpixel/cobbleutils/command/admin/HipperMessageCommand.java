package com.kingpixel.cobbleutils.command.admin;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.messages.HiperMessage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 07/11/2024 4:18
 */
public class HipperMessageCommand {
  private static List<String> messages = List.of(
    "chat: Hello %player:name%!",
    "broadcast: Hello everyone!",
    "actionbar: Hello %player:name%!",
    "actionbar_broadcast: Hello everyone!",
    "titlesubtitle:title: Hello %player:name%! subtitle: This is a subtitle example.",
    "titlesubtitle_broadcast:title: Hello everyone! subtitle: This is a subtitle example.",
    "bossbar: Hello %player:name%!",
    "bossbar_broadcast: Hello everyone!",
    "chat: Hello %player:name%! sound:entity.experience_orb.pickup volume:1.0 pitch:1.0 potion:minecraft:strength " +
      "duration:2000 particle:minecraft:happy_villager count:10"
  );

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, LiteralArgumentBuilder<ServerCommandSource> base) {

    dispatcher.register(
      base.then(
        CommandManager.literal("sendMessage")
          .requires(source -> source.hasPermissionLevel(2))

          // ---- Opción 1: /sendMessage <player> <message>
          .then(
            CommandManager.argument("player", EntityArgumentType.player())
              .then(
                CommandManager.argument("message", StringArgumentType.greedyString())
                  .suggests((context, builder) -> CommandSource.suggestMatching(messages, builder))
                  .executes(context -> {
                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                    String message = StringArgumentType.getString(context, "message");

                    new HiperMessage(message, null).sendMessage(
                      player.getUuid(),
                      CobbleUtils.config.getPrefix(),
                      false, false, null, null
                    );
                    return 1;
                  })
              )
          )

          // ---- Opción 2: /sendMessage <message> (al ejecutor)
          .then(
            CommandManager.argument("message", StringArgumentType.greedyString())
              .suggests((context, builder) -> CommandSource.suggestMatching(messages, builder))
              .executes(context -> {
                String message = StringArgumentType.getString(context, "message");

                ServerPlayerEntity player = context.getSource().getPlayer();

                new HiperMessage(message, null).sendMessage(
                  player == null ? null : player.getUuid(),
                  CobbleUtils.config.getPrefix(),
                  false, false, null, null
                );
                return 1;
              })
          )
      )
    );
  }


}
