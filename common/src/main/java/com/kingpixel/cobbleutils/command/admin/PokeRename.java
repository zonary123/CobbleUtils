package com.kingpixel.cobbleutils.command.admin;

import com.cobblemon.mod.common.command.argument.PartySlotArgumentType;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.JsonObject;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.LuckPermsUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 18/07/2024 11:28
 */
public class PokeRename implements Command<ServerCommandSource> {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                              LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base
        .then(
          CommandManager.literal("pokerename")
            .requires(source ->
              LuckPermsUtil.checkPermission(source, 2, List.of("cobbleutils.pokerename", "cobbleutils.admin")))
            .then(
              CommandManager.argument("slot", PartySlotArgumentType.Companion.partySlot())
                .then(
                  CommandManager.argument("name", StringArgumentType.greedyString())
                    .executes(new PokeRename())))));

  }

  @Override
  public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    try {
      if (!context.getSource().isExecutedByPlayer())
        return 0;

      ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
      Pokemon pokemon = PartySlotArgumentType.Companion.getPokemon(context, "slot");

      String name = StringArgumentType.getString(context, "name");

      player.sendMessage(Text.literal("This command is not implemented yet, please wait."));

      MutableText displayName = AdventureTranslator.toNativeComponent(name);

      pokemon.setNickname(displayName);

      PokemonEntity pokemonEntity = pokemon.getEntity();
      if (pokemonEntity != null) {
        pokemonEntity.setCustomName(displayName);
        pokemonEntity.setCustomNameVisible(true);
      }
      JsonObject json = pokemon.saveToJSON(DynamicRegistryManager.EMPTY, new JsonObject());

      if (json.has("Nickname")) {
        JsonObject nicknameObj = json.getAsJsonObject("Nickname");
        if (nicknameObj.has("text")) {
          nicknameObj.remove("text");
          nicknameObj.addProperty("text", displayName.getString());
        }
      }
      pokemon.loadFromJSON(DynamicRegistryManager.EMPTY, json);
      context.getSource().getPlayerOrThrow().sendMessage(displayName);

    } catch (Exception e) {
      CobbleUtils.LOGGER.error(e.getMessage());
      return 0;
    }
    return 1;
  }

}