package com.kingpixel.cobbleutils.Model.Animations.core;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.world.World;

/**
 *
 * @author Carlos Varas Alonso - 07/06/2026 12:23
 */
@EqualsAndHashCode(callSuper = true) @Data
public class CustomItemDisplayEntity extends DisplayEntity.ItemDisplayEntity {
  public CustomItemDisplayEntity(EntityType<?> entityType, World world) {
    super(entityType, world);
  }

  @Override public boolean shouldSave() {
    return false;
  }
}
