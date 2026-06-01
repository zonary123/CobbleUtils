package com.kingpixel.cobbleutils.Model.Animations.core;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class AnimationUtils {

  private AnimationUtils() {
    // Utility class helper
  }

  public static Vec3d getPosition(ServerPlayerEntity player, Vec3d position) {
    if (position == null) {
      Vec3d handOffset = new Vec3d(0, 0, 0);
      var vehicle = player.getVehicle();
      if (vehicle != null) {
        return vehicle.getPos().add(handOffset);
      } else {
        return player.getPos().add(handOffset);
      }
    }
    return position;
  }

  public static float getYawToFacePlayer(ServerPlayerEntity player, Vec3d entityPos) {
    Vec3d playerPos = player.getPos();
    double dx = playerPos.x - entityPos.x;
    double dz = playerPos.z - entityPos.z;
    return (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
  }


  /**
   * Spawns a non-glowing, gravity-free ItemDisplayEntity with FIXED transform mode.
   */
  public static DisplayEntity.ItemDisplayEntity spawnItemDisplay(
      ServerWorld world, Vec3d pos, ItemStack stack, Vector3f scale, float yaw, float pitch) {
    return spawnItemDisplay(world, pos, stack, scale, yaw, pitch, ModelTransformationMode.FIXED, false);
  }

  /**
   * Spawns an ItemDisplayEntity with full control over transform mode and glowing properties.
   */
  public static DisplayEntity.ItemDisplayEntity spawnItemDisplay(
      ServerWorld world, Vec3d pos, ItemStack stack, Vector3f scale, float yaw, float pitch,
      ModelTransformationMode context, boolean glowing) {
    
    DisplayEntity.ItemDisplayEntity itemDisplay = new DisplayEntity.ItemDisplayEntity(EntityType.ITEM_DISPLAY, world);
    itemDisplay.setItemStack(stack);
    itemDisplay.setTransformationMode(context);
    itemDisplay.setInvulnerable(true);
    itemDisplay.setNoGravity(true);
    itemDisplay.setGlowing(glowing);

    Quaternionf rotation = new Quaternionf().rotationYXZ((float) Math.toRadians(-yaw), (float) Math.toRadians(pitch), 0);
    itemDisplay.setTransformation(new AffineTransformation(null, rotation, scale, null));
    itemDisplay.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0f, 0f);
    
    world.spawnEntity(itemDisplay);
    return itemDisplay;
  }

  /**
   * Updates position, rotation, and scale of a display entity with client-side interpolation.
   */
  public static void updateDisplayTransformation(
      DisplayEntity entity, Vec3d pos, Quaternionf rotation, Vector3f scale, int interpolationTicks) {
    
    entity.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0f, 0f);
    entity.setStartInterpolation(0);
    entity.setInterpolationDuration(interpolationTicks);
    entity.setTransformation(new AffineTransformation(null, rotation, scale, null));
  }
}

