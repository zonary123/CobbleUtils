package com.kingpixel.cobbleutils.database.users.models;

import com.kingpixel.cobbleutils.CobbleUtils;
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

public class StorageItemStack extends Storage {
  private final String type = "itemStack";
  private ItemStack itemStack;

  public StorageItemStack(ItemStack itemStack) {
    super();
    this.itemStack = itemStack;
  }

  public StorageItemStack(UUID id, ItemStack itemStack) {
    super(id);
    this.itemStack = itemStack;
  }

  @Override
  public ItemStack getDisplay() {
    return itemStack;
  }

  @Override
  public CompletableFuture<Boolean> giveToPlayer(ServerPlayerEntity player) {
    return CobbleUtils.server.submit(() -> {
      if (player.getInventory().getEmptySlot() != -1) {
        player.getInventory().offerOrDrop(itemStack.copy());
        return true;
      }
      return false;
    });
  }

}
