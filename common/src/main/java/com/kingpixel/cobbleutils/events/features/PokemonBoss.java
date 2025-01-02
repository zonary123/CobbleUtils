package com.kingpixel.cobbleutils.events.features;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.config.BossConfig;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.Utils;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.PlayerEvent;
import kotlin.Unit;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;

import static com.kingpixel.cobbleutils.Model.CobbleUtilsTags.BOSS_TAG;

/**
 * @author Carlos Varas Alonso - 20/07/2024 9:04
 */
public class PokemonBoss {
  private static boolean spawnboss = false;

  public static void register() {
    CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(Priority.HIGH, (evt) -> {
      try {
        if (!CobbleUtils.config.isBoss()) return Unit.INSTANCE;

        if (!spawnboss) {
          if (Utils.RANDOM.nextInt((int) CobbleUtils.config.getBosschance()) != 0) {
            return Unit.INSTANCE;
          } else {
            spawnboss = true;
          }
        }

        Entity entity = evt.getEntity();
        if (!(entity instanceof PokemonEntity pokemonEntity)) return Unit.INSTANCE;


        MobEntity mobEntity = (MobEntity) entity;
        if (mobEntity.isPersistent() || mobEntity.isAiDisabled())
          return Unit.INSTANCE;

        Pokemon pokemon = pokemonEntity.getPokemon();
        if (BossConfig.notCanBeBoss(pokemon)) return Unit.INSTANCE;

        BossConfig config = BossConfig.getBossConfig(pokemon);
        if (config == null) return Unit.INSTANCE;
        if (Utils.RANDOM.nextInt(CobbleUtils.config.getBosschance()) == 1) config.apply(pokemonEntity);
      } catch (Exception e) {  // Reemplaza con excepciones específicas
        e.printStackTrace();
      }
      return Unit.INSTANCE;
    });

    CobblemonEvents.THROWN_POKEBALL_HIT.subscribe(Priority.HIGH, (evt) -> {
      if (!CobbleUtils.config.isBoss()) return Unit.INSTANCE;

      if (evt.getPokemon().getPokemon().getPersistentData().getBoolean(BOSS_TAG)) {
        evt.cancel();
      }
      return Unit.INSTANCE;
    });

    InteractionEvent.INTERACT_ENTITY.register((playerEntity, entity, hand) -> {
      if (!CobbleUtils.config.isBoss()) return EventResult.pass();
      if (entity instanceof PokemonEntity pokemonEntity) {
        BossConfig.openMenuRewards(PlayerUtils.castPlayer(playerEntity), pokemonEntity.getPokemon());
      }
      return EventResult.pass();
    });

    PlayerEvent.ATTACK_ENTITY.register((player, world, entity, hand, entityHitResult) -> {
      if (!CobbleUtils.config.isBoss()) return EventResult.pass();
      if (entity instanceof PokemonEntity pokemonEntity) {
        if (BossConfig.isBoss(pokemonEntity.getPokemon())) {
          BossConfig.openMenuRewards(PlayerUtils.castPlayer(player), pokemonEntity.getPokemon());
          return EventResult.interruptTrue();
        }

      }
      return EventResult.pass();
    });

    CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.HIGH, (evt) -> {
      if (!CobbleUtils.config.isBoss()) return Unit.INSTANCE;


      evt.getLosers().forEach(battleActor -> {
        if (battleActor instanceof PokemonBattleActor pokemonBattleActor) {
          Pokemon pokemon = pokemonBattleActor.getPokemon().getOriginalPokemon();
          if (!pokemon.isPlayerOwned() && pokemon.getPersistentData().getBoolean(BOSS_TAG)) {
            evt.getWinners().forEach(winner -> {
              if (winner instanceof PlayerBattleActor playerBattleActor) {
                BossConfig.GiveRewards(playerBattleActor.getEntity(), pokemon);
              }
            });
          }
        }
      });
      return Unit.INSTANCE;
    });
  }

  private static Unit handleBossSave(PokemonEntity pokemonEntity) {
    try {
      if (!CobbleUtils.config.isBoss()) return Unit.INSTANCE;
      Pokemon pokemon = pokemonEntity.getPokemon();
      if (pokemon.getPersistentData().getBoolean(BOSS_TAG)) {
        if (CobbleUtils.config.isDebug()) {
          CobbleUtils.LOGGER.info("Saving Boss Pokemon");
        }
        pokemon.setLevel(1);
        pokemonEntity.kill();
      }
    } catch (Exception ignored) {
    }
    return Unit.INSTANCE;
  }


}
