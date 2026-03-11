package com.kingpixel.cobbleutils.command.admin;

import com.kingpixel.cobbleutils.api.RewardsAPI;
import com.kingpixel.cobbleutils.command.suggests.CobbleUtilsSuggests;
import com.kingpixel.cobbleutils.model.rewards.AdvancedReward;
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
                  .suggests((context, builder) -> CommandSource.suggestMatching(RewardsAPI.getRewards().keySet(), builder))
                  .executes(context -> {
                    String playerName = StringArgumentType.getString(context, "player");
                    if (playerName == null) {
                      return 0;
                    }
                    String advancedRewardId = StringArgumentType.getString(context, "reward");
                    AdvancedReward advancedReward = RewardsAPI.getAdvancedRewardTemplate(advancedRewardId);
                    if (advancedReward == null) return 0;

                    CobbleUtilsSuggests.SUGGESTS_PLAYER_OFFLINE_AND_ONLINE.getPlayer(playerName)
                      .ifPresent(data -> advancedReward.giveRewards(data.user().getPlayerUUID()));
                    return 1;
                  })
              )
            )
        )
    );
  }
}
