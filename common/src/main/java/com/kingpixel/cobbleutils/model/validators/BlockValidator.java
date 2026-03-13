package com.kingpixel.cobbleutils.model.validators;

import lombok.*;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validator for Minecraft blocks.
 * <p>
 * Checks whether a block is valid based on a list of block IDs, blacklist, and optional tags.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class BlockValidator extends AbstractRegistryValidator<Block> {
  /**
   * List of allowed block IDs. Supports wildcards and regex.
   */
  private Set<String> blockIds = new HashSet<>(Set.of(
    "*",
    "regex:.*",
    "minecraft:stone"
  ));

  /**
   * Optional block tags for validation.
   */
  private Set<String> blockTags = new HashSet<>();

  private transient Set<TagKey<Item>> tagKeys;

  /**
   * Lazy initialization of TagKey<Item> set from blockTags.
   */
  private Set<TagKey<Item>> getTagKeysLazy() {
    if (tagKeys == null) {
      tagKeys = blockTags.stream()
        .map(t -> TagKey.of(Registries.ITEM.getKey(), Identifier.tryParse(t)))
        .collect(Collectors.toSet());
    }
    return tagKeys;
  }

  @Override
  protected Set<String> getIdSet() {
    return blockIds;
  }

  @Override
  protected Set<String> getTagSet() {
    return blockTags;
  }

  @Override
  protected String getId(@NonNull Block block) {
    return Registries.BLOCK.getId(block).toString();
  }

  @Override
  protected boolean isInTag(@NonNull Block block) {
    Item item = block.asItem();
    var keys = getTagKeysLazy();
    if (keys.isEmpty()) return false;

    var entry = Registries.ITEM.getEntry(item);
    for (TagKey<Item> tagKey : keys) {
      if (entry.isIn(tagKey)) return true;
    }
    return false;
  }

  @Override
  protected String getReason(Block value) {
    String id = getId(value);
    return "Block " + id + " does not match any of the allowed IDs or tags";
  }
}