package com.kingpixel.cobbleutils.Model.properties;

import com.cobblemon.mod.common.api.properties.CustomPokemonPropertyType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class LegendaryPropertyType implements CustomPokemonPropertyType<LegendaryProperty> {
  public static final LegendaryPropertyType INSTANCE = new LegendaryPropertyType();

  public LegendaryPropertyType() {
  }

  @NotNull @Override public Iterable<String> getKeys() {
    return Collections.singleton("legendary");
  }

  @Nullable @Override public LegendaryProperty fromString(@Nullable String s) {
    if (s == null) return new LegendaryProperty("no");
    return new LegendaryProperty(s);
  }

  @NotNull @Override public Collection<String> examples() {
    Set<String> examples = new HashSet<>();
    examples.add("yes");
    examples.add("no");
    return examples;
  }

  @Override public boolean getNeedsKey() {
    return true;
  }
}
