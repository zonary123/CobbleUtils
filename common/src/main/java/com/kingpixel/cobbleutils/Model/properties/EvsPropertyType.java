package com.kingpixel.cobbleutils.Model.properties;

import com.cobblemon.mod.common.api.properties.CustomPokemonPropertyType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EvsPropertyType implements CustomPokemonPropertyType<EvsProperty> {
  public static final EvsPropertyType INSTANCE = new EvsPropertyType();

  public EvsPropertyType() {
  }

  @NotNull @Override public Iterable<String> getKeys() {
    return List.of("evs", "ev");
  }

  @Nullable @Override public EvsProperty fromString(@Nullable String s) {
    if (s == null) return new EvsProperty("0,0,0,0,0,0");
    return new EvsProperty(s);
  }

  @NotNull @Override public Collection<String> examples() {
    Set<String> examples = new HashSet<>();
    examples.add("0,252,0,0,4,252");
    examples.add("252,0,0,252,4,0");
    examples.add("0");
    examples.add("reset");
    examples.add("max");
    return examples;
  }

  @Override public boolean getNeedsKey() {
    return true;
  }
}
