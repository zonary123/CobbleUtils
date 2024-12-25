package com.kingpixel.cobbleutils.command.admin.random;

import com.cobblemon.mod.common.api.storage.NoPokemonStoreException;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.CobbleUtilities;
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

/**
 * @author Carlos Varas Alonso - 13/06/2024 10:11
 */
public class RandomPokemon implements Command<ServerCommandSource> {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                              LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base.requires(source -> LuckPermsUtil.checkPermission(source, 2, List.of("cobbleutils.admin")))
        .then(
          CommandManager.literal("givepoke")
            .requires(source -> LuckPermsUtil.checkPermission(source, 2, List.of("cobbleutils.givepokemon",
              "cobbleutils" +
                ".admin")))
            .then(
              CommandManager.argument("type", StringArgumentType.string())
                .suggests(
                  (context, builder) -> {
                    CobbleUtils.poolPokemons.getRandompokemons()
                      .forEach((key, value) -> builder.suggest(key));
                    return builder.buildFuture();
                  })

                .then(
                  CommandManager.argument("player", EntityArgumentType.player())
                    .executes(new RandomPokemon()))

            )));
  }

  @Override
  public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
    String type = StringArgumentType.getString(context, "type");
    if (CobbleUtils.config.isDebug())
      CobbleUtils.LOGGER.info("RandomPokemon command");
    try {
      CobbleUtilities.giveRandomPokemon(player, type);
    } catch (NoPokemonStoreException e) {
      e.printStackTrace();
    }

    return 1;
  }
}
