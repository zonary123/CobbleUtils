package com.kingpixel.cobbleutils.command.test;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokedex.CaughtCount;
import com.cobblemon.mod.common.api.pokedex.Dexes;
import com.cobblemon.mod.common.api.pokedex.PokedexManager;
import com.cobblemon.mod.common.api.pokedex.entry.PokedexEntry;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Carlos Varas Alonso - 26/08/2024 2:28
 */
public class TestCommands {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                              LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base.then(
        CommandManager.literal("dex")
          .then(
            CommandManager.argument("id", StringArgumentType.string())
              .suggests((context, builder) -> {
                return builder.buildFuture();
              })
              .executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                String id = StringArgumentType.getString(context, "id");

                PokedexManager pokedexManager = Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(player);
                Identifier identifier = Identifier.of(id);

                var pokedexEntries = Dexes.INSTANCE.getDexEntryMap().get(identifier).getEntries();

                Map<Identifier, PokedexEntry> entries = new HashMap<>();
                pokedexEntries.forEach(pokedexEntry -> {
                  Identifier speciesId = pokedexEntry.getSpeciesId();

                  Species species = PokemonSpecies.INSTANCE.getByIdentifier(speciesId);
                  if (species != null) {
                    if (species.getImplemented()) {
                      entries.put(pokedexEntry.getId(), pokedexEntry);
                    }
                  }
                });

                PlayerUtils.sendMessage(
                  player,
                  "Entries: " + entries.size() +
                    " - " + CaughtCount.INSTANCE.calculate(pokedexManager, entries)
                );

                return 1;
              })
          )
      )
    );
  }
}
