package com.kingpixel.cobbleutils.command;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.command.admin.*;
import com.kingpixel.cobbleutils.command.admin.boss.SpawnBoss;
import com.kingpixel.cobbleutils.command.admin.egg.EggCommand;
import com.kingpixel.cobbleutils.command.admin.egg.Hatch;
import com.kingpixel.cobbleutils.command.admin.egg.IncenseCommand;
import com.kingpixel.cobbleutils.command.base.*;
import com.kingpixel.cobbleutils.command.base.shops.ShopCommand;
import com.kingpixel.cobbleutils.command.base.shops.ShopSellCommand;
import com.kingpixel.cobbleutils.command.test.TestCommands;
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

      // /cobbleutils scale <scale> <slot> and /cobbleutils scale <scale> <slot> <player>
      PokemonSize.register(dispatcher, base);

      // /cobbleutils reload
      Reload.register(dispatcher, base);

      // /cobbleutils shinytoken <player> <amount>
      if (CobbleUtils.config.isActiveshinytoken()) {
        ShinyToken.register(dispatcher, base);
      }

      // /cobbleutils pokerename <slot> <name>
      PokeRename.register(dispatcher, base);


      if (CobbleUtils.config.isBoss()) {
        // /cobbleutils boss <rarity> <coords>
        SpawnBoss.register(dispatcher, base);
      }


      if (CobbleUtils.breedconfig.isActive()) {
        // /cobbleutils breedable <slot> <breedable>
        BreedableCommand.register(dispatcher, base);

        // /cobbleutils egg <pokemon>
        EggCommand.register(dispatcher, base);

        // /cobbleutils incense <item>
        IncenseCommand.register(dispatcher, base);
        // /egginfo <slot>
        EggInfoCommand.register(dispatcher, CommandManager.literal("egginfo"));

        Hatch.register(dispatcher, CommandManager.literal("hatch"));
      }


    }

    if (CobbleUtils.breedconfig.isActive()) {
      for (String literal : CobbleUtils.breedconfig.getEggcommand()) {
        LiteralArgumentBuilder<ServerCommandSource> base = CommandManager.literal(literal)
          .requires(source -> LuckPermsUtil.checkPermission(source, 2, List.of("cobbleutils.admin", "cobbleutils.user",
            "cobbleutils.daycare")));

        // /cobbleutils egg <pokemon>
        BreedCommand.register(dispatcher, base);
      }
    }

    if (CobbleUtils.config.isShops()) {
      for (String literal : CobbleUtils.config.getCommandshop()) {
        ShopCommand.register(dispatcher, literal, CobbleUtils.shopConfig, CobbleUtils.MOD_ID, false);
        ShopSellCommand.register(dispatcher, CommandManager.literal("sell"));
      }
    }

    if (CobbleUtils.config.isDebug()) {
      LiteralArgumentBuilder<ServerCommandSource> base = CommandManager.literal("cobbleutilstest")
        .requires(source -> LuckPermsUtil.checkPermission(source, 2, List.of("cobbleutils.admin")));

      TestCommands.register(dispatcher, base);
    }

  }


}
