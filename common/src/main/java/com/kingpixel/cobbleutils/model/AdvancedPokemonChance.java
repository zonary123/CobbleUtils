package com.kingpixel.cobbleutils.model;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.pokemon.Pokemon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvancedPokemonChance {
  @Builder.Default
  private List<String> pokemons = new ArrayList<>(List.of("pikachu", "rattata", "bulbasaur", "charmander", "squirtle"));
  @Builder.Default
  private double chance = 100.0;

  public static List<AdvancedPokemonChance> getDefault() {
    List<AdvancedPokemonChance> defaultList = new ArrayList<>();
    defaultList.add(AdvancedPokemonChance.builder()
      .pokemons(List.of("pikachu", "rattata", "bulbasaur", "charmander", "squirtle"))
      .chance(100.0)
      .build());
    defaultList.add(AdvancedPokemonChance.builder()
      .pokemons(List.of("pikachu", "rattata", "bulbasaur", "charmander", "squirtle"))
      .chance(100.0)
      .build());
    return defaultList;
  }

  public static List<Pokemon> getPokemons(List<AdvancedPokemonChance> pokemonChances) {
    List<Pokemon> pokemons = new ArrayList<>();
    for (AdvancedPokemonChance pokemonChance : pokemonChances) {
      int size = pokemonChance.getPokemons().size();
      for (int i = 0; i < size; i++) {
        Pokemon pokemon = PokemonProperties.Companion.parse(pokemonChance.getPokemons().get(i)).create();
        pokemons.add(pokemon);
      }
    }
    return pokemons;
  }

  public static Pokemon getPokemon(List<AdvancedPokemonChance> pokemonChances) {
    double totalChance = 0;
    for (AdvancedPokemonChance pokemonChance : pokemonChances) {
      totalChance += pokemonChance.getChance();
    }
    double randomValue = ThreadLocalRandom.current().nextDouble() * totalChance;
    for (AdvancedPokemonChance pokemonChance : pokemonChances) {
      randomValue -= pokemonChance.getChance();
      if (randomValue <= 0) {
        List<String> pokemons = pokemonChance.getPokemons();
        int size = pokemons.size();
        int index = ThreadLocalRandom.current().nextInt(size);
        return PokemonProperties.Companion.parse(pokemons.get(index)).create();
      }
    }
    return PokemonProperties.Companion.parse("rattata").create();
  }

}
