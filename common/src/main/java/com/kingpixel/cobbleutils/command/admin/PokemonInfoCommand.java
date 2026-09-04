package com.kingpixel.cobbleutils.command.admin;

import com.cobblemon.mod.common.command.argument.PartySlotArgumentType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/**
 *
 * @author Carlos Varas Alonso - 02/09/2026 17:31
 */
public class PokemonInfoCommand {

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(base
      .then(
        CommandManager.literal("pokemoninfo")
          .then(
            CommandManager.argument("slot", PartySlotArgumentType.Companion.partySlot())
              .executes(context -> {
                Pokemon pokemon = PartySlotArgumentType.Companion.getPokemon(context, "slot");
                ServerCommandSource source = context.getSource();
                if (pokemon == null) {
                  source.sendFeedback(() -> Text.literal("No Pokémon found in the specified slot."), false);
                  return 0;
                }

                source.sendFeedback(() -> Text.literal("Pokémon Info: " +
                  "\n ShowdownId: " + pokemon.showdownId() +
                  "\n Aspects: " + String.join(", ", pokemon.getAspects()) +
                  "\n persistentmap: " + stringPersistent(pokemon.getPersistentData())
                ), false);

                return 1;
              })
          )
      )
    );
  }

  private static String stringPersistent(NbtCompound persistentData) {
    StringBuilder sb = new StringBuilder();
    persistentData.getKeys().forEach(key -> {
      sb.append(key).append(": ").append(persistentData.get(key)).append(", ");
    });
    if (!sb.isEmpty()) {
      sb.setLength(sb.length() - 2); // Remove the last comma and space
    }
    return sb.toString();
  }
}
