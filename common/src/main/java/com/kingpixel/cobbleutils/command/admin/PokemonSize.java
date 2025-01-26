package com.kingpixel.cobbleutils.command.admin;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.NoPokemonStoreException;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.CobbleUtilsTags;
import com.kingpixel.cobbleutils.Model.SizeChance;
import com.kingpixel.cobbleutils.util.LuckPermsUtil;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
public class PokemonSize implements Command<ServerCommandSource> {

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                              LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base

        .then(
          CommandManager.literal("scale")
            .requires(source ->
              LuckPermsUtil.checkPermission(source, 2, List.of("cobbleutils.admin", "cobbleutils.pokemon.size")))
            .then(CommandManager.argument("scale", FloatArgumentType.floatArg())
              .suggests((context, builder) -> {
                for (SizeChance size : CobbleUtils.config.getPokemonsizes()) {
                  builder.suggest(String.valueOf(size.getSize()));
                }
                return builder.buildFuture();
              })
              .then(CommandManager.argument("slot", IntegerArgumentType.integer(1, 6))
                .suggests((context, builder) -> {
                  for (int i = 1; i <= 6; i++) {
                    builder.suggest(String.valueOf(i));
                  }
                  return builder.buildFuture();
                })
                .executes(context -> {
                  if (!context.getSource().isExecutedByPlayer()) {
                    CobbleUtils.LOGGER.info("This command can only be executed by a player");
                    return 0;
                  }
                  ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                  int slot = IntegerArgumentType.getInteger(context, "slot");
                  float scale = FloatArgumentType.getFloat(context, "scale");
                  try {
                    scale(player, slot, scale);
                  } catch (NoPokemonStoreException e) {
                    e.printStackTrace();
                  }
                  return 1;
                })
                .then(CommandManager.argument("player", EntityArgumentType.players())
                  .executes(context -> {
                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                    int slot = IntegerArgumentType.getInteger(context, "slot");
                    float scale = FloatArgumentType.getFloat(context, "scale");
                    try {
                      scale(player, slot, scale);
                    } catch (NoPokemonStoreException e) {
                      e.printStackTrace();
                    }
                    return 1;
                  }))))));
  }

  @Override
  public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return 1;
  }

  private static void scale(ServerPlayerEntity player, int slot, float scale) throws NoPokemonStoreException {
    Pokemon pokemon = Cobblemon.INSTANCE.getStorage().getParty(player).get(--slot);
    if (pokemon == null) {
      PlayerUtils.sendMessage(player, CobbleUtils.language.getMessageNoPokemon(), CobbleUtils.config.getPrefix(), TypeMessage.CHAT);
      return;
    }
    pokemon.setScaleModifier(scale);
    pokemon.getPersistentData().putString(CobbleUtilsTags.SIZE_TAG, CobbleUtilsTags.SIZE_CUSTOM_TAG);
  }

}
