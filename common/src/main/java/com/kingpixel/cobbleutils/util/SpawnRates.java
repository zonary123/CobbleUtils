package com.kingpixel.cobbleutils.util;

import com.cobblemon.mod.common.api.spawning.BestSpawner;
import com.cobblemon.mod.common.api.spawning.CobblemonSpawnPools;
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.api.spawning.multiplier.WeightMultiplier;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Calculates and caches estimated spawn rates and rarities for Pokémon species
 * based on Cobblemon's world spawn pool and spawner bucket weights.
 */
public class SpawnRates {

  private static final float EVOLUTION_RARITY_MULTIPLIER = 0.2f;
  private static final String SPECIES_LANG_PREFIX = "cobblemon.species.";
  private static final String SPECIES_LANG_SUFFIX = ".name";

  // Volatile reference swapped atomically to provide lock-free reads and reload safety
  volatile Map<String, Float> rarity = Collections.emptyMap();

  public SpawnRates() {
  }

  /**
   * Initializes or recalculates the spawn rates cache from Cobblemon spawn pools.
   * Thread-safe and atomic snapshot update.
   */
  public synchronized void init() {
    Map<String, Float> worldBuckets = BestSpawner.INSTANCE.getConfig().getWorldBuckets();
    if (worldBuckets == null || worldBuckets.isEmpty()) {
      return;
    }

    Collection<SpawnDetail> spawnDetails = CobblemonSpawnPools.WORLD_SPAWN_POOL.getDetails();
    if (spawnDetails == null || spawnDetails.isEmpty()) {
      return;
    }

    Map<String, Map<String, Float>> bucketPokemonWeights = new HashMap<>();
    for (String bucket : worldBuckets.keySet()) {
      bucketPokemonWeights.put(bucket, new HashMap<>());
    }

    Set<String> allPokemon = new HashSet<>();
    populateBuckets(spawnDetails, bucketPokemonWeights, allPokemon);

    Map<String, Float> bucketTotalWeights = calculateBucketTotals(bucketPokemonWeights);
    Map<String, Float> computedRarities = computeRarities(allPokemon, bucketPokemonWeights, bucketTotalWeights, worldBuckets);

    // Atomically swap the snapshot
    this.rarity = Collections.unmodifiableMap(computedRarities);
  }

  private void populateBuckets(
    Collection<SpawnDetail> spawnDetails,
    Map<String, Map<String, Float>> bucketPokemonWeights,
    Set<String> allPokemon
  ) {
    for (SpawnDetail detail : spawnDetails) {
      String bucket = detail.getBucket();
      if (bucket == null) continue;

      Map<String, Float> bucketMap = bucketPokemonWeights.get(bucket);
      if (bucketMap == null) continue;

      String speciesId = extractNormalizedSpeciesId(detail);
      if (speciesId.isEmpty()) continue;

      float weight = calculateEffectiveWeight(detail);
      bucketMap.merge(speciesId, weight, Math::max);
      allPokemon.add(speciesId);
    }
  }

  private float calculateEffectiveWeight(SpawnDetail detail) {
    float maxMultiplier = 0f;
    for (WeightMultiplier multiplier : detail.getWeightMultipliers()) {
      if (multiplier.getMultiplier() > maxMultiplier) {
        maxMultiplier = multiplier.getMultiplier();
      }
    }
    return Math.max(detail.getWeight() * maxMultiplier, detail.getWeight());
  }

  private Map<String, Float> calculateBucketTotals(Map<String, Map<String, Float>> bucketPokemonWeights) {
    Map<String, Float> totals = new HashMap<>();
    for (Map.Entry<String, Map<String, Float>> entry : bucketPokemonWeights.entrySet()) {
      float sum = 0f;
      for (float weight : entry.getValue().values()) {
        sum += weight;
      }
      totals.put(entry.getKey(), sum);
    }
    return totals;
  }

