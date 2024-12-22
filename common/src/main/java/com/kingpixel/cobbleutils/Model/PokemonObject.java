package com.kingpixel.cobbleutils.Model;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.JsonObject;
import com.kingpixel.cobbleutils.util.ArraysPokemons;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import lombok.Getter;
import lombok.ToString;
import net.minecraft.registry.DynamicRegistryManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Model representing a Pokemon object with rarity and expiration date.
 * Optimized by [Tu Nombre] - 09/07/2024
 */
@Getter
@ToString
public class PokemonObject {
  private JsonObject pokemon;
  private String rarity;
  private Date date;

  public PokemonObject(JsonObject pokemon, Date date) {
    this.pokemon = pokemon;
    this.rarity = PokemonUtils.getRarityS(Pokemon.Companion.loadFromJSON(DynamicRegistryManager.EMPTY, pokemon));
    this.date = date;
  }

  /**
   * Check if the Pokémon has expired based on its date.
   *
   * @return True if expired, false otherwise.
   */
  private boolean expired() {
    return Optional.ofNullable(date).map(d -> new Date().after(d)).orElse(false);
  }

  /**
   * Get the Pokemon species id.
   *
   * @return The Pokémon species showdownId.
   */
  private String getPokemonId() {
    return getPokemon().getSpecies().showdownId();
  }

  /**
   * Get the Pokémon from the stored JsonObject.
   *
   * @return The Pokémon.
   */
  private Pokemon getPokemon() {
    return Pokemon.Companion.loadFromJSON(DynamicRegistryManager.EMPTY, pokemon);
  }

  /**
   * Generate a new Pokémon and update this object's JSON representation.
   */
  private void getNewPokemon() {
    this.pokemon = ArraysPokemons.getRandomPokemonNormalRarity(this.rarity).saveToJSON(DynamicRegistryManager.EMPTY, new JsonObject());
  }

  /**
   * Generic method to generate a list of Pokémon based on the given type and settings.
   *
   * @param size    The size of the list to generate.
   * @param expired Whether or not the Pokémon are considered expired.
   * @param minutes The time in minutes before expiration.
   * @param type    The type of Pokémon to generate (NORMAL, LEGENDARY, etc.).
   *
   * @return A list of PokemonObject.
   */
  private static List<PokemonObject> getPokemonListByType(int size, boolean expired, int minutes, ArraysPokemons.TypePokemon... type) {
    List<Pokemon> pokemons = ArraysPokemons.getRandomPokemon(List.of(type), size);
    List<PokemonObject> pokemonObjects = new ArrayList<>();

    Date expirationDate = expired ? new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes)) : null;

    pokemons.forEach(pokemon -> {
      PokemonObject pokemonObject = new PokemonObject(pokemon.saveToJSON(DynamicRegistryManager.EMPTY, new JsonObject()), expirationDate);
      pokemonObjects.add(pokemonObject);
    });

    return pokemonObjects;
  }

  /**
   * Get a list of regular Pokémon (NORMAL type).
   *
   * @param size    The size of the list.
   * @param expired If the Pokémon is expired.
   * @param minutes The minutes of expiration.
   *
   * @return List of Pokémon objects.
   */
  public static List<PokemonObject> getListPokemon(int size, boolean expired, int minutes) {
    return getPokemonListByType(size, expired, minutes, ArraysPokemons.TypePokemon.NORMAL);
  }

  /**
   * Get a list of legendary Pokémon (LEGENDARY and PARADOX types).
   *
   * @param size    The size of the list.
   * @param expired If the Pokémon is expired.
   * @param minutes The minutes of expiration.
   *
   * @return List of legendary Pokémon objects.
   */
  public static List<PokemonObject> getListLegendaries(int size, boolean expired, int minutes) {
    return getPokemonListByType(size, expired, minutes, ArraysPokemons.TypePokemon.LEGENDARY, ArraysPokemons.TypePokemon.PARADOX);
  }

  /**
   * Get a list of Ultra Beast Pokémon (ULTRABEAST type).
   *
   * @param size    The size of the list.
   * @param expired If the Pokémon is expired.
   * @param minutes The minutes of expiration.
   *
   * @return List of Ultra Beast Pokémon objects.
   */
  public static List<PokemonObject> getListUltraBeast(int size, boolean expired, int minutes) {
    return getPokemonListByType(size, expired, minutes, ArraysPokemons.TypePokemon.ULTRABEAST);
  }
}
