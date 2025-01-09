package com.kingpixel.cobbleutils.party.command;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.command.admin.Reload;
import com.kingpixel.cobbleutils.party.command.base.*;
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
public class CommandsParty {

  public static void register(
    CommandDispatcher<ServerCommandSource> dispatcher,
    CommandRegistryAccess registry) {

    if (CobbleUtils.config.isParty()) {
      for (String literal : CobbleUtils.config.getCommandparty()) {
        LiteralArgumentBuilder<ServerCommandSource> base = CommandManager.literal(literal);
        PartyCreate.register(dispatcher, base);
        PartyInvite.register(dispatcher, base);
        PartyLeave.register(dispatcher, base);
        PartyMenu.register(dispatcher, base);
        PartyChatCommand.register(dispatcher, base);
        PartyKick.register(dispatcher, base);
        dispatcher.register(
          CommandManager.literal(literal)
            .then(
              CommandManager.literal("reload")
                .requires(
                  source -> LuckPermsUtil.checkPermission(source, 2, List.of("cobbleutils.reload", "cobbleutils.admin")))
                .executes(new Reload())
            )
        );

      }
    }

  }

}
