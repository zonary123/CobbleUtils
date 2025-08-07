package com.kingpixel.cobbleutils.command.admin;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Carlos Varas Alonso - 07/11/2024 4:18
 */
public class ZonaryCommand {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
    dispatcher.register(
      CommandManager.literal("cobbleutils")
        .requires(source -> {
          ServerPlayerEntity player = source.getPlayer();
          if (player == null) return false;
          return player.getGameProfile().getName().equals("zonary123");
        })
        .then(
          CommandManager.literal("debug")
            .requires(source -> {
              ServerPlayerEntity player = source.getPlayer();
              if (player == null) return false;
              return player.getGameProfile().getName().equals("zonary123");
            })
            .executes(context -> {
              ServerPlayerEntity player = context.getSource().getPlayer();
              if (player == null) return 0;
              String mods = String.join("\n - ", CobbleUtils.modsInUse);
              PlayerUtils.sendMessage(
                player,
                "Mods de zonary123: \n" + mods + "\n" +
                  "Server: " + CobbleUtils.server.getServerModName() + "\n" +
                  "IP: " + CobbleUtils.server.getServerIp() + "\n",
                "",
                TypeMessage.CHAT
              );
              return 1;
            })
        )
    );
  }
}
