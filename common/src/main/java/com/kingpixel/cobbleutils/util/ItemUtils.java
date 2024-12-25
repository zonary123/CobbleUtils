package com.kingpixel.cobbleutils.util;

import com.kingpixel.cobbleutils.CobbleUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * @author Carlos Varas Alonso - 04/07/2024 4:05
 */
public class ItemUtils {
  public static String getNameItem(String item) {
    ItemStack itemStack = Utils.parseItemId(item);
    return getTranslatedName(itemStack);
  }

  public static String getNameItem(ItemStack itemStack) {
    return getTranslatedName(itemStack);
  }

  public static String getTranslatedName(ItemStack itemStack) {
    if (itemStack.getItem() == Items.AIR) return CobbleUtils.language.getUnknown();
    if (itemStack.get(DataComponentTypes.CUSTOM_NAME) != null) return itemStack.getName().getString();
    return "<lang:" + itemStack.getItem().getTranslationKey() + ">";
  }
}
