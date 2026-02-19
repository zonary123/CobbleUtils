package com.kingpixel.cobbleutils.database.users.models;

import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.Model.rewards.Reward;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 06/10/2025 5:11
 */
@EqualsAndHashCode(callSuper = true)
@Data

public class StorageRewards extends Storage {
  private String type = "reward";
  private Reward reward;

  public StorageRewards(ItemChance itemChance) {
    super();
    this.reward = itemChance.toReward();
  }

  public StorageRewards(UUID id, ItemChance itemChance) {
    super(id);
    this.reward = itemChance.toReward();
  }

  public StorageRewards(Reward reward) {
    super();
    this.reward = reward;
  }

  public StorageRewards(UUID id, Reward reward) {
    super(id);
    this.reward = reward;
  }

  @Override
  public ItemStack getDisplay() {
    return reward.getIcon();
  }

  @Override
  public CompletableFuture<Boolean> giveToPlayer(ServerPlayerEntity player) {
    return reward.giveToPlayer(player);
  }

}
