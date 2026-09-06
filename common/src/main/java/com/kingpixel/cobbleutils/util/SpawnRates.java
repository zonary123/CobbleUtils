package com.kingpixel.cobbleutils.util;

import com.cobblemon.mod.common.api.spawning.BestSpawner;
import com.cobblemon.mod.common.api.spawning.CobblemonSpawnPools;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.api.spawning.multiplier.WeightMultiplier;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2023
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
public class SpawnRates {

  // Stores all rarities to easily reference for a price.
  HashMap<String, Float> rarity;

  public SpawnRates() {
    rarity = new HashMap<>();
  }

  public void init() {
    ArrayList<SpawnDetail> spawnDetails = new ArrayList<>(CobblemonSpawnPools.WORLD_SPAWN_POOL.getDetails());
    Map<String, Float> worldBuckets = BestSpawner.INSTANCE.getConfig().getWorldBuckets();

    HashMap<String, HashMap<String, Float>> buckets = new HashMap<>();
    Set<String> pokemon = new HashSet<>();

    if (worldBuckets != null) {
      for (String bucket : worldBuckets.keySet()) {
        buckets.put(bucket, new HashMap<>());
      }
    }

    populateBuckets(spawnDetails, buckets, pokemon);
    HashMap<String, Float> totalWeights = calculateTotalWeights(buckets);
    calculatePokemonRarities(pokemon, buckets, totalWeights, worldBuckets);
  }

  private void populateBuckets(
    ArrayList<SpawnDetail> spawnDetails,
    HashMap<String, HashMap<String, Float>> buckets,
    Set<String> pokemon
  ) {
    for (SpawnDetail detail : spawnDetails) {
      String bucket = detail.getBucket();
      if (bucket == null || !buckets.containsKey(bucket)) continue;

      float highestWeight = calculateHighestWeight(detail);
      HashMap<String, Float> bucketMap = buckets.get(bucket);
      String pokeName = detail.getName().getString();

      if (!bucketMap.containsKey(pokeName) || highestWeight > bucketMap.get(pokeName)) {
        bucketMap.put(pokeName, highestWeight);
      }
      pokemon.add(pokeName);
    }
  }

  private float calculateHighestWeight(SpawnDetail detail) {
    float weightMultiplier = 0;
    for (WeightMultiplier multiplier : detail.getWeightMultipliers()) {
      if (multiplier.getMultiplier() > weightMultiplier) {
        weightMultiplier = multiplier.getMultiplier();
      }
    }
    return Math.max(detail.getWeight() * weightMultiplier, detail.getWeight());
  }

  private HashMap<String, Float> calculateTotalWeights(HashMap<String, HashMap<String, Float>> buckets) {
    HashMap<String, Float> totalWeights = new HashMap<>();
    for (Map.Entry<String, HashMap<String, Float>> entry : buckets.entrySet()) {
      float bucketTotalWeight = 0;
      for (float weight : entry.getValue().values()) {
        bucketTotalWeight += weight;
      }
      totalWeights.put(entry.getKey(), bucketTotalWeight);
    }
    return totalWeights;
  }

  private void calculatePokemonRarities(
    Set<String> pokemon,
    HashMap<String, HashMap<String, Float>> buckets,
    HashMap<String, Float> totalWeights,
    Map<String, Float> worldBuckets
  ) {
    for (String poke : pokemon) {
      BigDecimal highestWeight = calculateHighestWeightForPokemon(poke, buckets, totalWeights, worldBuckets);
      rarity.put(poke.toLowerCase(), highestWeight.multiply(BigDecimal.valueOf(100)).floatValue());
    }
  }

  private BigDecimal calculateHighestWeightForPokemon(
    String poke,
    HashMap<String, HashMap<String, Float>> buckets,
    HashMap<String, Float> totalWeights,
    Map<String, Float> worldBuckets
  ) {
    BigDecimal highestWeight = BigDecimal.ZERO;
    for (Map.Entry<String, HashMap<String, Float>> entry : buckets.entrySet()) {
      String bucket = entry.getKey();
      HashMap<String, Float> bucketMap = entry.getValue();

      if (bucketMap.containsKey(poke)) {
        float totalWeightInBucket = totalWeights.getOrDefault(bucket, 0f);
        if (totalWeightInBucket <= 0) continue;

        BigDecimal rarityInBucket = BigDecimal.valueOf(bucketMap.get(poke) / totalWeightInBucket);
        float bucketWeight = worldBuckets != null ? worldBuckets.getOrDefault(bucket, 0f) : 0f;
        BigDecimal totalWeight = rarityInBucket.multiply(BigDecimal.valueOf(bucketWeight));

        if (totalWeight.compareTo(highestWeight) > 0) {
          highestWeight = totalWeight;
        }
      }
    }
    return highestWeight;
  }

  /**
   * Gets the rarity hashmap.
   *
   * @return rarity of given pokemon as a float.
   */
  public float getRarity(Pokemon pokemon) {
    if (rarity.isEmpty()) init();
    var r = rarity.get(pokemon.getSpecies().showdownId());
    return r == null ? -1 : r;
  }

  public float getRarity(Species species) {
    if (rarity.isEmpty()) init();
    Pokemon p = species.create(1);
    return getRarity(p);
  }
}