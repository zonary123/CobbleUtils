package com.kingpixel.cobbleutils.properties;

import com.cobblemon.mod.common.api.properties.CustomPokemonProperty;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Carlos Varas Alonso - 04/08/2024 19:40
 */
public class BreedableProperty implements CustomPokemonProperty {
  private boolean value;

  public BreedableProperty(boolean s) {
    this.value = s;
  }


  @NotNull @Override public String asString() {
    if (this.value) {
      return "true";
    } else {
      return "false";
    }
  }

  @Override public void apply(@NotNull PokemonEntity pokemonEntity) {
    if (!CobbleUtils.breedconfig.isActive()) return;
    value = CobbleUtils.breedconfig.canCreateEgg(pokemonEntity.getPokemon());
    PokemonUtils.setBreedable(pokemonEntity.getPokemon(), this.value);
  }

  @Override public void apply(@NotNull Pokemon pokemon) {
    if (!CobbleUtils.breedconfig.isActive()) return;
    value = CobbleUtils.breedconfig.canCreateEgg(pokemon);
    PokemonUtils.setBreedable(pokemon, value);
  }


  @Override public boolean matches(@NotNull Pokemon pokemon) {
    return true;
  }

  @Override public boolean matches(@NotNull PokemonEntity pokemonEntity) {
    return true;
  }
}
