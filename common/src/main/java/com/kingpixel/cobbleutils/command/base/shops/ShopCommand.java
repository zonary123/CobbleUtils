package com.kingpixel.cobbleutils.command.base.shops;

import ca.landonjw.gooeylibs2.api.UIManager;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.config.ShopConfig;
import com.kingpixel.cobbleutils.features.shops.Shop;
import com.kingpixel.cobbleutils.features.shops.ShopConfigMenu;
import com.kingpixel.cobbleutils.features.shops.models.Product;
import com.kingpixel.cobbleutils.features.shops.models.types.ShopType;
import com.kingpixel.cobbleutils.features.shops.models.types.ShopTypeDynamic;
import com.kingpixel.cobbleutils.features.shops.models.types.ShopTypeDynamicWeekly;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.LuckPermsUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.UUID;

import static com.kingpixel.cobbleutils.command.base.shops.ShopTransactionCommand.getTransactionPlayer;
import static com.kingpixel.cobbleutils.command.base.shops.ShopTransactionCommand.getTransactionsPlayers;

public class ShopCommand implements Command<ServerCommandSource> {

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                              String command,
                              ShopConfig shopConfig,
                              String mod_id, boolean api) {
    LiteralArgumentBuilder<ServerCommandSource> base = CommandManager.literal(command);
    if (api) {
      base = base.then(getBase(CommandManager.literal("shop"), shopConfig, mod_id));
    } else {
      base = getBase(base, shopConfig, mod_id);
    }

    dispatcher.register(base);
  }

  private static LiteralArgumentBuilder<ServerCommandSource> getBase(LiteralArgumentBuilder<ServerCommandSource> base, ShopConfig shopConfig, String mod_id) {
    return base
      .requires(source -> LuckPermsUtil.checkPermission(source, 0, List.of(mod_id + ".admin", mod_id + ".shop", mod_id + ".user")))
      .executes(context -> executeOpenConfigMenu(context, shopConfig, mod_id))
      .then(CommandManager.literal("shops")
        .requires(source -> LuckPermsUtil.checkPermission(source, 2, List.of(mod_id + ".admin", mod_id + ".shop.shops")))
        .then(CommandManager.argument("shop", StringArgumentType.string())
          .suggests((context, builder) -> {
            ShopConfigMenu.getShopsMod(mod_id).forEach(shop -> {
              if (context.getSource().isExecutedByPlayer() &&
                LuckPermsUtil.checkPermission(context.getSource(), 2, List.of(mod_id + ".admin", mod_id + ".shop." + shop.getId()))) {
                builder.suggest(shop.getId());
              }
            });
            return builder.buildFuture();
          })
          .executes(context -> executeOpenShop(context, shopConfig, mod_id, false))
          .then(CommandManager.argument("player", EntityArgumentType.player())
            .requires(source -> LuckPermsUtil.checkPermission(source, 2, List.of(mod_id + ".admin", mod_id + ".shopother")))
            .executes(context -> executeOpenShopForOtherPlayer(context, shopConfig, mod_id))
            .then(CommandManager.argument("close", StringArgumentType.string())
              .suggests((context, builder) -> {
                builder.suggest("true");
                builder.suggest("false");
                return builder.buildFuture();
              })
              .executes(context -> executeOpenShopWithClose(context, shopConfig, mod_id))
            )
          )
          .then(CommandManager.literal("addProduct")
            .executes(context -> executeAddProduct(context, shopConfig, mod_id))
          )
        )
      )
      .then(CommandManager.literal("reload")
        .requires(source -> LuckPermsUtil.checkPermission(source, 2, List.of(mod_id + ".admin", mod_id + ".shopreload")))
        .executes(context -> executeReload(context))
      )
      .then(CommandManager.literal("resetDynamics")
        .requires(source -> LuckPermsUtil.checkPermission(source, 2, List.of(mod_id + ".admin", mod_id + ".shopresetdynamics")))
        .then(CommandManager.argument("shop", StringArgumentType.string())
          .suggests((context, builder) -> {
            ShopConfigMenu.getShopsMod(mod_id).forEach(shop -> {
              if (shop.getShopType().getTypeShop() == ShopType.TypeShop.DYNAMIC ||
                shop.getShopType().getTypeShop() == ShopType.TypeShop.DYNAMIC_WEEKLY) {
                builder.suggest(shop.getId());
              }
            });
            return builder.buildFuture();
          })
          .executes(context -> executeResetDynamics(context, mod_id))
        )
      ).then(CommandManager.literal("transactions")
        .requires(source -> LuckPermsUtil.checkPermission(
          source, 2, List.of("cobbleutils.admin", "cobbleutils.shoptransactions")
        ))
        .executes(context -> {
          if (!context.getSource().isExecutedByPlayer()) {
            return 0;
          }
          ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
          UIManager.openUIForcefully(player, getTransactionsPlayers(player, CobbleUtils.shopConfig.getShopConfigMenu()));
          return 1;
        })
        .then(CommandManager.argument("player", EntityArgumentType.player())
          .executes(context -> {
            if (!context.getSource().isExecutedByPlayer()) {
              return 0;
            }
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            UUID targetUUID = EntityArgumentType.getPlayer(context, "player").getUuid();
            UIManager.openUIForcefully(player, getTransactionPlayer(player, targetUUID, CobbleUtils.shopConfig.getShopConfigMenu()));
            return 1;
          })
        )
      );
  }

  private static int executeOpenConfigMenu(CommandContext<ServerCommandSource> context, ShopConfig shopConfig, String mod_id) {
    try {
      if (!context.getSource().isExecutedByPlayer()) {
        CobbleUtils.LOGGER.error("This command can only be executed by a player");
        return 0;
      }
      ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER.info("Opening shop config menu");
      }
      ShopConfigMenu.open(player, shopConfig, mod_id, false);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return 1;
  }

  private static int executeOpenShop(CommandContext<ServerCommandSource> context, ShopConfig shopConfig, String mod_id, boolean isForOtherPlayer) throws CommandSyntaxException {
    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
    String shop = StringArgumentType.getString(context, "shop");
    if (LuckPermsUtil.checkPermission(player, mod_id + ".shop." + shop)) {
      shopConfig.getShopConfigMenu().open(player, shop, shopConfig, mod_id, isForOtherPlayer);
      return 1;
    } else {
      player.sendMessage(
        AdventureTranslator.toNative(
          CobbleUtils.shopLang.getMessageNotHavePermission()
            .replace("%prefix%", CobbleUtils.language.getPrefixShop())
        )
      );
      return 0;
    }
  }

  private static int executeOpenShopForOtherPlayer(CommandContext<ServerCommandSource> context, ShopConfig shopConfig, String mod_id) throws CommandSyntaxException {
    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
    String shop = StringArgumentType.getString(context, "shop");
    shopConfig.getShopConfigMenu().open(player, shop, shopConfig, mod_id, true);
    return 1;
  }

  private static int executeOpenShopWithClose(CommandContext<ServerCommandSource> context, ShopConfig shopConfig, String mod_id) throws CommandSyntaxException {
    try {
      ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
      String shop = StringArgumentType.getString(context, "shop");
      boolean close = Boolean.parseBoolean(StringArgumentType.getString(context, "close"));
      shopConfig.getShopConfigMenu().open(player, shop, shopConfig, mod_id, close);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return 1;
  }

  private static int executeAddProduct(CommandContext<ServerCommandSource> context, ShopConfig shopConfig, String mod_id) throws CommandSyntaxException {
    String shop = StringArgumentType.getString(context, "shop");
    ShopAddFuntionality.open(context.getSource().getPlayerOrThrow(), shopConfig, mod_id, shop, new Product());
    return 1;
  }

  private static int executeReload(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    CobbleUtils.load();
    if (!context.getSource().isExecutedByPlayer()) {
      CobbleUtils.LOGGER.info("Reloaded");
    } else {
      context.getSource().getPlayer().sendMessage(
        AdventureTranslator.toNative(
          CobbleUtils.language.getMessageReload()
            .replace("%prefix%", CobbleUtils.language.getPrefixShop())
        )
      );
    }
    return 1;
  }

  private static int executeResetDynamics(CommandContext<ServerCommandSource> context, String mod_id) throws CommandSyntaxException {
    String shopId = StringArgumentType.getString(context, "shop");
    Shop shop = ShopConfigMenu.getShop(mod_id, shopId);
    if (shop.getShopType() instanceof ShopTypeDynamic shopTypeDynamic) {
      shopTypeDynamic.replenish(shop);
    } else if (shop.getShopType() instanceof ShopTypeDynamicWeekly shopTypeDynamicWeekly) {
      shopTypeDynamicWeekly.replenish(shop);
    }
    return 1;
  }

  @Override
  public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return 0;
  }
}