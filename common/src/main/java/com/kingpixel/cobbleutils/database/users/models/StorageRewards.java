package com.kingpixel.cobbleutils.database.users.models;

import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.Model.rewards.Reward;
import lombok.*;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 06/10/2025 5:11
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StorageRewards extends Storage {
  @Builder.Default
  private final String type = "reward";
  private Reward reward;

  public StorageRewards(ItemChance itemChance) {
    super();
    this.type = "reward";
    this.reward = itemChance.toReward();
  }

  public StorageRewards(UUID id, ItemChance itemChance) {
    super(id);
    this.type = "reward";
    this.reward = itemChance.toReward();
  }

  public StorageRewards(Reward reward) {
    super();
    this.type = "reward";
    this.reward = reward;
  }

  public StorageRewards(UUID id, Reward reward) {
    super(id);
    this.type = "reward";
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
