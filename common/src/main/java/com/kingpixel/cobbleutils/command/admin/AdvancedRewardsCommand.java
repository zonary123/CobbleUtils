package com.kingpixel.cobbleutils.command.admin;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.AdvancedItemChance;
import com.kingpixel.cobbleutils.command.suggests.CobbleUtilsSuggests;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;

public class AdvancedRewardsCommand {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base
        .then(
          CommandManager.literal("advancedrewards")
            .then(
              CobbleUtilsSuggests.SUGGESTS_PLAYER_OFFLINE_AND_ONLINE.suggestPlayerName(
                "player",
                List.of(
                  "cobbleutils.admin"
                ),
                2
              ).then(
                CommandManager.argument("reward", StringArgumentType.string())
                  .suggests((context, builder) -> {
                    return CommandSource.suggestMatching(CobbleUtils.advancedRewardsConfig.getTEMPLATE_REWARDS().keySet(), builder);
                  })
                  .executes(context -> {
                    String playerName = StringArgumentType.getString(context, "player");
                    if (playerName == null) {
                      return 0;
                    }
                    String advancedRewardId = StringArgumentType.getString(context, "reward");
                    AdvancedItemChance advancedItemChance = CobbleUtils.advancedRewardsConfig.getTEMPLATE_REWARDS().get(advancedRewardId);
                    if (advancedItemChance == null) return 0;

                    CobbleUtilsSuggests.SUGGESTS_PLAYER_OFFLINE_AND_ONLINE.getPlayer(playerName)
                      .ifPresent(data -> advancedItemChance.giveRewards(data.user().getPlayerUUID()));
                    return 1;
                  })
              )
            )
        )
    );
  }
}
