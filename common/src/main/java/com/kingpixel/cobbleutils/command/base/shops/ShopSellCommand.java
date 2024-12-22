package com.kingpixel.cobbleutils.command.base.shops;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.features.shops.ShopConfigMenu;
import com.kingpixel.cobbleutils.features.shops.ShopSell;
import com.kingpixel.cobbleutils.util.LuckPermsUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 13/08/2024 18:53
 */
public class ShopSellCommand implements Command<ServerCommandSource> {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                              LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base
        .requires(source -> LuckPermsUtil.checkPermission(
          source, 0, List.of("cobbleutils.admin", "cobbleutils.shop.sell",
            "cobbleutils.user")
        ))
        .then(
          CommandManager.literal("all")
            .executes(context -> {
              if (!context.getSource().isExecutedByPlayer()) {
                return 0;
              }
              ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
              ShopConfigMenu shopConfigMenu = CobbleUtils.shopConfig.getShopConfigMenu();
              ShopSell.sellProducts(player, shopConfigMenu);
              return 1;
            })
        ).then(
          CommandManager.literal("hand")
            .executes(context -> {
              if (!context.getSource().isExecutedByPlayer()) {
                return 0;
              }
              ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
              ShopConfigMenu shopConfigMenu = CobbleUtils.shopConfig.getShopConfigMenu();
              ShopSell.sellProductHand(player, shopConfigMenu);
              return 1;
            })
        ).then(
          CommandManager.literal("menu")
            .executes(context -> {
              if (!context.getSource().isExecutedByPlayer()) {
                return 0;
              }
              ServerPlayerEntity player = context.getSource().getPlayerOrThrow();

              return 0;
            })
        )
    );
  }

  @Override public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return 0;
  }
}
