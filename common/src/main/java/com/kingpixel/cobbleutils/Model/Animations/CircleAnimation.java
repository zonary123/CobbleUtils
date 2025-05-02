package com.kingpixel.cobbleutils.Model.Animations;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class CircleAnimation {

  public static void start(ServerPlayerEntity player, List<ItemStack> showRewards, Vec3d position) {
    int totalRewards = showRewards.size();
    Vec3d centerPosition = AnimationUtils.getPosition(player, position);
    double radius = 3;

    for (int i = 0; i < totalRewards; i++) {
      double angle = Math.toRadians((360.0 / totalRewards) * i);
      double offsetX = radius * Math.cos(angle);
      double offsetZ = radius * Math.sin(angle);
      double offsetY = centerPosition.y;

      ItemStack reward = showRewards.get(i);
      if (reward == null) continue;
      Vec3d entityPosition = new Vec3d(centerPosition.x + offsetX, offsetY, centerPosition.z + offsetZ);
      float yaw = AnimationUtils.getYawToFacePlayer(player, entityPosition);

      CircleEntity circleEntity = new CircleEntity(player.getServerWorld(), entityPosition.x, entityPosition.y, entityPosition.z, reward, player, angle);
      circleEntity.setYaw(yaw);

      player.getServerWorld().spawnEntity(circleEntity);
    }
  }

  public static class CircleEntity extends CustomArmorStandEntity {
    private final ServerPlayerEntity player;
    private final double radius;
    private final double initialAngle;

    public CircleEntity(World world, double x, double y, double z, ItemStack stack, ServerPlayerEntity player, double initialAngle) {
      super(world, x, y, z);
      this.player = player;
      this.radius = 3;
      this.initialAngle = initialAngle;

      equipStack(EquipmentSlot.MAINHAND, stack);
      setNoGravity(true);
      setInvisible(true);
      setInvulnerable(true);
      setRightArmRotation(new EulerAngle(90, 0, 180));
    }

    @Override
    public void tick() {
      super.tick();

      if (this.player == null || this.player.isRemoved() || !this.isAlive()) {
        this.kill();
        return;
      }

      Vec3d centerPosition = AnimationUtils.getPosition(player, null);
      double angle = this.initialAngle + Math.toRadians((getTicks() * 4) % 360);
      double offsetX = this.radius * Math.cos(angle);
      double offsetZ = this.radius * Math.sin(angle);

      double targetX = centerPosition.x + offsetX;
      double targetY = centerPosition.y;
      double targetZ = centerPosition.z + offsetZ;

      this.refreshPositionAndAngles(targetX, targetY, targetZ, AnimationUtils.getYawToFacePlayer(player, this.getPos()), this.getPitch());

      if (getTicks() >= 160) {
        this.kill();
      }

      setTicks(getTicks() + 1);
    }

  }
}