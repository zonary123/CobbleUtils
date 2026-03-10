package com.kingpixel.cobbleutils.Model.validators;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
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
public class BlockValidator {
  private Set<String> blacklistBlockIds = new HashSet<>();
  private Set<String> blockIds = new HashSet<>(Set.of(
    "*",
    "regex:.*",
    "minecraft:stone"
  ));
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
   * Check if the given block ID is valid according to the validator's criteria.
   *
   * @param blockId The block ID to validate.
   * @return True if the block ID is valid, false otherwise.
   */
  public boolean isValid(@NonNull String blockId) {
    if (ValidatorUtil.match(blockId, blacklistBlockIds)) return false;
    return ValidatorUtil.match(blockId, blockIds);
  }


  /**
   * Check if the given BlockType is valid according to the validator's criteria.
   *
   * @param block The BlockType to validate.
   * @return True if the BlockType is valid, false otherwise.
   */
  public boolean isValid(Block block) {
    Item item = block.asItem();
    if (blockIds.contains("*") || blockIds.contains(item.toString())) return true;

    Set<TagKey<Item>> keys = getTagKeysLazy();
    if (keys.isEmpty()) return false;

    var entry = Registries.ITEM.getEntry(item);
    for (TagKey<Item> tagKey : keys) {
      if (entry.isIn(tagKey)) return true;
    }
    return false;
  }
}
