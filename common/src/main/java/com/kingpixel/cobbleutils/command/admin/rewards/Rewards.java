package com.kingpixel.cobbleutils.command.admin.rewards;

import ca.landonjw.gooeylibs2.api.UIManager;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.ui.RewardsUI;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.LuckPermsUtil;
import com.kingpixel.cobbleutils.util.RewardsUtils;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 12/06/2024 3:47
 */
public class Rewards implements Command<ServerCommandSource> {

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                              LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base
        .executes(new Rewards())
        .then(
          CommandManager.literal("other")
            .requires(source -> LuckPermsUtil.checkPermission(source, 2, List.of("cobbleutils.storage_rewards",
              "cobbleutils" +
                ".admin")))
            .then(
              CommandManager.argument("player", EntityArgumentType.player())
                .executes(new Rewards())))
    );
  }

  @Override
  public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    if (!CobbleUtils.config.isStorageRewards()) return 0;
    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
    ServerPlayerEntity target = null;
    try {
      target = EntityArgumentType.getPlayer(context, "player");
    } catch (Exception e) {
      if (!context.getSource().isExecutedByPlayer()) {
        CobbleUtils.LOGGER.info("You must was a player to execute this command");
        return 0;
      }
    }
    if (target != null) {
      if (RewardsUtils.hasRewards(target)) {
        UIManager.openUIForcefully(target, RewardsUI.getRewards(target));
      } else {
        target.sendMessage(AdventureTranslator.toNative(CobbleUtils.language.getMessageNotHaveRewards()));
      }
      return 1;
    }
    if (RewardsUtils.hasRewards(player)) {
      UIManager.openUIForcefully(player, RewardsUI.getRewards(player));
    } else {
      player.sendMessage(AdventureTranslator.toNative(CobbleUtils.language.getMessageNotHaveRewards()));
    }
    return 1;
  }

}
