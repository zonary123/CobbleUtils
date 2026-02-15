package com.kingpixel.cobbleutils.Model.validators;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.minecraft.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Carlos Varas Alonso - 20/01/2026 11:01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemStackValidator {
  private Set<String> itemIds = new HashSet<>(
    Set.of("*")
  );


  /**
   * Check if the given item ID is valid according to the validator's criteria.
   *
   * @param itemId The item ID to validate.
   * @return True if the item ID is valid, false otherwise.
   */
  public boolean isValid(@NonNull String itemId) {
    return ValidatorUtil.match(itemId, itemIds);
  }

  /**
   * Check if the given ItemStack is valid according to the validator's criteria.
   *
   * @param itemStack The ItemStack to validate.
   * @return True if the ItemStack is valid, false otherwise.
   */
  public boolean isValid(@NonNull ItemStack itemStack) {
    try {
      //var tags = itemStack.streamTags();

      return isValid(itemStack.getItem().toString());
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

}
