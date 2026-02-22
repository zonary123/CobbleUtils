package com.kingpixel.cobbleutils.command.admin;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.Model.PokemonSpawner;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import kotlin.Unit;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class PokemonSpawnerCommand {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base.then(
        CommandManager.literal("pokemonspawner")
          .then(
            CommandManager.argument("type", StringArgumentType.string())
              .suggests((commandContext, suggestionsBuilder) -> {
                for (PokemonSpawner.Type value : PokemonSpawner.Type.values()) {
                  suggestionsBuilder.suggest(value.name());
                }
                return suggestionsBuilder.buildFuture();
              })
              .executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                String type = StringArgumentType.getString(context, "type");
                PokemonSpawner.Type spawnerType;
                try {
                  spawnerType = PokemonSpawner.Type.valueOf(type.toUpperCase());
                } catch (IllegalArgumentException e) {
                  context.getSource().sendError(Text.literal("Invalid spawner type: " + type));
                  return 0;
                }
                PokemonSpawner pokemonSpawner = new PokemonSpawner();
                pokemonSpawner.setType(spawnerType);
                player.sendMessage(Text.literal("Pokemon Spawner created with type: " + spawnerType.name()), false);
                Pokemon pokemon = pokemonSpawner.getPokemon();
                if (pokemon != null && pokemon.sendOut(player.getServerWorld(), player.getPos(), null, pokemonEntity -> Unit.INSTANCE) != null) {
                  player.sendMessage(Text.literal("Generated Pokemon: " + pokemon.getSpecies().getName()), false);
                } else {
                  player.sendMessage(Text.literal("No Pokemon generated."), false);
                }
                return 1;
              })
          )
      )
    );
  }
}
