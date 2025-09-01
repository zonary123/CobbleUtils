package com.kingpixel.cobbleutils.Model;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 13/06/2024 10:19
 */
@Getter
@ToString
public class PokemonChance {
  private String pokemon;
  private int chance;

  public PokemonChance() {
    this.pokemon = "bulbasaur";
    this.chance = 100;
  }

  public PokemonChance(String pokemon, int chance) {
    this.pokemon = pokemon;
    this.chance = chance;
  }

  public static Pokemon getPokemonCreate(List<PokemonChance> specialPokemons) {
    String pokemon = getPokemon(specialPokemons);
    if (pokemon == null) return null;
    return PokemonProperties.Companion.parse(pokemon).create();
  }

  public static Species getPokemonSpecies(List<PokemonChance> specialPokemons) {
    String pokemon = getPokemon(specialPokemons);
    if (pokemon == null) return null;
    return PokemonProperties.Companion.parse(pokemon).create().getSpecies();
  }

  public static String getPokemon(List<PokemonChance> specialPokemons) {
    // Calcula la suma total de las probabilidades
    double totalChance = specialPokemons.stream()
      .mapToDouble(PokemonChance::getChance)
      .sum();

    if (totalChance <= 0) return null; // Manejo de caso cuando la suma de probabilidades es 0

    // Genera un valor aleatorio entre 0 y el total de probabilidades
    double randomValue = Utils.getRandom().nextDouble() * totalChance;

    // Recorre la lista de Pokémon especiales y selecciona uno basado en el valor aleatorio
    double cumulativeChance = 0.0;
    for (PokemonChance specialPokemon : specialPokemons) {
      cumulativeChance += specialPokemon.getChance();
      if (randomValue <= cumulativeChance) {
        return specialPokemon.getPokemon();
      }
    }

    // En caso de algún error inesperado, retorna el último Pokémon en la lista
    return specialPokemons.isEmpty() ? null : specialPokemons.get(specialPokemons.size() - 1).getPokemon();
  }


}
