package com.kingpixel.cobbleutils.Model;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.JsonObject;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.network.ProxyPacket;
import com.kingpixel.cobbleutils.util.MinecraftUtils;
import com.kingpixel.cobbleutils.util.redis.handlers.RedisTeleportHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 13/10/2025 22:03
 */
@Data
@AllArgsConstructor
public class Location {
  private static final Cache<String, ServerWorld> WORLDS = Caffeine.newBuilder().build();
  private ItemModel displayItem = new ItemModel("minecraft:compass", "&eLocation");
  private String server;
  private String world;
  private double x;
  private double y;
  private double z;
  private float yaw;
  private float pitch;

  public Location() {
    this.displayItem = new ItemModel("minecraft:compass", "&eLocation");
    this.server = "default";
    this.world = "world";
    this.x = 0;
    this.y = 64;
    this.z = 0;
    this.yaw = 0;
    this.pitch = 0;
  }

  public Location(ServerPlayerEntity player) {
    this.displayItem = new ItemModel("minecraft:compass", "&eLocation");
    this.server = CobbleUtils.getServerName();
    this.world = player.getWorld().getRegistryKey().getValue().toString();
    this.x = player.getX();
    this.y = player.getY();
    this.z = player.getZ();
    this.yaw = player.getYaw();
    this.pitch = player.getPitch();
  }

  public void teleportTo(ServerPlayerEntity player) {
    try {
      boolean crossServer = CobbleUtils.config.isRedisMessaging()
        && !Objects.equals(CobbleUtils.getServerName(), server);

      if (crossServer) {
        teleportToCrossServer(player);
      } else {
        teleportToNoCrossServer(player);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public CompletableFuture<Boolean> teleportToNoCrossServer(ServerPlayerEntity player) {
    return CobbleUtils.server.submit(() -> {
      ServerWorld targetWorld = MinecraftUtils.getServerWorld(world);
      if (targetWorld == null) return false;
      return player.teleport(targetWorld, x, y, z, PositionFlag.VALUES, yaw, pitch);
    });
  }

  private void teleportToCrossServer(ServerPlayerEntity player) {
    ProxyPacket.sendServer(player, server);
    JsonObject json = new JsonObject();

    json.addProperty("type", "teleport");
    json.addProperty("uuid", player.getUuid().toString());

    JsonObject loc = new JsonObject();
    loc.addProperty("server", server);
    loc.addProperty("world", world);
    loc.addProperty("x", x);
    loc.addProperty("y", y);
    loc.addProperty("z", z);
    loc.addProperty("yaw", yaw);
    loc.addProperty("pitch", pitch);

    json.add("location", loc);
    json.addProperty("server", server);
    json.addProperty("reason", "cross-server-teleport");

    try {
      if (CobbleUtils.redisManager != null) {
        CobbleUtils.redisManager.saveState("teleport:" + player.getUuid().toString(), json, 15);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    CobbleUtils.redisManager.publish(RedisTeleportHandler.CHANNEL, json);
  }


}
