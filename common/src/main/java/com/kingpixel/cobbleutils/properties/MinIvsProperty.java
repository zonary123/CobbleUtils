package com.kingpixel.cobbleutils.properties;

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

/**
 * @author Carlos Varas Alonso - 04/08/2024 19:40
 */
public class MinIvsProperty implements CustomPokemonProperty {
  private String value;

  public MinIvsProperty(String value) {
    this.value = value;
  }

  @Override public @NotNull String asString() {
    return "min_ivs";
  }

  @Override public void apply(@NotNull Pokemon pokemon) {
    applyMinIvs(pokemon);
  }

  @Override public void apply(@NotNull PokemonEntity pokemonEntity) {
    applyMinIvs(pokemonEntity.getPokemon());
  }

  @Override public boolean matches(@NotNull Pokemon pokemon) {
    return true;
  }

  @Override public boolean matches(@NotNull PokemonEntity pokemonEntity) {
    return true;
  }

  private static final List<Stats> stats = new ArrayList<>(Arrays.stream(Stats.values()).filter(stats1 -> stats1 != Stats.EVASION && stats1 != Stats.ACCURACY).toList());

  private void applyMinIvs(Pokemon pokemon) {
    if (this.value == null || this.value.isEmpty()) return;
    try {
      int min = 0;
      int amount_of_stats = 0;


      min = Integer.parseInt(this.value.split("_")[0]);
      amount_of_stats = Integer.parseInt(this.value.split("_")[1]);
      min = Math.max(0, Math.min(min, 31));

      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER.info("Min -> " + min + " Amount of stats -> " + amount_of_stats);
      }
      List<Stats> statsCopy = new ArrayList<>(stats);
      for (int i = 0; i < amount_of_stats; i++) {

        if (stats.isEmpty()) return;
        Stats stats1 = statsCopy.remove(Utils.RANDOM.nextInt(stats.size()));
        int iv = Utils.RANDOM.nextInt(min, 32);
        if (CobbleUtils.config.isDebug()) {
          CobbleUtils.LOGGER.info("Setting IVs for " + pokemon.getSpecies().getName() + " to " + iv + " in " + stats1.getShowdownId());
        }
        pokemon.getIvs().set(stats1, iv);
      }
    } catch (Exception ignored) {
    }
  }
}
