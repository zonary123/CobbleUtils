package com.kingpixel.cobbleutils.Model.Animations.entity;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.Animations.core.Animation;
import com.kingpixel.cobbleutils.Model.Animations.core.AnimationUtils;
import com.kingpixel.cobbleutils.Model.Animations.core.CustomArmorStandEntity;
import com.kingpixel.cobbleutils.Model.Animations.core.CustomItemDisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class AllRewardsCircleAnimation extends Animation {

  @Override
  public void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                    List<ItemStack> allRewards, Runnable onComplete) {
    startCircle(player, allRewards, position, onComplete);
  }

  public static void startCircle(ServerPlayerEntity player, List<ItemStack> showAllRewards, Vec3d position, Runnable onComplete) {
    int totalRewards = showAllRewards.size();
    if (totalRewards == 0) {
      if (onComplete != null) onComplete.run();
      return;
    }
    Vec3d centerPosition = AnimationUtils.getPosition(player, position);

    CobbleUtils.server.executeSync(() -> {
      CircleController controller = new CircleController(
        player.getServerWorld(), centerPosition.x, centerPosition.y, centerPosition.z,
        showAllRewards, player, onComplete
      );
      player.getServerWorld().spawnEntity(controller);
    });
  }

  public static class CircleController extends CustomArmorStandEntity {
    private final ServerPlayerEntity player;
    private final List<ItemStack> rewards;
    private final Runnable onDestroy;
    private boolean completed = false;
    private final double radius = 3.0;

    private final List<CustomItemDisplayEntity> displays = new ArrayList<>();
    private final List<Double> initialAngles = new ArrayList<>();

    public CircleController(World world, double x, double y, double z,
                            List<ItemStack> rewards, ServerPlayerEntity player, Runnable onDestroy) {
      super(world, x, y, z);
      this.player = player;
      this.rewards = rewards;
      this.onDestroy = onDestroy;

      setNoGravity(true);
      setInvisible(true);
      setInvulnerable(true);

      ServerWorld sw = (ServerWorld) world;
      int size = rewards.size();
      for (int i = 0; i < size; i++) {
        ItemStack reward = rewards.get(i);
        if (reward == null) continue;

        double angle = Math.toRadians((360.0 / size) * i);
        double offsetX = radius * Math.cos(angle);
        double offsetZ = radius * Math.sin(angle);
        Vec3d itemPos = new Vec3d(x + offsetX, y, z + offsetZ);

        float yaw = AnimationUtils.getYawToFacePlayer(player, itemPos);
        CustomItemDisplayEntity display = AnimationUtils.spawnItemDisplay(
          sw, itemPos, reward.copy(), new Vector3f(1.1f, 1.1f, 1.1f), yaw, 0
        );

        displays.add(display);
        initialAngles.add(angle);
      }
    }

    @Override public void complete() {
      if (!completed) {
        completed = true;
        for (CustomItemDisplayEntity display : displays) {
          if (display != null) display.discard();
        }
        if (onDestroy != null) onDestroy.run();
      }
    }

    @Override
    public void tick() {
      super.tick();

      if (this.player == null || this.player.isRemoved() || !this.isAlive()) {
        this.kill();
        complete();
        return;
      }

      int ticks = getTicks();
      Vec3d centerPosition = AnimationUtils.getPosition(player, null);

      if (ticks < 160) {
        for (int i = 0; i < displays.size(); i++) {
          CustomItemDisplayEntity display = displays.get(i);
          if (display == null) continue;

          double angle = initialAngles.get(i) + Math.toRadians((ticks * 4) % 360);
          double offsetX = radius * Math.cos(angle);
          double offsetZ = radius * Math.sin(angle);

          double targetX = centerPosition.x + offsetX;
          double targetY = centerPosition.y + Math.sin(ticks * 0.15 + i) * 0.15;
          double targetZ = centerPosition.z + offsetZ;
          Vec3d targetPos = new Vec3d(targetX, targetY, targetZ);

          float yaw = AnimationUtils.getYawToFacePlayer(player, targetPos);
          Quaternionf rotation = new Quaternionf().rotationY((float) Math.toRadians(-yaw));
          AnimationUtils.updateDisplayTransformation(
            display, targetPos, rotation, new Vector3f(1.1f, 1.1f, 1.1f), 2
          );
        }
      } else {
        this.kill();
        complete();
      }

      setTicks(ticks + 1);
    }
  }
}
