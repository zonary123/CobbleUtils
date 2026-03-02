package com.kingpixel.cobbleutils.Model.validators;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author Carlos Varas Alonso - 20/01/2026 11:01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemStackValidator {
  private Set<String> blacklist = new HashSet<>();
  private Set<String> itemIds = new HashSet<>(
    Set.of(
      "*",
      "regex:.*",
      "minecraft:stone"
    )
  );
  private Set<String> tags = new HashSet<>();
  private transient Set<TagKey<Item>> tagKeys;

  private Set<TagKey<Item>> getTagKeysLazy() {
    if (tagKeys == null) {
      tagKeys = tags.stream()
        .map(t -> TagKey.of(Registries.ITEM.getKey(), Identifier.tryParse(t)))
        .collect(Collectors.toSet());
    }
    return tagKeys;
  }

  /**
   * Check if the given item ID is valid according to the validator's criteria.
   *
   * @param itemId The item ID to validate.
   * @return True if the item ID is valid, false otherwise.
   */
  public boolean isValid(@NonNull String itemId) {
    if (ValidatorUtil.match(itemId, blacklist)) return false;
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
      Item item = itemStack.getItem();
      var keys = getTagKeysLazy();
      var entry = Registries.ITEM.getEntry(item);
      for (TagKey<Item> key : keys) {
        if (entry.isIn(key)) return true;
      }
      return isValid(item.toString());
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

}
