package com.kingpixel.cobbleutils.Model.Animations.core;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

/**
 * Custom BlockDisplayEntity that is never saved to world or chunk files.
 *
 * @author Carlos Varas Alonso
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CustomBlockDisplayEntity extends DisplayEntity.BlockDisplayEntity {
  public CustomBlockDisplayEntity(EntityType<?> entityType, World world) {
    super(entityType, world);
  }

  @Override
  public boolean shouldSave() {
    return false;
  }

  @Override
  public boolean saveNbt(NbtCompound nbt) {
    return false;
  }

  @Override
  public boolean saveSelfNbt(NbtCompound nbt) {
    return false;
  }
}
