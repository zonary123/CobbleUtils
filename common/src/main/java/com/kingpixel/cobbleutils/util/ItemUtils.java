package com.kingpixel.cobbleutils.util;

import net.minecraft.world.item.ItemStack;

/**
 * @author Carlos Varas Alonso - 04/07/2024 4:05
 */
public class ItemUtils {
  public static String getNameItem(String item) {
    return Utils.parseItemId(item).getHoverName().getString();
  }

  public static String getNameItem(ItemStack itemStack) {
    return itemStack.getHoverName().getString();
  }
}
