package com.kingpixel.cobbleutils.model.properties;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.labels.CobblemonPokemonLabels;
import com.cobblemon.mod.common.api.properties.CustomPokemonProperty;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 04/08/2024 19:40
 */
public class LegendaryProperty implements CustomPokemonProperty {
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
    applyLegendary(pokemon);
  }


  @Override
  public boolean matches(@NotNull Pokemon pokemon) {
    return true;
  }

  private final List<Species> legendaries = new ArrayList<>();

  private void applyLegendary(Pokemon pokemon) {
    int level = pokemon.getLevel();
    if (isLegendary()) {
      Species legendary = getRandomLegendary();
      pokemon.setSpecies(legendary);
    }
  }

  private Species getRandomLegendary() {
    if (legendaries.isEmpty()) {
      var s = PokemonSpecies.getSpecies();
      for (Species species : s) {
        if (!species.getImplemented()) continue;
        if (species.getLabels().contains(CobblemonPokemonLabels.LEGENDARY)) {
          legendaries.add(species);
        }
      }
    }
    return legendaries.get((int) (Math.random() * legendaries.size()));
  }

  private boolean isLegendary() {
    return value.equalsIgnoreCase("yes");
  }

  @Override
  public void apply(@NotNull PokemonEntity pokemonEntity) {

  }

  @Override
  public boolean matches(@NotNull PokemonEntity pokemonEntity) {
    return false;
  }
}
