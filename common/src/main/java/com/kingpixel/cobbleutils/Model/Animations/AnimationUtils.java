package com.kingpixel.cobbleutils.Model.Animations;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

public class AnimationUtils {


    public static Vec3d getPosition(ServerPlayerEntity player, Vec3d position) {
        if (position == null) {

            Vec3d handOffset = new Vec3d(0, 0, 0);
            return player.getPos().add(handOffset);
        }
        return position;
    }
    public static float getYawToFacePlayer(ServerPlayerEntity player, Vec3d entityPos) {
        Vec3d playerPos = player.getPos();

        double dx = playerPos.x - entityPos.x;
        double dz = playerPos.z - entityPos.z;

        return (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
    }
}