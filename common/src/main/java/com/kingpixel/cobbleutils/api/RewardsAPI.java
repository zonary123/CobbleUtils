package com.kingpixel.cobbleutils.api;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.Model.rewards.AdvancedReward;
import com.kingpixel.cobbleutils.Model.rewards.Reward;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Carlos Varas Alonso - 02/09/2025 12:25
 */
public class RewardsAPI {
  private static final Map<String, AdvancedReward> ADVANCED_REWARDS_TEMPLATE = new ConcurrentHashMap<>();

  public static void registerAdvancedReward(String id, AdvancedReward advancedReward) {
    if (ADVANCED_REWARDS_TEMPLATE.containsKey(id)) {
      CobbleUtils.LOGGER.error("Advanced reward with id " + id + " already exists!");
    } else ADVANCED_REWARDS_TEMPLATE.put(id, advancedReward);
  }

  public static void giveAdvancedReward(UUID playerUUID, AdvancedReward advancedReward) {
    AdvancedReward rewardToGive = advancedReward;
    if (rewardToGive.getId() != null && !rewardToGive.getId().isEmpty()) {
      rewardToGive = ADVANCED_REWARDS_TEMPLATE.getOrDefault(advancedReward.getId(), advancedReward);
    }
    rewardToGive.giveRewards(playerUUID);
  }

  private static final Map<String, Reward> REWARDS_TEMPLATE = new ConcurrentHashMap<>();

  public static void registerReward(Reward reward) {
    String id = reward.getId();
    if (id == null || id.isEmpty()) {
      CobbleUtils.LOGGER.error("Reward id cannot be null or empty!");
      return;
    }
    if (REWARDS_TEMPLATE.containsKey(id)) {
      CobbleUtils.LOGGER.error("Reward with id " + id + " already exists!");
    } else REWARDS_TEMPLATE.put(id, reward);
  }

  public static CompletableFuture<Boolean> giveReward(UUID playerUUID, Reward reward) {
    Reward rewardToGive = reward;
    if (rewardToGive.getId() != null && !rewardToGive.getId().isEmpty()) {
      rewardToGive = REWARDS_TEMPLATE.getOrDefault(reward.getId(), reward);
    }
    ServerPlayerEntity player = CobbleUtils.server.getPlayerManager().getPlayer(playerUUID);
    return player == null ? rewardToGive.giveToPlayerDisconnected(playerUUID) : rewardToGive.giveToPlayer(player);
  }

  public static @Nullable Reward getRewardTemplate(String id) {
    Reward reward = REWARDS_TEMPLATE.getOrDefault(id, null);
    if (reward == null) CobbleUtils.LOGGER.error("Reward with id " + id + " not found!");
    return reward;
  }

  public static @Nullable ItemChance getReward(String id) {
    ItemChance itemChance = CobbleUtils.rewardsConfig.getRewards().getOrDefault(id, null);
    if (itemChance == null) CobbleUtils.LOGGER.error("Reward with id " + id + " not found!");
    return itemChance;
  }

  public static AdvancedReward getAdvancedRewardTemplate(String advancedRewardId) {
    AdvancedReward advancedReward = ADVANCED_REWARDS_TEMPLATE.getOrDefault(advancedRewardId, null);
    if (advancedReward == null) CobbleUtils.LOGGER.error("Advanced reward with id " + advancedRewardId + " not found!");
    return advancedReward;
  }
}
