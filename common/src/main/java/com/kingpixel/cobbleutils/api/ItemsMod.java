package com.kingpixel.cobbleutils.api;

import com.kingpixel.cobbleutils.Model.ItemChance;
import net.minecraft.item.ItemStack;

/**
 * @author Carlos Varas Alonso - 12/11/2024 2:32
 */
public class ItemsMod {
  /**
   * Add a new item to the loot table
   *
   * @param modId  The mod id
   * @param itemId The item id
   * @param item   The item to add
   */
  public static void addItem(String modId, String itemId, ItemStack item) {
    ItemChance.addModItem(modId, itemId, item);
  }
}
