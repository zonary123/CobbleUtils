package com.kingpixel.cobbleutils.Model;

import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Carlos Varas Alonso - 13/01/2025 2:17
 */
@Getter
@Setter
public class PokemonBlackList {
  transient
  private final Map<String, Boolean> resultsCache = new HashMap<>();
  private boolean onlyImplemented;
  private boolean allowEvolutions = true;
  private Set<String> pokemons;
  private Set<String> forms;
  private Set<String> aspects;
  private Set<String> labels;
  private Set<String> types;
  private Set<String> rarities;
  private Set<EggGroup> eggGroups;

  public PokemonBlackList() {
    this.pokemons = new HashSet<>();
    pokemons.add("egg");
    pokemons.add("pokestop");
    this.labels = new HashSet<>();
    labels.add("legendary");
    labels.add("mythical");
    labels.add("ultra_beast");
    this.types = new HashSet<>();
    types.add("water_Example");
    this.forms = new HashSet<>();
    forms.add("hisuian_Example");
    this.aspects = new HashSet<>();
    aspects.add("gmax_Example");
    this.eggGroups = new HashSet<>();
    this.rarities = new HashSet<>();
  }

  public void fix() {
    List<String> pokemonsBanneds = List.of("egg", "pokestop");
    pokemons.addAll(pokemonsBanneds);

    pokemons = pokemons.stream()
      .map(String::toLowerCase)
      .collect(Collectors.toSet());
    forms = forms.stream()
      .map(String::toLowerCase)
      .collect(Collectors.toSet());
    aspects = aspects.stream()
      .map(String::toLowerCase)
      .collect(Collectors.toSet());
    labels = labels.stream()
      .map(String::toLowerCase)
      .collect(Collectors.toSet());
    types = types.stream()
      .map(String::toLowerCase)
      .collect(Collectors.toSet());
  }

  public boolean isBlackListed(Pokemon pokemon) {
    if (!aspects.isEmpty()) {
      for (String aspect : pokemon.getAspects()) {
        if (this.aspects.contains(aspect)) return true;
      }
    }
    String pokemonShowdownId = pokemon.showdownId();
    Boolean result = resultsCache.get(pokemonShowdownId);
    if (result != null) return result;
    if (onlyImplemented && !pokemon.getSpecies().getImplemented()) return cacheResult(pokemonShowdownId, true);
    String pokemonFormShowdownId = "";
    if (!allowEvolutions) {
      Pokemon firstEvolution = PokemonUtils.getFirstEvolution(pokemon);
      pokemonFormShowdownId = pokemon.getForm().showdownId();
      if (!firstEvolution.getForm().showdownId().equals(pokemonFormShowdownId)) return true;
    }
    if (pokemon.getForm().getEggGroups().stream().anyMatch(eggGroups::contains))
      return cacheResult(pokemonShowdownId, true);
    if (pokemons.contains("*") || pokemons.contains(pokemonFormShowdownId)) {
      return cacheResult(pokemonShowdownId, true);
    } else if (pokemons.contains(pokemonShowdownId)) return cacheResult(pokemonShowdownId, true);

    if (pokemon.getForm().getLabels().stream().anyMatch(labels::contains)) return cacheResult(pokemonShowdownId, true);
    if (forms.contains(pokemon.getForm().formOnlyShowdownId())) return cacheResult(pokemonShowdownId, true);

    List<ElementalType> typeList = new ArrayList<>();
    pokemon.getTypes().forEach(typeList::add);
    if (typeList.stream().anyMatch(type -> {
      String keyType = type.getName().toLowerCase();
      return this.types.contains(keyType);
    })) return cacheResult(pokemonShowdownId, true);


    String rarity = PokemonUtils.getRarityS(pokemon);
    return cacheResult(pokemonShowdownId, rarities.contains(rarity));
  }

  public boolean cacheResult(String pokemonShowdownId, boolean result) {
    return resultsCache.put(pokemonShowdownId, result) != null;
  }
}