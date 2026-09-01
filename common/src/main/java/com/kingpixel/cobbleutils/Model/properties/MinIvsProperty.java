package com.kingpixel.cobbleutils.Model.properties;

import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.properties.CustomPokemonProperty;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MinIvsProperty implements CustomPokemonProperty {
  private static final List<Stats> STATS = new ArrayList<>(
      Arrays.stream(Stats.values())
          .filter(s -> s != Stats.EVASION && s != Stats.ACCURACY)
          .toList());
  private final String value;

  public MinIvsProperty(String value) {
    this.value = value;
  }

  @Override
  public @NotNull String asString() {
    return "min_ivs";
  }

  @Override
  public void apply(@NotNull Pokemon pokemon) {
    applyMinIvs(pokemon);
  }

  @Override
  public boolean matches(@NotNull Pokemon pokemon) {
    return true;
  }

  private void applyMinIvs(Pokemon pokemon) {
    if (this.value == null || this.value.isEmpty())
      return;

    try {
      String[] parts = this.value.split("_");
      int min = Math.max(0, Math.min(Integer.parseInt(parts[0]), 31));
      int amountOfStats = Integer.parseInt(parts[1]);

      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER_RAW.info("Min -> " + min + " Amount of stats -> " + amountOfStats);
      }

      List<Stats> statsCopy = new ArrayList<>(STATS);
      for (int i = 0; i < amountOfStats; i++) {
        var stat = statsCopy.get(Utils.getRandom().nextInt(statsCopy.size()));
        if (stat == null)
          continue;

        statsCopy.remove(stat);
        int iv = Utils.getRandom().nextInt(min, 32);

        if (CobbleUtils.config.isDebug()) {
          CobbleUtils.LOGGER_RAW
              .info("Setting IVs for " + pokemon.getSpecies().getName() + " to " + iv + " in " + stat.getShowdownId());
        }

        pokemon.getIvs().set(stat, iv);
      }
      for (Stats stats1 : statsCopy) {
        pokemon.getIvs().set(stats1, Utils.getRandom().nextInt(0, 32));
      }
    } catch (NumberFormatException e) {
      CobbleUtils.LOGGER_RAW.error("Invalid value format for MinIvsProperty: " + this.value, e);
    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.error("Unexpected error in applyMinIvs", e);
    }
  }

  @Override
  public void apply(@NotNull PokemonEntity pokemonEntity) {
    if (pokemonEntity.getPokemon() != null) {
      apply(pokemonEntity.getPokemon());
    }
  }

  @Override
  public boolean matches(@NotNull PokemonEntity pokemonEntity) {
    return pokemonEntity.getPokemon() != null && matches(pokemonEntity.getPokemon());
  }
}
