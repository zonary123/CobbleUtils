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
    return isBlacklisted(pokemon);
  }

  public boolean isBlacklisted(Pokemon pokemon) {
    // ✅ Quick wildcard check
    if (pokemons.contains("*")) {
      return true;
    }

    // ✅ Check cached result first to avoid redundant computation
    String showdownId = pokemon.showdownId();
    Boolean cached = resultsCache.get(showdownId);
    if (cached != null) {
      return cached;
    }

    // ✅ Check aspects
    if (!aspects.isEmpty()) {
      for (String aspect : pokemon.getAspects()) {
        if (aspects.contains(aspect)) {
          return cacheResult(showdownId, true);
        }
      }
    }

    // ✅ Check "only implemented" restriction
    if (onlyImplemented && !pokemon.getSpecies().getImplemented()) {
      return cacheResult(showdownId, true);
    }

    // ✅ Form and evolution restrictions
    var form = pokemon.getForm();
    String formShowdownId = form.showdownId();

    if (!allowEvolutions) {
      Pokemon firstEvolution = PokemonUtils.getFirstEvolution(pokemon);
      if (!firstEvolution.getForm().showdownId().equals(formShowdownId)) {
        return cacheResult(showdownId, true);
      }
    }

    // ✅ Check egg groups
    for (EggGroup eggGroup : form.getEggGroups()) {
      if (eggGroups.contains(eggGroup)) {
        return cacheResult(showdownId, true);
      }
    }

    // ✅ Check for direct Pokémon matches
    if (pokemons.contains("*") || pokemons.contains(formShowdownId) || pokemons.contains(showdownId)) {
      return cacheResult(showdownId, true);
    }

    // ✅ Check labels
    for (String label : form.getLabels()) {
      if (labels.contains(label)) {
        return cacheResult(showdownId, true);
      }
    }

    // ✅ Check forms
    if (forms.contains(form.formOnlyShowdownId())) {
      return cacheResult(showdownId, true);
    }

    // ✅ Check types
    for (ElementalType type : pokemon.getTypes()) {
      String typeName = type.getName().toLowerCase();
      if (types.contains(typeName)) {
        return cacheResult(showdownId, true);
      }
    }

    // ✅ Check rarity
    String rarity = PokemonUtils.getRarityS(pokemon);
    boolean isBlacklisted = rarities.contains(rarity);

    return cacheResult(showdownId, isBlacklisted);
  }


  public boolean cacheResult(String pokemonShowdownId, boolean result) {
    resultsCache.putIfAbsent(pokemonShowdownId, result);
    return result;
  }
}