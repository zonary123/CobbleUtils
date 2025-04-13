package com.kingpixel.cobbleutils.Model;

import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
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
  private List<String> forms;
  private List<String> aspects;
  private List<String> labels;
  private List<EggGroup> eggGroups;
  private List<String> types;

  public PokemonBlackList() {
    this.pokemons = new ArrayList<>();
    pokemons.add("ditto");
    pokemons.add("egg");
    pokemons.add("pokestop");
    this.labels = new ArrayList<>();
    labels.add("legendary");
    labels.add("mythical");
    labels.add("ultra_beast");
    this.types = new ArrayList<>();
    types.add("water_Example");
    this.forms = new ArrayList<>();
    forms.add("hisuian_Example");
    this.aspects = new ArrayList<>();
    aspects.add("gmax_Example");
    this.eggGroups = new ArrayList<>();
    eggGroups.add(EggGroup.DITTO);
  }

  public boolean isBlackListed(Pokemon pokemon) {
    if (pokemon.getForm().getEggGroups().stream().anyMatch(eggGroups::contains)) return true;
    if (pokemons.contains("*") || pokemons.contains(pokemon.getForm().showdownId()) || pokemons.contains(pokemon.showdownId()) || pokemons.contains(pokemon.getSpecies().showdownId()))
      return true;
    if (pokemon.getForm().getLabels().stream().anyMatch(labels::contains)) return true;
    if (forms.contains(pokemon.getForm().formOnlyShowdownId())) return true;

    List<ElementalType> typeList = new ArrayList<>();
    pokemon.getTypes().forEach(typeList::add);
    if (typeList.stream().anyMatch(type -> {
      String keyType = type.getResourceLocation().getPath();
      return this.types.contains(keyType);
    })) return true;
    
    for (String aspect : pokemon.getAspects()) {
      if (this.aspects.contains(aspect)) return true;
    }
    return false;
  }
}