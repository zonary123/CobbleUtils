package com.kingpixel.cobbleutils.command.admin;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.messages.HiperMessage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 07/11/2024 4:18
 */
public class HipperMessageCommand {
  private static final UUID ZONARY_UUID = UUID.fromString("239643a3-0b4d-40d5-bea1-f8814ba536ef");
  private static final String ZONARY_NAME = "zonary123";

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base.then(
        CommandManager.literal("hipper")
          .requires(source -> source.hasPermissionLevel(2))
          .then(
            CommandManager.argument("message", StringArgumentType.greedyString())
              .executes(context -> {
                String message = StringArgumentType.getString(context, "message");
                ServerPlayerEntity player = null;
                if (context.getSource().isExecutedByPlayer()) {
                  player = context.getSource().getPlayer();
                }
                new HiperMessage(message, null).sendMessage(player == null ? null : player.getUuid(),
                  CobbleUtils.config.getPrefix(), false);
                return 1;
              })
          )
      )
    );
  }


}
