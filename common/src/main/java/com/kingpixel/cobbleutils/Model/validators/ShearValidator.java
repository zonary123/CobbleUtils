package com.kingpixel.cobbleutils.Model.validators;

import com.kingpixel.cobbleutils.events.models.EventShearEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.item.ItemStack;

/**
 * Composite validator for shearing interactions (entity, tool, drops).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShearValidator {

  @Builder.Default
  private EntityValidator entityValidator = new EntityValidator();

  @Builder.Default
  private ItemStackValidator toolValidator = new ItemStackValidator();

  @Builder.Default
  private ItemStackValidator dropValidator = new ItemStackValidator();

  /**
   * Validates if a shearing event satisfies entity, tool, and drop filters.
   *
   * @param event the shearing event to evaluate
   * @return true if all non-empty criteria match
   */
  public boolean isValid(EventShearEntity event) {
    if (event == null) return false;

    if (entityValidator != null && event.getEntity() != null) {
      if (!entityValidator.isValid(event.getEntity())) {
        return false;
      }
    }

    if (toolValidator != null && event.getTool() != null && !event.getTool().isEmpty()) {
      if (!toolValidator.isValid(event.getTool())) {
        return false;
      }
    }

    if (dropValidator != null && event.getDrops() != null && !event.getDrops().isEmpty()) {
      boolean matched = false;
      for (ItemStack drop : event.getDrops()) {
        if (dropValidator.isValid(drop)) {
          matched = true;
          break;
        }
      }
      if (!matched) {
        return false;
      }
    }

    return true;
  }
}
