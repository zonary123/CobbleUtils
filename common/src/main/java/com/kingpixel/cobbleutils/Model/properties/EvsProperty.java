package com.kingpixel.cobbleutils.Model.properties;

import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.properties.CustomPokemonProperty;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EvsProperty implements CustomPokemonProperty {
  private static final List<Stat> STAT_ORDER = List.of(
      Stats.HP,
      Stats.ATTACK,
      Stats.DEFENCE,
      Stats.SPECIAL_ATTACK,
      Stats.SPECIAL_DEFENCE,
      Stats.SPEED);

  private final String value;

  public EvsProperty(String value) {
    this.value = value;
  }

  @Override
  public @NotNull String asString() {
    return "evs";
  }

  @Override
  public void apply(@NotNull Pokemon pokemon) {
    if (this.value == null || this.value.isBlank())
      return;

    String clean = this.value.trim().toLowerCase();

    if (clean.equals("0") || clean.equals("zero") || clean.equals("reset") || clean.equals("clean")) {
      for (Stat stat : STAT_ORDER) {
        pokemon.getEvs().set(stat, 0);
      }
      return;
    }

    if (clean.equals("max")) {
      for (Stat stat : STAT_ORDER) {
        pokemon.getEvs().set(stat, 252);
      }
      return;
    }

    String[] parts = clean.split("[,/]");

    if (parts.length == 1) {
      try {
        int singleEv = Math.max(0, Math.min(Integer.parseInt(parts[0].trim()), 252));
        for (Stat stat : STAT_ORDER) {
          pokemon.getEvs().set(stat, singleEv);
        }
      } catch (NumberFormatException ignored) {
      }
      return;
    }

    for (int i = 0; i < parts.length && i < STAT_ORDER.size(); i++) {
      String part = parts[i].trim();
      try {
        int ev = Math.max(0, Math.min(Integer.parseInt(part), 252));
        Stat stat = STAT_ORDER.get(i);
        pokemon.getEvs().set(stat, ev);
      } catch (NumberFormatException ignored) {
      }
    }
  }

  @Override
  public boolean matches(@NotNull Pokemon pokemon) {
    if (this.value == null || this.value.isBlank())
      return true;

    String clean = this.value.trim().toLowerCase();

    if (clean.equals("0") || clean.equals("zero") || clean.equals("reset") || clean.equals("clean")) {
      for (Stat stat : STAT_ORDER) {
        if (pokemon.getEvs().getOrDefault(stat) != 0)
          return false;
      }
      return true;
    }

    if (clean.equals("max")) {
      for (Stat stat : STAT_ORDER) {
        if (pokemon.getEvs().getOrDefault(stat) != 252)
          return false;
      }
      return true;
    }

    String[] parts = clean.split("[,/]");

    if (parts.length == 1) {
      try {
        int expected = Math.max(0, Math.min(Integer.parseInt(parts[0].trim()), 252));
        for (Stat stat : STAT_ORDER) {
          if (pokemon.getEvs().getOrDefault(stat) != expected)
            return false;
        }
        return true;
      } catch (NumberFormatException ignored) {
        return false;
      }
    }

    for (int i = 0; i < parts.length && i < STAT_ORDER.size(); i++) {
      String part = parts[i].trim();
      if (part.equals("*"))
        continue;
      try {
        int expected = Math.max(0, Math.min(Integer.parseInt(part), 252));
        Stat stat = STAT_ORDER.get(i);
        if (pokemon.getEvs().getOrDefault(stat) != expected)
          return false;
      } catch (NumberFormatException ignored) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void apply(@NotNull PokemonEntity pokemonEntity) {
    if (pokemonEntity.getPokemon() != null) {
      apply(pokemonEntity.getPokemon());
    }
  }

  @Override
  public boolean matches(@NotNull PokemonEntity pokemonEntity) {
    return pokemonEntity.getPokemon() != null && matches(pokemonEntity.getPokemon());
  }
}
