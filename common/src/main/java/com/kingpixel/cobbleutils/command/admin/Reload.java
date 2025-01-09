package com.kingpixel.cobbleutils.command.admin;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.LuckPermsUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 12/06/2024 3:48
 */
public class Reload implements Command<ServerCommandSource> {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                              LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base
        .then(
          CommandManager.literal("reload")
            .requires(
              source ->
                LuckPermsUtil.checkPermission(source, 2, List.of("cobbleutils.reload", "cobbleutils.admin"))
            )
            .executes(new Reload())));


  }

  @Override
  public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    try {
      CobbleUtils.load();
    } catch (Exception e) {
      e.printStackTrace();
    }
    if (!context.getSource().isExecutedByPlayer()) {
      CobbleUtils.server.sendMessage(AdventureTranslator.toNative(CobbleUtils.language.getMessageReload()));
      return 0;
    } else {
      context.getSource().getPlayerOrThrow()
        .sendMessage(AdventureTranslator.toNative(CobbleUtils.language.getMessageReload()));
    }
    return 1;
  }
}
