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
 * Validator for Minecraft items.
 * <p>
 * Checks whether an ItemStack is valid based on a list of item IDs, blacklist, and optional tags.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemStackValidator extends AbstractRegistryValidator<ItemStack> {
  /**
   * List of allowed item IDs. Supports wildcards and regex.
   */
  private Set<String> itemIds = new HashSet<>(Set.of(
    "*",
    "regex:.*",
    "minecraft:stone"
  ));

  /**
   * Optional tags for validation.
   */
  private Set<String> itemTags = new HashSet<>();

  private transient Set<TagKey<Item>> tagKeys;

  /**
   * Lazy initialization of TagKey<Item> set from itemTags.
   */
  private Set<TagKey<Item>> getTagKeysLazy() {
    if (tagKeys == null) {
      tagKeys = itemTags.stream()
        .map(t -> TagKey.of(Registries.ITEM.getKey(), Identifier.tryParse(t)))
        .collect(Collectors.toSet());
    }
    return tagKeys;
  }

  @Override
  protected Set<String> getIdSet() {
    return itemIds;
  }

  @Override
  protected Set<String> getTagSet() {
    return itemTags;
  }

  @Override
  protected String getId(@NonNull ItemStack itemStack) {
    Item item = itemStack.getItem();
    return Registries.ITEM.getId(item).toString();
  }

  @Override
  protected boolean isInTag(@NonNull ItemStack itemStack) {
    Item item = itemStack.getItem();
    var keys = getTagKeysLazy();
    if (keys.isEmpty()) return false;

    var entry = Registries.ITEM.getEntry(item);
    for (TagKey<Item> key : keys) {
      if (entry.isIn(key)) return true;
    }
    return false;
  }
}