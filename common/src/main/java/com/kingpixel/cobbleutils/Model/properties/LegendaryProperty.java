package com.kingpixel.cobbleutils.Model.properties;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.labels.CobblemonPokemonLabels;
import com.cobblemon.mod.common.api.properties.CustomPokemonProperty;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.kingpixel.cobbleutils.util.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class LegendaryProperty implements CustomPokemonProperty {
  private static final List<Species> LEGENDARIES = new ArrayList<>();

  private final String value;

  public LegendaryProperty(String value) {
    this.value = value;
  }

  @Override
  public @NotNull String asString() {
    return "legendary";
  }

  @Override
  public void apply(@NotNull Pokemon pokemon) {
    if (isLegendary()) {
      Species legendary = getRandomLegendary();
      if (legendary != null) {
        pokemon.setSpecies(legendary);
      }
    }
  }

  @Override
  public boolean matches(@NotNull Pokemon pokemon) {
    if (isLegendary()) {
      return pokemon.getSpecies().getLabels().contains(CobblemonPokemonLabels.LEGENDARY);
    }
    return !pokemon.getSpecies().getLabels().contains(CobblemonPokemonLabels.LEGENDARY);
  }

  private static synchronized Species getRandomLegendary() {
    if (LEGENDARIES.isEmpty()) {
      for (Species species : PokemonSpecies.getSpecies()) {
        if (!species.getImplemented())
          continue;
        if (species.getLabels().contains(CobblemonPokemonLabels.LEGENDARY)) {
          LEGENDARIES.add(species);
        }
      }
    }
    if (LEGENDARIES.isEmpty())
      return null;
    return LEGENDARIES.get(Utils.getRandom().nextInt(LEGENDARIES.size()));
  }

  private boolean isLegendary() {
    return "yes".equalsIgnoreCase(this.value) || "true".equalsIgnoreCase(this.value);
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
