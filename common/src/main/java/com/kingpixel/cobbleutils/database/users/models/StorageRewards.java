package com.kingpixel.cobbleutils.database.users.models;

import com.kingpixel.cobbleutils.Model.ItemChance;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 06/10/2025 5:11
 */
@EqualsAndHashCode(callSuper = true)
@Data

public class StorageRewards extends Storage {
  private String type = "reward";
  private ItemChance reward;

  public StorageRewards(ItemChance itemChance) {
    super();
    this.reward = itemChance;
  }

  public StorageRewards(UUID id, ItemChance itemChance) {
    super(id);
    this.reward = itemChance;
  }

  @Override
  public ItemStack getDisplay() {
    return reward.getIcon();
  }

  @Override
  public boolean giveToPlayer(ServerPlayerEntity player) {
    if (reward.getType().equals(ItemChance.ItemChanceType.ITEM) && player.getInventory().getEmptySlot() == -1) {
      return false;
    }
    reward.giveReward(player);
    return true;
  }

}
