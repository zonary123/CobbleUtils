package com.kingpixel.cobbleutils.command.base;

import com.cobblemon.mod.common.command.argument.PartySlotArgumentType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.util.*;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Carlos Varas Alonso - 26/07/2024 14:14
 */
public class PokeShout implements Command<CommandSource> {
  public static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                              LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base
        .requires(source -> PermissionApi.hasPermission(source, List.of("cobbleutils.pokeshoutplus",
          "cobbleutils" +
            ".user"), 2))
        .then(
          CommandManager.argument("slot", PartySlotArgumentType.Companion.partySlot())
            .executes(context -> {
              if (!context.getSource().isExecutedByPlayer()) {
                CobbleUtils.LOGGER.error("This command can only be executed by a player");
                return 0;
              }
              Pokemon pokemon = PartySlotArgumentType.Companion.getPokemon(context, "slot");
              ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
              if (player == null) return 0;
              if (pokemon == null) {
                PlayerUtils.sendMessage(player,
                  CobbleUtils.language.getMessageNoPokemon(),
                  CobbleUtils.config.getPrefix(),
                  TypeMessage.CHAT
                );
                return 0;
              }
              long cooldown = cooldowns.getOrDefault(player.getUuid(), 0L);
              if (PlayerUtils.isCooldown(cooldown)) {
                PlayerUtils.sendMessage(player, CobbleUtils.language.getMessageCooldownCommand()
                    .replace("%cooldown%", String.valueOf(PlayerUtils.getCooldown(cooldown))),
                  CobbleUtils.config.getPrefix(),
                  TypeMessage.CHAT
                );
                return 0;
              }
              cooldowns.put(player.getUuid(),
                System.currentTimeMillis() + CobbleUtils.config.getCooldownpokeshout().toMillis());
              if (CobbleUtils.config.isRedisMessaging()) {
                Utils.broadcastMessage(getMessageString(player, pokemon));
              } else {
                Utils.broadcastMessage(getMessage(player, pokemon));
              }
              return 1;
            })));
  }

  @Override
  public int run(CommandContext<CommandSource> context) throws CommandSyntaxException {
    return 0;
  }

  public static String getMessageString(ServerPlayerEntity player, Pokemon pokemon) {
    String baseMessage = CobbleUtils.language.getMessagePokeShout();
    String playerName = player.getGameProfile().getName();
    String messageContent = PokemonUtils.replace(baseMessage, pokemon).replace("%player%", playerName);

    String hoverInfo = String.join("<br>", PokemonUtils.replaceLore(pokemon))
      .replace("'", "\\'");
    messageContent = "<hover:show_text:'" + hoverInfo + "'>" + messageContent + "</hover>";

    return messageContent;
  }

  public static Text getMessage(ServerPlayerEntity player, Pokemon pokemon) {
    String baseMessage = CobbleUtils.language.getMessagePokeShout();
    String playerName = player.getGameProfile().getName();
    String messageContent = PokemonUtils.replace(baseMessage, pokemon).replace("%player%", playerName);

    return AdventureTranslator.toNativeComponent(messageContent)
      .setStyle(Style.EMPTY
        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
          AdventureTranslator.toNative(String.join("\n", PokemonUtils.replaceLore(pokemon))))));
  }

}