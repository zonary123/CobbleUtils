package com.kingpixel.cobbleutils.util;

import com.cobblemon.mod.common.api.spawning.BestSpawner;
import com.cobblemon.mod.common.api.spawning.CobblemonSpawnPools;
import com.cobblemon.mod.common.api.spawning.SpawnBucket;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.api.spawning.multiplier.WeightMultiplier;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
    // Checks for highest value for each key and adds the key with the highest
    // weight.


    // Holds all of the buckets as a key, with another hashmap that will hold the
    // Pokemon
    // and their weights for that bucket as the value.
    HashMap<SpawnBucket, HashMap<String, Float>> buckets = new HashMap<>();
    Set<String> pokemon = new HashSet<>();

    // Gets all buckets and adds them to the HashMap.
    for (SpawnBucket bucket : BestSpawner.INSTANCE.getConfig().getBuckets()) {
      buckets.put(bucket, new HashMap<>());
    }

    // For each SpawnDetail
    for (SpawnDetail detail : spawnDetails) {

      // Finds the highest weight multiplier.
      float weightMultiplier = 0;
      for (WeightMultiplier multiplier : detail.getWeightMultipliers()) {
        if (multiplier.getMultiplier() > weightMultiplier) {
          weightMultiplier = multiplier.getMultiplier();
        }
      }

      // Makes sure there was a weight multiplier. Chooses highest weight.
      float highestWeight = Math.max(detail.getWeight() * weightMultiplier, detail.getWeight());

      // If the bucket of the detail doesn't contain the Pokemon, or the detail value
      // is higher than
      // the currently saved value, add the details combined weight.
      if (!buckets.get(detail.getBucket()).containsKey(detail.getName().getString()) ||
        highestWeight > buckets.get(detail.getBucket()).get(detail.getName().getString())) {

        // Adds the weight to the bucket.
        buckets.get(detail.getBucket()).put(detail.getName().getString(), highestWeight);

      }

      // Stores the Pokemon name so we can compare the different weights after
      pokemon.add(detail.getName().getString());
    }

    // Calculates the total weight of all buckets.
    HashMap<SpawnBucket, Float> totalWeights = new HashMap<>();
    for (SpawnBucket bucket : buckets.keySet()) {

      // Finds the total weight of all Pokemon in the bucket.
      float bucketTotalWeight = 0;
      for (float weight : new ArrayList<>(buckets.get(bucket).values())) {
        bucketTotalWeight += weight;
      }
      totalWeights.put(bucket, bucketTotalWeight);
    }

    for (String poke : pokemon) {
      // Iterate over each bucket and find the highest weight of them all.
      BigDecimal highestWeight = new BigDecimal(0);

      // Checks each bucket, calculates the weight and compares it to the current one.
      for (SpawnBucket bucket : buckets.keySet()) {

        // Calculates the weight and compares to previous ones to find the highest.
        if (buckets.get(bucket).containsKey(poke)) {
          BigDecimal rarityInBucket = BigDecimal.valueOf(buckets.get(bucket).get(poke) / totalWeights.get(bucket));

          BigDecimal totalWeight = rarityInBucket.multiply(BigDecimal.valueOf(bucket.getWeight()));

          if (totalWeight.compareTo(highestWeight) > 0) {
            highestWeight = totalWeight;
          }
        }
      }
      rarity.put(poke.toLowerCase(), highestWeight.multiply(BigDecimal.valueOf(100)).floatValue());
    }
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