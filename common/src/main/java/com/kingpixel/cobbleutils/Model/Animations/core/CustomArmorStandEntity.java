package com.kingpixel.cobbleutils.Model.Animations.core;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@EqualsAndHashCode(callSuper = true) @Data
public class CustomArmorStandEntity extends ArmorStandEntity {

  // Campo necesario para que los controladores manejen el tiempo físico (ticks)
  private int ticks = 0;

  // Constructor requerido para el registro interno de entidades en Minecraft
  public CustomArmorStandEntity(EntityType<? extends ArmorStandEntity> entityType, World world) {
    super(entityType, world);
  }

  // Constructor dinámico que usamos para spawnear la entidad en coordenadas
  // específicas
  public CustomArmorStandEntity(World world, double x, double y, double z) {
    super(world, x, y, z);
  }

  public void setSmall(boolean small) {
    // Obtenemos los bytes de estado actuales del DataTracker
    byte currentFlags = this.dataTracker.get(ArmorStandEntity.ARMOR_STAND_FLAGS);

    if (small) {
      // Activamos el bit 0x01 (que corresponde a la flag de tamaño pequeño)
      this.dataTracker.set(ArmorStandEntity.ARMOR_STAND_FLAGS, (byte) (currentFlags | 0x01));
    } else {
      // Desactivamos el bit 0x01 si pasamos false
      this.dataTracker.set(ArmorStandEntity.ARMOR_STAND_FLAGS, (byte) (currentFlags & ~0x01));
    }
  }

  @Override
  public ActionResult interactAt(PlayerEntity player, Vec3d hitPos, Hand hand) {
    // Evita que los jugadores abran o interactúen con el Armor Stand de la
    // animación
    return ActionResult.FAIL;
  }

  @Override
  public boolean canEquip(ItemStack stack) {
    // Bloquea modificaciones externas al equipamiento
    return false;
  }

  @Override
  public boolean shouldSave() {
    // Evita que la entidad se guarde en los archivos de la región del mundo si el
    // servidor se apaga
    return false;
  }

  public void complete() {
    // Overridden by subclasses to clean up displays
  }

  @Override
  public void remove(RemovalReason reason) {
    super.remove(reason);
    complete();
  }
}