package com.kingpixel.cobbleutils.Model;

import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 13/01/2025 2:17
 */
@Getter
@Setter
public class PokemonBlackList {
  private List<String> pokemons;
  private List<String> labels;
  private List<String> types;

  public PokemonBlackList() {
    this.pokemons = new ArrayList<>();
    pokemons.add("magikarp_Example");
    this.labels = new ArrayList<>();
    labels.add("gen1_Example");
    this.types = new ArrayList<>();
    types.add("water_Example");
  }

  public boolean isBlackListed(Pokemon pokemon) {
    boolean showdownId = this.pokemons.contains(pokemon.showdownId()) || this.pokemons.contains("*");
    boolean isLabel = pokemon.getForm().getLabels().stream().anyMatch(this.labels::contains);
    boolean isType;

    List<ElementalType> typeList = new ArrayList<>();
    pokemon.getTypes().forEach(typeList::add);
    isType = typeList.stream().anyMatch(type -> {
      String keyType = type.getResourceLocation().getPath();
      return this.types.contains(keyType);
    });
    return showdownId || isLabel || isType;

  }
}