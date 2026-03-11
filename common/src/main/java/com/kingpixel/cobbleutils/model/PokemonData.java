package com.kingpixel.cobbleutils.model;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import lombok.*;

/**
 * @author Carlos Varas Alonso - 15/07/2024 1:35
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Data
@Deprecated(forRemoval = true)
public class PokemonData {
  private String pokename;
  private String form;

  public PokemonData(String pokename, String form) {
    this.pokename = pokename;
    this.form = form;
  }

  public String info() {
    return "Pokemon: " + pokename + " Form: " + form;
  }

  public static boolean equals(PokemonData pokemonData1, PokemonData pokemonData2) {
    return pokemonData1.getPokename().equalsIgnoreCase(pokemonData2.getPokename()) &&
      pokemonData1.getForm().equalsIgnoreCase(pokemonData2.getForm());
  }

  public static PokemonData from(Pokemon pokemon) {
    return new PokemonData(pokemon.getSpecies().showdownId(), pokemon.getForm().formOnlyShowdownId());
  }

  public static PokemonData from(Species specie) {
    return new PokemonData(specie.showdownId(), "normal");
  }
}
