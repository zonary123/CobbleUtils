package com.kingpixel.cobbleutils.properties;

import com.cobblemon.mod.common.api.properties.CustomPokemonProperty;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ScalePokemonData;
import com.kingpixel.cobbleutils.Model.SizeChanceWithoutItem;
import org.jetbrains.annotations.NotNull;

/**
 * @author Carlos Varas Alonso - 04/08/2024 19:40
 */
public class ScaleProperty implements CustomPokemonProperty {
  public Float value;

  public ScaleProperty(String value) {
    if (CobbleUtils.config.isRandomsize()) {
      if (value != null) {
        this.value = Float.parseFloat(value);
      } else {
        this.value = 1.0f;
      }
    } else {
      this.value = 1.0f;
    }
  }

  @Override public void apply(@NotNull PokemonEntity pokemonEntity) {
    if (CobbleUtils.config.isRandomsize()) {
      SizeChanceWithoutItem scalePokemonData = ScalePokemonData.getScalePokemonData(pokemonEntity.getPokemon(), this.value);
      scalePokemonData.apply(pokemonEntity.getPokemon());
    } else {
      pokemonEntity.getPokemon().setScaleModifier(1.0f);
    }
  }

  @NotNull @Override public String asString() {
    return "scale";
  }

  @Override public void apply(@NotNull Pokemon pokemon) {
    if (CobbleUtils.config.isRandomsize()) {
      SizeChanceWithoutItem scalePokemonData = ScalePokemonData.getScalePokemonData(pokemon, this.value);
      scalePokemonData.apply(pokemon);
    } else {
      pokemon.setScaleModifier(1.0f);
    }
  }

  @Override public boolean matches(@NotNull Pokemon pokemon) {

    return true;
  }

  @Override public boolean matches(@NotNull PokemonEntity pokemonEntity) {
    return true;
  }
}
