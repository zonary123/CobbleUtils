package com.kingpixel.cobbleutils.Model.properties;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.labels.CobblemonPokemonLabels;
import com.cobblemon.mod.common.api.properties.CustomPokemonProperty;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.kingpixel.cobbleutils.util.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RandomSpeciesProperty implements CustomPokemonProperty {
  private static final Map<String, List<Species>> CACHE = new ConcurrentHashMap<>();

  private final String value;

  public RandomSpeciesProperty(String value) {
    this.value = value;
  }

  @Override
  public @NotNull String asString() {
    return "random";
  }

  @Override
  public void apply(@NotNull Pokemon pokemon) {
    if (this.value == null || this.value.isBlank())
      return;

    List<Species> candidates = getCandidates(this.value.trim().toLowerCase());
    if (candidates.isEmpty())
      return;

    Species selected = candidates.get(Utils.getRandom().nextInt(candidates.size()));
    pokemon.setSpecies(selected);
  }

  @Override
  public boolean matches(@NotNull Pokemon pokemon) {
    if (this.value == null || this.value.isBlank())
      return true;

    List<Species> candidates = getCandidates(this.value.trim().toLowerCase());
    return candidates.contains(pokemon.getSpecies());
  }

  public static List<Species> getCandidates(String category) {
    return CACHE.computeIfAbsent(category, cat -> {
      List<Species> pool = new ArrayList<>();
      Collection<Species> all = PokemonSpecies.getSpecies();

      for (Species s : all) {
        if (!s.getImplemented())
          continue;

        int dex = s.getNationalPokedexNumber();
        boolean match = switch (cat) {
          case "all", "any", "true", "yes" -> true;
          case "legendary" ->
            s.getLabels().contains(CobblemonPokemonLabels.LEGENDARY) || s.getLabels().contains("legendary");
          case "mythical" ->
            s.getLabels().contains(CobblemonPokemonLabels.MYTHICAL) || s.getLabels().contains("mythical");
          case "ultrabeast", "ultra_beast" -> s.getLabels().contains(CobblemonPokemonLabels.ULTRA_BEAST)
              || s.getLabels().contains("ultra_beast") || s.getLabels().contains("ultrabeast");
          case "starter" -> s.getLabels().contains("starter");
          case "baby" -> s.getLabels().contains("baby");
          case "paradox" -> s.getLabels().contains("paradox");
          case "pseudo_legendary", "pseudolegendary" ->
            s.getLabels().contains("pseudo_legendary") || s.getLabels().contains("pseudolegendary");
          case "fossil" -> s.getLabels().contains("fossil");
          case "gen1" -> dex >= 1 && dex <= 151;
          case "gen2" -> dex >= 152 && dex <= 251;
          case "gen3" -> dex >= 252 && dex <= 386;
          case "gen4" -> dex >= 387 && dex <= 493;
          case "gen5" -> dex >= 494 && dex <= 649;
          case "gen6" -> dex >= 650 && dex <= 721;
          case "gen7" -> dex >= 722 && dex <= 809;
          case "gen8" -> dex >= 810 && dex <= 905;
          case "gen9" -> dex >= 906 && dex <= 1025;
          default -> s.getLabels().stream().anyMatch(l -> l.equalsIgnoreCase(cat));
        };

        if (match) {
          pool.add(s);
        }
      }
      return List.copyOf(pool);
    });
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
