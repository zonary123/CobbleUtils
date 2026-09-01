package com.kingpixel.cobbleutils.Model.properties;

import com.cobblemon.mod.common.api.properties.CustomPokemonPropertyType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IvsPropertyType implements CustomPokemonPropertyType<IvsProperty> {
  public static final IvsPropertyType INSTANCE = new IvsPropertyType();

  public IvsPropertyType() {
  }

  @NotNull @Override public Iterable<String> getKeys() {
    return List.of("ivs", "iv");
  }

  @Nullable @Override public IvsProperty fromString(@Nullable String s) {
    if (s == null) return new IvsProperty("31,31,31,31,31,31");
    return new IvsProperty(s);
  }

  @NotNull @Override public Collection<String> examples() {
    Set<String> examples = new HashSet<>();
    examples.add("31,31,31,31,31,31");
    examples.add("31,31,31,31,31,0");
    examples.add("perfect");
    examples.add("0,0,0,0,0,0");
    examples.add("random");
    return examples;
  }

  @Override public boolean getNeedsKey() {
    return true;
  }
}
