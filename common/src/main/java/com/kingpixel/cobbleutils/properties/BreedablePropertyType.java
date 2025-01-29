package com.kingpixel.cobbleutils.properties;

import com.cobblemon.mod.common.api.properties.CustomPokemonPropertyType;
import com.kingpixel.cobbleutils.Model.CobbleUtilsTags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * @author Carlos Varas Alonso - 04/08/2024 19:40
 */
public class BreedablePropertyType implements CustomPokemonPropertyType<BreedableProperty> {
  public static final BreedablePropertyType INSTANCE = new BreedablePropertyType();

  public BreedablePropertyType() {
  }

  public static BreedablePropertyType getInstance() {
    return INSTANCE;
  }

  @NotNull @Override public Iterable<String> getKeys() {
    return Collections.singleton(CobbleUtilsTags.BREEDABLE_TAG);
  }

  @Override public boolean getNeedsKey() {
    return true;
  }

  @Nullable @Override public BreedableProperty fromString(String s) {
    boolean value = s == null || Boolean.parseBoolean(s);
    return new BreedableProperty(value);
  }

  @NotNull @Override public Collection<String> examples() {
    return Set.of("true", "false");
  }
}
