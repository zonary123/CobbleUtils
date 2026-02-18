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
@Deprecated(forRemoval = true)
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
    if (specialPokemons == null || specialPokemons.isEmpty()) {
      return null;
    }

    // ✅ Calculate total chance manually
    double totalChance = 0.0;
    for (int i = 0; i < specialPokemons.size(); i++) {
      totalChance += specialPokemons.get(i).getChance();
    }

    if (totalChance <= 0) {
      return null; // No valid chances
    }

    // ✅ Generate a random value between 0 and totalChance
    double randomValue = Utils.getRandom().nextDouble() * totalChance;

    // ✅ Iterate and pick the Pokémon based on cumulative probability
    double cumulativeChance = 0.0;
    for (int i = 0; i < specialPokemons.size(); i++) {
      PokemonChance specialPokemon = specialPokemons.get(i);
      cumulativeChance += specialPokemon.getChance();
      if (randomValue <= cumulativeChance) {
        return specialPokemon.getPokemon();
      }
    }

    // ✅ Fallback: return last Pokémon if something goes wrong
    return specialPokemons.get(specialPokemons.size() - 1).getPokemon();
  }


}
