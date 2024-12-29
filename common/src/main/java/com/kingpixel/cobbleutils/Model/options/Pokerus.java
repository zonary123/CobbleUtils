package com.kingpixel.cobbleutils.Model.options;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.CobbleUtilsTags;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Getter;
import lombok.ToString;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.kingpixel.cobbleutils.Model.CobbleUtilsTags.POKERUS_INFECTED_TAG;
import static com.kingpixel.cobbleutils.Model.CobbleUtilsTags.POKERUS_TAG;

/**
 * @author Carlos Varas Alonso - 20/07/2024 6:33
 */
@Getter
@ToString
public class Pokerus {
  private final boolean active;
  private final double multiplier;
  private final int rarity;
  private final boolean roundup;
  private final boolean canspawnwithpokerus;

  public Pokerus() {
    this.active = false;
    this.multiplier = 2;
    this.rarity = 10000;
    this.roundup = true;
    this.canspawnwithpokerus = true;
  }

  public Pokerus(boolean active, double multiplier, int rarity, boolean roundup, boolean canspawnwithpokerus) {
    this.active = active;
    this.multiplier = multiplier;
    this.rarity = rarity;
    this.roundup = roundup;
    this.canspawnwithpokerus = canspawnwithpokerus;
  }

  public static void apply(Pokemon pokemon) {
    if (pokemon.getPersistentData().getBoolean(POKERUS_INFECTED_TAG)) return;
    pokemon.getPersistentData().putBoolean(POKERUS_TAG, true);
    pokemon.getPersistentData().putLong(CobbleUtilsTags.POKERUS_TIME_TAG, new Date().getTime() + TimeUnit.DAYS.toMillis(2));
  }

  public static void applyPokemon(Pokemon pokemon) {
    if (pokemon.getPersistentData().contains(POKERUS_TAG)) {
      boolean pokerus = pokemon.getPersistentData().getBoolean(POKERUS_INFECTED_TAG);
      if (!pokerus) {
        pokemon.getPersistentData().putBoolean(POKERUS_TAG, true);
        pokemon.getPersistentData().putLong(CobbleUtilsTags.POKERUS_TIME_TAG, new Date().getTime() + TimeUnit.DAYS.toMillis(2));
      } else {
        pokemon.getPersistentData().putBoolean(POKERUS_TAG, false);
        pokemon.getPersistentData().remove(CobbleUtilsTags.POKERUS_TIME_TAG);
      }

    } else {
      pokemon.getPersistentData().putBoolean(POKERUS_TAG, true);
      pokemon.getPersistentData().putLong(CobbleUtilsTags.POKERUS_TIME_TAG, new Date().getTime() + TimeUnit.DAYS.toMillis(2));
    }
  }

  public static void applywithrarity(Pokemon pokemon) {
    if (pokemon.getPersistentData().getBoolean(POKERUS_TAG)) return;
    if (pokemon.getPersistentData().getBoolean(POKERUS_INFECTED_TAG)) return;
    boolean apply = Utils.RANDOM.nextInt(CobbleUtils.config.getPokerus().getRarity()) == 0;
    if (CobbleUtils.config.isDebug() && apply) {
      CobbleUtils.LOGGER.info("Applying Pokerus to " + pokemon.getDisplayName());
    }
    pokemon.getPersistentData().putBoolean(POKERUS_TAG, apply);
    if (apply)
      pokemon.getPersistentData().putLong(CobbleUtilsTags.POKERUS_TIME_TAG, new Date().getTime() + TimeUnit.DAYS.toMillis(2));
  }

  public Pokemon apply(Pokemon pokemon, boolean battle) {
    if (!active) return pokemon;
    if (pokemon == null) return null;
    if (pokemon.getPersistentData().getBoolean(POKERUS_TAG)) return pokemon;
    if (pokemon.getPersistentData().getBoolean(POKERUS_INFECTED_TAG)) return pokemon;
    boolean apply = Utils.RANDOM.nextInt(rarity) == 0;
    if (battle) {
      pokemon.getPersistentData().putBoolean(POKERUS_TAG, apply);
      pokemon.getPersistentData().putLong(CobbleUtilsTags.POKERUS_TIME_TAG, new Date().getTime() + TimeUnit.DAYS.toMillis(2));
    } else {
      if (canspawnwithpokerus) {
        pokemon.getPersistentData().putBoolean(POKERUS_TAG, apply);
        pokemon.getPersistentData().putLong(CobbleUtilsTags.POKERUS_TIME_TAG, new Date().getTime() + TimeUnit.DAYS.toMillis(2));
      }
    }
    return pokemon;
  }


}