  private Map<String, Float> computeRarities(
    Set<String> allPokemon,
    Map<String, Map<String, Float>> bucketPokemonWeights,
    Map<String, Float> bucketTotalWeights,
    Map<String, Float> worldBuckets
  ) {
    Map<String, Float> result = new HashMap<>();

    for (String speciesId : allPokemon) {
      double maxWeightedRate = 0.0;

      for (Map.Entry<String, Map<String, Float>> entry : bucketPokemonWeights.entrySet()) {
        String bucket = entry.getKey();
        Map<String, Float> bucketMap = entry.getValue();

        Float weight = bucketMap.get(speciesId);
        if (weight == null) continue;

        float totalWeightInBucket = bucketTotalWeights.getOrDefault(bucket, 0f);
        if (totalWeightInBucket <= 0f) continue;

        float bucketWeight = worldBuckets.getOrDefault(bucket, 0f);
        double rateInBucket = (double) weight / (double) totalWeightInBucket;
        double overallRate = rateInBucket * (double) bucketWeight * 100.0;

        if (overallRate > maxWeightedRate) {
          maxWeightedRate = overallRate;
        }
      }

      result.put(speciesId, (float) maxWeightedRate);
    }

    return result;
  }

  private String extractNormalizedSpeciesId(SpawnDetail detail) {
    String rawName = null;
    if (detail instanceof PokemonSpawnDetail pokemonDetail && pokemonDetail.getPokemon() != null) {
      rawName = pokemonDetail.getPokemon().getSpecies();
    }
    if (rawName == null || rawName.isBlank()) {
      rawName = detail.getName().getString();
    }
    return normalizeSpeciesId(rawName);
  }

  private String normalizeSpeciesId(String name) {
    if (name == null || name.isBlank()) return "";
    String normalized = name.trim();
    if (normalized.startsWith(SPECIES_LANG_PREFIX)) {
      normalized = normalized.substring(SPECIES_LANG_PREFIX.length());
      if (normalized.endsWith(SPECIES_LANG_SUFFIX)) {
        normalized = normalized.substring(0, normalized.length() - SPECIES_LANG_SUFFIX.length());
      }
    }
    if (normalized.contains(":")) {
      normalized = normalized.substring(normalized.indexOf(':') + 1);
    }
    return normalized.toLowerCase().replace(" ", "").replace("_", "").replace("-", "");
  }

  private void ensureInitialized() {
    if (rarity.isEmpty()) {
      init();
    }
  }

  /**
   * Retrieves the spawn rarity percentage for a Pokemon.
   *
   * @param pokemon The Pokemon instance to query.
   * @return Spawn rarity percentage (0-100), or -1 if unknown.
   */
  public float getRarity(Pokemon pokemon) {
    ensureInitialized();
    if (pokemon == null || pokemon.getSpecies() == null) return -1f;

    String showdownId = pokemon.getSpecies().showdownId();
    Float directRarity = rarity.get(showdownId);
    if (directRarity != null) {
      return directRarity;
    }

    // Fallback for evolved forms not directly registered in world spawn pools
    Pokemon firstEvolution = PokemonUtils.getFirstEvolution(pokemon);
    if (firstEvolution != null && firstEvolution.getSpecies() != null) {
      String baseId = firstEvolution.getSpecies().showdownId();
      if (!baseId.equals(showdownId)) {
        Float baseRarity = rarity.get(baseId);
        if (baseRarity != null) {
          return baseRarity * EVOLUTION_RARITY_MULTIPLIER;
        }
      }
    }

    return -1f;
  }

  /**
   * Retrieves the spawn rarity percentage for a Species.
   *
   * @param species The Species to query.
   * @return Spawn rarity percentage (0-100), or -1 if unknown.
   */
  public float getRarity(Species species) {
    ensureInitialized();
    if (species == null) return -1f;

    String showdownId = species.showdownId();
    Float directRarity = rarity.get(showdownId);
    if (directRarity != null) {
      return directRarity;
    }

    // Direct pre-evolution traversal without creating dummy Pokemon entity
    Species base = PokemonUtils.getFirstPreEvolution(species);
    if (base != null && !base.showdownId().equals(showdownId)) {
      Float baseRarity = rarity.get(base.showdownId());
      if (baseRarity != null) {
        return baseRarity * EVOLUTION_RARITY_MULTIPLIER;
      }
    }

    return getRarity(species.create(1));
  }

  /**
   * Returns an unmodifiable snapshot of the calculated rarity map.
   */
  public Map<String, Float> getRarityMap() {
    ensureInitialized();
    return rarity;
  }
}