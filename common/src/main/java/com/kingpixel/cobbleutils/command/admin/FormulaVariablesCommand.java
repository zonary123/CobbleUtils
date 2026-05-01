package com.kingpixel.cobbleutils.command.admin;

import com.cobblemon.mod.common.command.argument.PartySlotArgumentType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.Model.PokemonFormula;
import com.kingpixel.cobbleutils.ui.PokemonFormulaVariablesMenu;
import com.kingpixel.cobbleutils.util.LuckPermsUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * Admin command to inspect formula variables for a selected party pokemon.
 */
public final class FormulaVariablesCommand {

  private static final List<String> PERMISSIONS = List.of("cobbleutils.admin", "cobbleutils.formula");

  private FormulaVariablesCommand() {
  }

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                              LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base
        .then(
          CommandManager.literal("formulavars")
            .requires(source -> LuckPermsUtil.checkPermission(source, 2, PERMISSIONS))
            .then(CommandManager.literal("menu")
              .then(CommandManager.argument("slot", PartySlotArgumentType.Companion.partySlot())
                .executes(context -> {
                  if (!context.getSource().isExecutedByPlayer()) {
                    return 0;
                  }
                  ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                  Pokemon pokemon = PartySlotArgumentType.Companion.getPokemon(context, "slot");
                  PokemonFormulaVariablesMenu.open(player, new PokemonFormula(), pokemon);
                  return 1;
                })))
            .then(CommandManager.literal("debug")
              .then(CommandManager.argument("slot", PartySlotArgumentType.Companion.partySlot())
                .executes(context -> {
                  if (!context.getSource().isExecutedByPlayer()) {
                    return 0;
                  }
                  ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                  Pokemon pokemon = PartySlotArgumentType.Companion.getPokemon(context, "slot");
                  new PokemonFormula().sendVariablesDebug(player, pokemon);
                  return 1;
                })))
        )
    );
  }
}

