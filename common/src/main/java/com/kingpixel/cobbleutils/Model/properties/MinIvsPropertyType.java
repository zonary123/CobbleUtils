package com.kingpixel.cobbleutils.Model.properties;

import com.cobblemon.mod.common.api.properties.CustomPokemonPropertyType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Carlos Varas Alonso - 04/08/2024 19:40
 */
public class MinIvsPropertyType implements CustomPokemonPropertyType<MinIvsProperty> {
  private static final MinIvsPropertyType INSTANCE = new MinIvsPropertyType();

  public MinIvsPropertyType() {
  }

  public static MinIvsPropertyType getInstance() {
    return INSTANCE;
  }

  @NotNull @Override public Iterable<String> getKeys() {
    return Collections.singleton("min_ivs");
  }


  @Nullable @Override public MinIvsProperty fromString(@Nullable String s) {
    return new MinIvsProperty(s);
  }

  @NotNull @Override public Collection<String> examples() {
    Set<String> examples = new HashSet<>();
    examples.add("0_6");
    examples.add("31_6");
    return examples;
  }

  @Override public boolean getNeedsKey() {
    return true;
  }
}
