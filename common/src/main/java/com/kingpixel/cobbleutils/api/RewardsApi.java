package com.kingpixel.cobbleutils.api;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemChance;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author Carlos Varas Alonso - 02/09/2025 12:25
 */
public class RewardsApi {

  public static Map<String, ItemChance> getAllRewards() {
    return CobbleUtils.rewardsC.getRewards();
  }

  public static @Nullable ItemChance getReward(String id) {
    ItemChance itemChance = CobbleUtils.rewardsC.getRewards().getOrDefault(id, null);
    if (itemChance == null) {
      CobbleUtils.LOGGER.error("Reward with id " + id + " not found!");
    } else return itemChance;
    return null;
  }
}
