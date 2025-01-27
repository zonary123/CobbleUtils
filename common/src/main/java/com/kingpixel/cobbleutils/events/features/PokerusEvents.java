package com.kingpixel.cobbleutils.events.features;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.EVs;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.options.Pokerus;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import kotlin.Unit;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.kingpixel.cobbleutils.Model.CobbleUtilsTags.POKERUS_INFECTED_TAG;
import static com.kingpixel.cobbleutils.Model.CobbleUtilsTags.POKERUS_TAG;

/**
 * @author Carlos Varas Alonso - 20/07/2024 6:45
 */
public class PokerusEvents {

  public static void register() {
    if (CobbleUtils.config.getPokerus().isActive()) {
      EntityEvent.ADD.register((entity, level) -> {
        if (!CobbleUtils.config.getPokerus().isActive())
          return EventResult.pass();
        if (entity instanceof PokemonEntity pokemon) {
          if (((MobEntity) entity).isPersistent())
            return EventResult.pass();
          if (((MobEntity) entity).isAiDisabled())
            return EventResult.pass();
          if (pokemon.getPokemon().isPlayerOwned())
            return EventResult.pass();
          CobbleUtils.config.getPokerus().apply(pokemon.getPokemon(), false);
        }
        return EventResult.pass();
      });

      CobblemonEvents.POKEMON_SENT_POST.subscribe(Priority.NORMAL, (evt) -> {
        if (!CobbleUtils.config.getPokerus().isActive())
          return Unit.INSTANCE;
        Pokemon pokemon = evt.getPokemon();
        if (pokemon.getPersistentData().getLong(POKERUS_TAG) < new Date().getTime()) {
          pokemon.getPersistentData().remove(POKERUS_TAG);
          pokemon.getPersistentData().putBoolean(POKERUS_INFECTED_TAG, true);
          CobbleUtils.config.getPokerus().apply(pokemon, false);
        }
        return Unit.INSTANCE;
      });

      CobblemonEvents.BATTLE_FAINTED.subscribe(Priority.NORMAL, (evt) -> {
        if (!CobbleUtils.config.getPokerus().isActive())
          return Unit.INSTANCE;
        List<Pokemon> pokemons = new ArrayList<>();
        Pokemon pokemonkilled = evt.getKilled().getEffectedPokemon();
        for (ActiveBattlePokemon activeBattlePokemon : evt.getBattle().getActivePokemon()) {
          BattlePokemon pokemonEntity = activeBattlePokemon.getBattlePokemon();
          if (pokemonEntity != null) {
            Pokemon pokemon = pokemonEntity.getOriginalPokemon();
            if (pokemon.isPlayerOwned()) {
              pokemons.add(pokemon);
            }
          }

        }

        for (Pokemon pokemon : pokemons) {
          boolean pokerus = pokemon.getPersistentData().getBoolean(POKERUS_TAG);
          if (pokerus)
            pokemonkilled.getForm().getEvYield().forEach((stat, integer) -> {
              EVs evs = pokemon.getEvs();
              double multiplier = CobbleUtils.config.getPokerus().getMultiplier();
              int adjustedValue;
              if (integer < 0)
                return;
              if (integer == 1) {
                adjustedValue = (int) multiplier;
              } else {
                double value = integer * multiplier - 3;
                if (CobbleUtils.config.getPokerus().isRoundup()) {
                  adjustedValue = (int) Math.ceil(value);
                } else {
                  adjustedValue = (int) Math.floor(value);
                }
              }

              evs.set(stat, evs.get(stat) + adjustedValue);
            });
        }

        return Unit.INSTANCE;
      });

      CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, (evt) -> {
        if (!CobbleUtils.config.getPokerus().isActive())
          return Unit.INSTANCE;

        List<ServerPlayerEntity> players = new ArrayList<>();
        Set<PokemonEntity> processedPokemon = new HashSet<>();

        // Process all battle actors
        evt.getWinners().forEach(winner -> {
          if (winner instanceof PlayerBattleActor playerBattleActor) {
            players.add(playerBattleActor.getEntity());
          }
        });

        evt.getLosers().forEach(loser -> {
          if (loser instanceof PlayerBattleActor playerBattleActor) {
            players.add(playerBattleActor.getEntity());
          }
        });

        // Process each player's party for Pokerus
        players.forEach(player -> {
          PlayerPartyStore playerPartyStore = Cobblemon.INSTANCE.getStorage().getParty(player);
          AtomicBoolean hasPokerus = new AtomicBoolean(false);
          playerPartyStore.forEach(pokemon -> {
            if (pokemon.getPersistentData().getBoolean(POKERUS_TAG))
              hasPokerus.set(true);
          });

          if (hasPokerus.get()) {
            playerPartyStore.forEach(Pokerus::applywithrarity);
          }
        });

        return Unit.INSTANCE;
      });
    }
  }
}
