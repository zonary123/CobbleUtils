package com.kingpixel.cobbleutils.features.breeding.events;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Carlos Varas Alonso - 23/07/2024 22:07
 */
public class EggInteract {
  public static void register() {
    InteractionEvent.INTERACT_ENTITY.register((playerEntity, entity, hand) -> {
      handleEgg(entity, PlayerUtils.castPlayer(playerEntity));
      return EventResult.pass();
    });

    PlayerEvent.ATTACK_ENTITY.register((playerEntity, world, entity, hand, entityHitResult) -> {
      handleEgg(entity, PlayerUtils.castPlayer(playerEntity));
      return EventResult.pass();
    });

  }

  private static void handleEgg(Entity entity, ServerPlayerEntity player) {
    if (entity instanceof PokemonEntity pokemonEntity) {
      Pokemon pokemon = pokemonEntity.getPokemon();
      if (pokemon.getSpecies().showdownId().equalsIgnoreCase("egg")) {
        if (pokemon.getPersistentData().getBoolean("SpawnEgg")) {
          pokemon.getPersistentData().remove("SpawnEgg");
          Cobblemon.INSTANCE.getStorage().getParty(player).add(pokemon);
          pokemonEntity.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        }
      }
    }
  }
}
