package com.kingpixel.cobbleutils.command.admin;

import com.kingpixel.cobbleutils.Model.AdvancedItemChance;
import com.kingpixel.cobbleutils.config.AdvancedRewardsConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;
import java.util.List;

/**
 *
 * @author Carlos Varas Alonso - 24/05/2026 10:25
 */
public class AdvancedRewardCommand {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base.then(
        CommandManager.literal("advancedReward")
          .then(
            CommandManager.argument("id", StringArgumentType.string())
              .suggests((context, builder) -> {
                String remaining = builder.getRemaining().toLowerCase();

                AdvancedRewardsConfig.getTEMPLATE_REWARDS().keySet().forEach(id -> {
                  if (id.toLowerCase().startsWith(remaining)) {
                    builder.suggest(id);
                  }
                });

                return builder.buildFuture();
              })
              .executes(context -> {
                String id = StringArgumentType.getString(context, "id");
                run(context, List.of(context.getSource().getPlayerOrThrow()), id);
                return 1;
              })
              .then(
                CommandManager.argument("other", EntityArgumentType.players())
                  .executes(context -> {
                    Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "other");
                    String id = StringArgumentType.getString(context, "id");
                    run(context, players, id);
                    return 1;
                  })
              )
          )
      )
    );
  }

  public static void run(CommandContext<ServerCommandSource> context, Collection<ServerPlayerEntity> players, String id) {
    AdvancedItemChance reward = AdvancedRewardsConfig.getAdvancedReward(id);
    for (ServerPlayerEntity player : players) {
      reward.giveRewards(player);
    }
  }
}