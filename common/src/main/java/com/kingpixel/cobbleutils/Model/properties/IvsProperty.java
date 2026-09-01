package com.kingpixel.cobbleutils.Model.properties;

import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.properties.CustomPokemonProperty;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.util.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class IvsProperty implements CustomPokemonProperty {
  private static final List<Stat> STAT_ORDER = List.of(
      Stats.HP,
      Stats.ATTACK,
      Stats.DEFENCE,
      Stats.SPECIAL_ATTACK,
      Stats.SPECIAL_DEFENCE,
      Stats.SPEED);

  private final String value;

  public IvsProperty(String value) {
    this.value = value;
  }

  @Override
  public @NotNull String asString() {
    return "ivs";
  }

  @Override
  public void apply(@NotNull Pokemon pokemon) {
    if (this.value == null || this.value.isBlank())
      return;

    String clean = this.value.trim().toLowerCase();

    if (clean.equals("perfect") || clean.equals("max") || clean.equals("all31")) {
      for (Stat stat : STAT_ORDER) {
        pokemon.getIvs().set(stat, 31);
      }
      return;
    }

    if (clean.equals("zero") || clean.equals("min") || clean.equals("all0")) {
      for (Stat stat : STAT_ORDER) {
        pokemon.getIvs().set(stat, 0);
      }
      return;
    }

    if (clean.equals("random")) {
      for (Stat stat : STAT_ORDER) {
        pokemon.getIvs().set(stat, Utils.getRandom().nextInt(0, 32));
      }
      return;
    }

    String[] parts = clean.split("[,/]");

    if (parts.length == 1) {
      try {
        int singleIv = Math.max(0, Math.min(Integer.parseInt(parts[0].trim()), 31));
        for (Stat stat : STAT_ORDER) {
          pokemon.getIvs().set(stat, singleIv);
        }
      } catch (NumberFormatException ignored) {
      }
      return;
    }

    for (int i = 0; i < parts.length && i < STAT_ORDER.size(); i++) {
      String part = parts[i].trim();
      Stat stat = STAT_ORDER.get(i);
      if (part.equals("r") || part.equals("random")) {
        pokemon.getIvs().set(stat, Utils.getRandom().nextInt(0, 32));
      } else {
        try {
          int iv = Math.max(0, Math.min(Integer.parseInt(part), 31));
          pokemon.getIvs().set(stat, iv);
        } catch (NumberFormatException ignored) {
        }
      }
    }
  }

  @Override
  public boolean matches(@NotNull Pokemon pokemon) {
    if (this.value == null || this.value.isBlank())
      return true;

    String clean = this.value.trim().toLowerCase();

    if (clean.equals("perfect") || clean.equals("max") || clean.equals("all31")) {
      for (Stat stat : STAT_ORDER) {
        if (pokemon.getIvs().getOrDefault(stat) != 31)
          return false;
      }
      return true;
    }

    if (clean.equals("zero") || clean.equals("min") || clean.equals("all0")) {
      for (Stat stat : STAT_ORDER) {
        if (pokemon.getIvs().getOrDefault(stat) != 0)
          return false;
      }
      return true;
    }

    if (clean.equals("random")) {
      return true;
    }

    String[] parts = clean.split("[,/]");

    if (parts.length == 1) {
      try {
        int expected = Math.max(0, Math.min(Integer.parseInt(parts[0].trim()), 31));
        for (Stat stat : STAT_ORDER) {
          if (pokemon.getIvs().getOrDefault(stat) != expected)
            return false;
        }
        return true;
      } catch (NumberFormatException ignored) {
        return false;
      }
    }

    for (int i = 0; i < parts.length && i < STAT_ORDER.size(); i++) {
      String part = parts[i].trim();
      if (part.equals("r") || part.equals("random") || part.equals("*"))
        continue;
      try {
        int expected = Math.max(0, Math.min(Integer.parseInt(part), 31));
        Stat stat = STAT_ORDER.get(i);
        if (pokemon.getIvs().getOrDefault(stat) != expected)
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
