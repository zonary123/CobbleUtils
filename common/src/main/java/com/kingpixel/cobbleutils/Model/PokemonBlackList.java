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
    for (EggGroup eggGroup : eggGroups) {
      if (pokemon.getForm().getEggGroups().contains(eggGroup)) return true;
    }
    boolean showdownId = this.pokemons.contains(pokemon.getForm().showdownId())
      || this.pokemons.contains("*")
      || this.pokemons.contains(pokemon.showdownId())
      || this.pokemons.contains(pokemon.getSpecies().showdownId());
    if (showdownId) return true;
    boolean isLabel = pokemon.getForm().getLabels().stream().anyMatch(this.labels::contains);
    if (isLabel) return true;
    boolean isForm = this.forms.contains(pokemon.getForm().formOnlyShowdownId());
    if (isForm) return true;
    boolean isType;

    List<ElementalType> typeList = new ArrayList<>();
    pokemon.getTypes().forEach(typeList::add);
    isType = typeList.stream().anyMatch(type -> {
      String keyType = type.getResourceLocation().getPath();
      return this.types.contains(keyType);
    });
    if (isType) return true;
    for (String aspect : pokemon.getAspects()) {
      if (this.aspects.contains(aspect)) return true;
    }
    return false;
  }
}