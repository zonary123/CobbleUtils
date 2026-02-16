package com.kingpixel.cobbleutils.Model.validators;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.minecraft.block.Block;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Carlos Varas Alonso - 20/01/2026 11:01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlockValidator {
  private Set<String> blockIds = new HashSet<>(
    Set.of(
      "*",
      "regex:.*",
      "minecraft:stone"
    )
  );

  /**
   * Check if the given block ID is valid according to the validator's criteria.
   *
   * @param blockId The block ID to validate.
   * @return True if the block ID is valid, false otherwise.
   */
  public boolean isValid(@NonNull String blockId) {
    return ValidatorUtil.match(blockId, blockIds);
  }


  /**
   * Check if the given BlockType is valid according to the validator's criteria.
   *
   * @param block The BlockType to validate.
   * @return True if the BlockType is valid, false otherwise.
   */
  public boolean isValid(@NonNull Block block) {
    String blockId = block.getRegistryEntry().getIdAsString();
    return isValid(blockId);
  }
}
