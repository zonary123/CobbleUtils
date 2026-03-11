package com.kingpixel.cobbleutils.model.Animations;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * @author Carlos Varas Alonso - 02/05/2025 9:22
 */
@Setter
@Getter
public class CustomArmorStandEntity extends ArmorStandEntity {
  private int ticks = 0;

  public CustomArmorStandEntity(EntityType<? extends ArmorStandEntity> entityType, World world) {
    super(entityType, world);
  }

  public CustomArmorStandEntity(World world, double x, double y, double z) {
    super(world, x, y, z);
  }

  @Override
  public ActionResult interactAt(PlayerEntity player, Vec3d hitPos, Hand hand) {
    return ActionResult.FAIL;
  }

  @Override
  public boolean canEquip(ItemStack stack) {
    return false;
  }

  @Override
  public boolean shouldSave() {
    return false;
  }

}
