package com.kingpixel.cobbleutils.Model.Animations.entity;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.Animations.core.Animation;
import com.kingpixel.cobbleutils.Model.Animations.core.AnimationUtils;
import com.kingpixel.cobbleutils.Model.Animations.core.CustomArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class PyramidAnimation extends Animation {

  @Override
  public void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                    List<ItemStack> allRewards, Runnable onComplete) {
    int totalRewards = obtained.size();
    if (totalRewards == 0) {
      if (onComplete != null) onComplete.run();
      return;
    }

    Vec3d direction = player.getRotationVec(1.0f).normalize();
    Vec3d right = new Vec3d(-direction.z, 0, direction.x).normalize();
    Vec3d centerPosition = player.getPos().add(direction.x * 2.5, 0.0, direction.z * 2.5);

    CobbleUtils.server.executeSync(() -> {
      PyramidController controller = new PyramidController(
        player.getServerWorld(), centerPosition.x, centerPosition.y, centerPosition.z,
        obtained, player, right, onComplete
      );
      player.getServerWorld().spawnEntity(controller);
    });
  }

  public static class PyramidController extends CustomArmorStandEntity {
    private final ServerPlayerEntity player;
    private final List<ItemStack> rewards;
    private final Runnable onDestroy;
    private boolean completed = false;

    private final List<DisplayEntity.ItemDisplayEntity> displays = new ArrayList<>();
    private final List<Vec3d> itemPositions = new ArrayList<>();

    public PyramidController(World world, double x, double y, double z,
                             List<ItemStack> rewards, ServerPlayerEntity player, Vec3d right, Runnable onDestroy) {
      super(world, x, y, z);
      this.player = player;
      this.rewards = rewards;
      this.onDestroy = onDestroy;

      setNoGravity(true);
      setInvisible(true);
      setInvulnerable(true);

      ServerWorld sw = (ServerWorld) world;
      int size = rewards.size();
      float itemYaw = player.getYaw() + 180f;

      for (int i = 0; i < size; i++) {
        ItemStack reward = rewards.get(i);
        if (reward == null) continue;

        int row = 0;
        int temp = i;
        while (temp > row) {
          temp -= (row + 1);
          row++;
        }
        int col = temp;

        double hOffset = (col - (row / 2.0)) * 0.9;
        double yOffset = 1.6 - (row * 0.6);

        Vec3d itemPos = new Vec3d(x, y, z).add(right.x * hOffset, yOffset, right.z * hOffset);
        itemPositions.add(itemPos);

        DisplayEntity.ItemDisplayEntity display = AnimationUtils.spawnItemDisplay(
          sw, itemPos, reward.copy(), new Vector3f(1.0f, 1.0f, 1.0f), itemYaw, 0
        );
        displays.add(display);
      }
    }

    private void complete() {
      if (!completed) {
        completed = true;
        for (DisplayEntity.ItemDisplayEntity display : displays) {
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
      ServerWorld sw = (ServerWorld) this.getWorld();

      if (ticks < 100) {
        // Spin and float items gently
        float spinYaw = player.getYaw() + 180f + ticks * 5.0f;
        Quaternionf rotation = new Quaternionf().rotationY((float) Math.toRadians(-spinYaw));

        for (int i = 0; i < displays.size(); i++) {
          DisplayEntity.ItemDisplayEntity display = displays.get(i);
          if (display == null) continue;

          Vec3d basePos = itemPositions.get(i);
          double targetY = basePos.y + Math.sin(ticks * 0.2 + i) * 0.1;
          Vec3d targetPos = new Vec3d(basePos.x, targetY, basePos.z);

          AnimationUtils.updateDisplayTransformation(
            display, targetPos, rotation, new Vector3f(1.0f, 1.0f, 1.0f), 2
          );

          if (ticks % 3 == 0) {
            sw.spawnParticles(
              ParticleTypes.END_ROD,
              targetPos.x, targetPos.y + 0.5, targetPos.z,
              1, 0.1, 0.1, 0.1, 0.0
            );
          }
        }
      } else {
        this.kill();
        complete();
      }

      setTicks(ticks + 1);
    }
  }
}
