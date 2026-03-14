package com.kingpixel.cobbleutils.Model.validators;

import lombok.*;
import net.minecraft.entity.Entity;

import java.util.HashSet;
import java.util.Set;

/**
 * Validator for Minecraft entities.
 * <p>
 * Checks whether an entity is valid based on a list of entity IDs and a blacklist.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true)
public class EntityValidator extends AbstractRegistryValidator<Entity> {

  /**
   * List of allowed entity IDs. Supports wildcards and regex.
   */
  @Builder.Default
  private Set<String> entityIds = new HashSet<>(Set.of(
    "*",
    "regex:.*",
    "minecraft:pig"
  ));

  @Override
  protected Set<String> getIdSet() {
    return entityIds;
  }

  @Override
  protected Set<String> getTagSet() {
    return Set.of();
  }

  @Override
  protected String getId(@NonNull Entity entity) {
    return entity.getSavedEntityId();
  }

  @Override
  protected boolean isInTag(@NonNull Entity entity) {
    return false;
  }
}