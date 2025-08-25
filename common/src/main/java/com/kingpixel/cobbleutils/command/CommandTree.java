package com.kingpixel.cobbleutils.command;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.command.admin.*;
import com.kingpixel.cobbleutils.command.base.PokeShout;
import com.kingpixel.cobbleutils.command.base.PokeShoutAll;
import com.kingpixel.cobbleutils.command.base.PokeShoutAllMe;
import com.kingpixel.cobbleutils.command.base.PokeShoutMe;
import com.kingpixel.cobbleutils.util.LuckPermsUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 10/06/2024 14:08
 */
public class CommandTree {

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registry) {

    if (!CobbleUtils.config.getPokeshout().isEmpty()) {
      PokeShout.register(dispatcher, CommandManager.literal(CobbleUtils.config.getPokeshout()));
      PokeShoutMe.register(dispatcher, CommandManager.literal(CobbleUtils.config.getPokeshout() + "me"));
    }
    if (!CobbleUtils.config.getPokeshoutall().isEmpty()) {
      PokeShoutAll.register(dispatcher, CommandManager.literal(CobbleUtils.config.getPokeshoutall()));
      PokeShoutAllMe.register(dispatcher, CommandManager.literal(CobbleUtils.config.getPokeshoutall() + "me"));
    }


    for (String literal : CobbleUtils.config.getCommmandplugin()) {
      LiteralArgumentBuilder<ServerCommandSource> base = CommandManager.literal(literal).requires(source ->
        LuckPermsUtil.checkPermission(source, 2, List.of("cobbleutils.admin")));

      ModRewardsCommand.register(dispatcher, base);

      WikiCommand.register(dispatcher, base, "https://zonary123-dev.gitbook.io/zonary123-dev-docs/mods/cobbleutils");

      // /cobbleutils reload
      Reload.register(dispatcher, base);

      // /cobbleutils shinytoken <player> <amount>
      if (CobbleUtils.config.isActiveshinytoken()) {
        ShinyToken.register(dispatcher, base);
      }

      // /cobbleutils pokerename <slot> <name>
      PokeRename.register(dispatcher, base);

      EconomyIdCommand.register(dispatcher, base);

      BlockCommand.register(dispatcher, base);
    }
    ZonaryCommand.register(dispatcher);
  }


}
