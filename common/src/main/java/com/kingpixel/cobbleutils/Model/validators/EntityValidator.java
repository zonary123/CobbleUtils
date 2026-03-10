package com.kingpixel.cobbleutils.Model.validators;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.minecraft.entity.Entity;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Carlos Varas Alonso - 20/01/2026 11:01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityValidator {
  private Set<String> blacklistEntityIds = new HashSet<>();
  private Set<String> entityIds = new HashSet<>(
    Set.of(
      "*",
      "regex:.*",
      "minecraft:pig"
    )
  );

  public boolean isValid(@NonNull String entityId) {
    if (ValidatorUtil.match(entityId, blacklistEntityIds)) return false;
    return ValidatorUtil.match(entityId, entityIds);
  }

  public boolean isValid(@NonNull Entity entity) {
    String id = entity.getSavedEntityId();
    return id != null && isValid(id);
  }


}
