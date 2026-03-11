package com.kingpixel.cobbleutils.command.base;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.api.PermissionAPI;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 26/07/2024 14:14
 */
public class PokeShoutAllMe implements Command<ServerCommandSource> {

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                              LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base
        .requires(source -> PermissionAPI.hasPermission(source, List.of("cobbleutils.pokeshoutplusall",
          "cobbleutils" +
            ".user"), 2))
        .executes(context -> {
          if (!context.getSource().isExecutedByPlayer()) {
            CobbleUtils.LOGGER.error("This command can only be executed by a player");
            return 0;
          }
          ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
          long cooldown = PokeShout.cooldowns.getOrDefault(player.getUuid(), 0L);
          if (PlayerUtils.isCooldown(cooldown)) {
            PlayerUtils.sendMessage(player, CobbleUtils.language.getMessageCooldownCommand()
                .replace("%cooldown%", String.valueOf(PlayerUtils.getCooldown(cooldown))),
              CobbleUtils.config.getPrefix(),
              TypeMessage.CHAT
            );
            return 0;
          }
          PokeShout.cooldowns.put(player.getUuid(), System.currentTimeMillis() + CobbleUtils.config.getCooldownpokeshout().toMillis());
          PlayerPartyStore playerPartyStore = Cobblemon.INSTANCE.getStorage().getParty(player);
          if (playerPartyStore.size() == 0) {
            player.sendMessage(
              AdventureTranslator.toNative(
                CobbleUtils.language.getMessageNoPokemon()
              )
            );
            return 0;
          }
          playerPartyStore.forEach(pokemon -> {
            if (CobbleUtils.config.isRedisMessaging()) {
              PlayerUtils.sendMessage(player, PokeShout.getMessageString(player, pokemon), "", TypeMessage.CHAT);
            } else {
              player.sendMessage(PokeShout.getMessage(player, pokemon));
            }
          });
          return 1;
        }));
  }

  @Override
  public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return 0;
  }

}