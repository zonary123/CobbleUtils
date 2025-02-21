package com.kingpixel.cobbleutils.Model.Animations;

import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class AllRewardsCircleAnimation {

    private static final int LIFETIME_TICKS = 100;
    private static final double ROTATION_SPEED = 2.0;
    private static final double RADIUS = 3.0;

    public static void start(ServerPlayerEntity player, List<ItemStack> rewardItems) {
        if (rewardItems.isEmpty()) {
            return;
        }

        World world = player.getServerWorld();
        Vec3d initialPos = player.getPos();

        for (int i = 0; i < rewardItems.size(); i++) {
            ItemStack reward = rewardItems.get(i);
            double angle = Math.toRadians((360.0 / rewardItems.size()) * i);
            double offsetX = RADIUS * Math.cos(angle);
            double offsetZ = RADIUS * Math.sin(angle);
            var entity = new SmoothSpinEntity(world, initialPos, initialPos.x + offsetX, initialPos.y + 1, initialPos.z + offsetZ, reward, angle);
            world.spawnEntity(entity);
        }
    }

    public static class SmoothSpinEntity extends ItemEntity {
        private final Vec3d origin;
        private double angle;
        private int ticksExisted = 0;

        public SmoothSpinEntity(World world, Vec3d origin, double x, double y, double z, ItemStack stack, double initialAngle) {
            super(world, x, y, z, stack);
            this.origin = origin;
            this.angle = initialAngle;
            setNoGravity(true);
            setPickupDelay(Integer.MAX_VALUE);
            setInvulnerable(true);
            setVelocity(Vec3d.ZERO);
        }

        @Override
        public void tick() {
            try {
                super.tick();

                if (ticksExisted >= LIFETIME_TICKS) {
                    this.kill();
                    return;
                }

                angle += Math.toRadians(ROTATION_SPEED);
                if (angle >= 2 * Math.PI) {
                    angle -= 2 * Math.PI;
                }

                double offsetX = RADIUS * Math.cos(angle);
                double offsetZ = RADIUS * Math.sin(angle);
                setPos(origin.x + offsetX, origin.y + 1, origin.z + offsetZ);

                ticksExisted++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public boolean shouldSave() {
            return false;
        }
    }
}
