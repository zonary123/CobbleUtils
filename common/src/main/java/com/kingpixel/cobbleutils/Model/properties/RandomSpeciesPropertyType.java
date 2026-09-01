package com.kingpixel.cobbleutils.Model.properties;

import com.cobblemon.mod.common.api.properties.CustomPokemonPropertyType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RandomSpeciesPropertyType implements CustomPokemonPropertyType<RandomSpeciesProperty> {
  public static final RandomSpeciesPropertyType INSTANCE = new RandomSpeciesPropertyType();

  public RandomSpeciesPropertyType() {
  }

  @NotNull @Override public Iterable<String> getKeys() {
    return List.of("random_species", "random_pokemon");
  }

  @Nullable @Override public RandomSpeciesProperty fromString(@Nullable String s) {
    if (s == null) return new RandomSpeciesProperty("all");
    return new RandomSpeciesProperty(s);
  }

  @NotNull @Override public Collection<String> examples() {
    Set<String> examples = new HashSet<>();
    examples.add("legendary");
    examples.add("mythical");
    examples.add("starter");
    examples.add("ultrabeast");
    examples.add("paradox");
    examples.add("gen1");
    examples.add("all");
    return examples;
  }

  @Override public boolean getNeedsKey() {
    return true;
  }
}
