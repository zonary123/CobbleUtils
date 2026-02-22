package com.kingpixel.cobbleutils.Model;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonPropertyExtractor;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.spawning.CobblemonSpawnPools;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.validators.AdvancedPokemonChance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PokemonSpawner {
  // <Bucket, Pokemons>
  private static final Map<String, List<Pokemon>> BUCKET_POKEMONS = new ConcurrentHashMap<>();
  // List<Pokemon>
  private transient final List<Pokemon> pokemons = new ArrayList<>();


  @Builder.Default
  private Type type = Type.WHITELIST;
  // Buckets
  @Builder.Default
  private Map<String, Double> buckets = Map.of(
    "bucket1", 100.0,
    "bucket2", 100.0
  );
  // BlackList
  @Builder.Default
  public PokemonBlackList whitelist = new PokemonBlackList();
  // Use Chances
  @Builder.Default
  private boolean useChances = false;
  @Builder.Default
  private List<AdvancedPokemonChance> pokemonsChances = AdvancedPokemonChance.getDefault();

  public Pokemon getPokemon() {
    var pokemons = loadPokemons();
    if (pokemons.isEmpty()) return null;
    int index = (int) (Math.random() * pokemons.size());
    return createPokemon(pokemons.get(index));
  }

  public List<Pokemon> getPokemons(int amount) {
    List<Pokemon> result = new ArrayList<>();
    for (int i = 0; i < amount; i++) {
      result.add(getPokemon());
    }
    return result;
  }

  private Pokemon createPokemon(Pokemon pokemon) {
    Pokemon copy = pokemon.clone(true, CobbleUtils.server.getRegistryManager());
    copy.createPokemonProperties(List.of(
      PokemonPropertyExtractor.LEVEL,
      PokemonPropertyExtractor.NATURE,
      PokemonPropertyExtractor.IVS,
      PokemonPropertyExtractor.GENDER,
      PokemonPropertyExtractor.POKEBALL,
      PokemonPropertyExtractor.FORM
    )).apply(copy);
    return copy;
  }

  public synchronized List<Pokemon> loadPokemons() {
    return switch (type) {
      case BUCKETS -> {
        if (BUCKET_POKEMONS.isEmpty()) loadPokemonsFromBuckets();
        yield getRandomBucketPokemons();
      }
      case WHITELIST -> {
        if (pokemons.isEmpty()) loadPokemonsFromWhitelist();
        yield pokemons;
      }
      case POKEMON_CHANCES -> AdvancedPokemonChance.getPokemons(pokemonsChances);
    };
  }

  private List<Pokemon> getRandomBucketPokemons() {
    if (buckets == null || buckets.isEmpty()) return List.of();

    double totalWeight = buckets.values().stream()
      .mapToDouble(Double::doubleValue)
      .sum();

    double random = Math.random() * totalWeight;

    double current = 0.0;
    for (Map.Entry<String, Double> entry : buckets.entrySet()) {
      current += entry.getValue();
      if (random <= current) {
        List<Pokemon> pokemons = BUCKET_POKEMONS.get(entry.getKey());
        if (pokemons == null || pokemons.isEmpty()) continue;
        return pokemons;
      }
    }

    var list = BUCKET_POKEMONS.values().stream().toList();
    int size = list.size();
    return list.get((int) (Math.random() * size));
  }

  private synchronized void loadPokemonsFromBuckets() {
    var spawnDetails = CobblemonSpawnPools.WORLD_SPAWN_POOL.getDetails();
    Set<String> buckets = new HashSet<>();
    for (SpawnDetail detail : spawnDetails) {
      if (buckets.add(detail.getBucket().getName())) {
        CobbleUtils.LOGGER.info("Loading bucket: " + detail.getBucket().getName());
      }
      BUCKET_POKEMONS.computeIfAbsent(detail.getBucket().getName(), k -> new ArrayList<>()).add(PokemonProperties.Companion.parse(detail.getName().getString()).create());
    }
  }

  private synchronized void loadPokemonsFromWhitelist() {
    if (!pokemons.isEmpty()) return;
    List<Pokemon> allPokemons = new ArrayList<>();
    Set<String> uniquePokemonIds = new HashSet<>();
    List<Species> species = new ArrayList<>(PokemonSpecies.getSpecies().stream().toList());
    species.sort(Comparator.comparing(Species::getNationalPokedexNumber));

    species.forEach(pokemon -> {
      List<FormData> forms = pokemon.getForms();
      if (forms.isEmpty()) {
        Pokemon p = pokemon.create(1);
        if (uniquePokemonIds.add(p.getForm().showdownId())) {
          allPokemons.add(p);
        }
      } else {
        forms.forEach(form -> {
          List<String> aspects = form.getAspects();
          if (aspects.isEmpty()) {
            Pokemon p = pokemon.create(1);
            if (uniquePokemonIds.add(p.getForm().showdownId())) {
              allPokemons.add(p);
            }
          } else {
            aspects.forEach(aspect -> {
              String formattedAspect = aspect.replace("-", "_");
              int lastUnderscore = formattedAspect.lastIndexOf("_");
              if (lastUnderscore != -1) {
                formattedAspect = formattedAspect.substring(0, lastUnderscore) + "=" + formattedAspect.substring(lastUnderscore + 1);
              }
              Pokemon p = PokemonProperties.Companion.parse(pokemon.showdownId() + " " + formattedAspect).create();
              if (uniquePokemonIds.add(p.getForm().showdownId())) {
                allPokemons.add(p);
              }
            });
          }
        });
      }
    });
    pokemons.addAll(allPokemons);
  }

  public enum Type {
    BUCKETS,
    WHITELIST,
    POKEMON_CHANCES
  }
}
