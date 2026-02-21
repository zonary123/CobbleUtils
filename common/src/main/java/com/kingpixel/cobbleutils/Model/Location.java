package com.kingpixel.cobbleutils.Model;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.JsonObject;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.RedisManager;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.Objects;

/**
 * @author Carlos Varas Alonso - 13/10/2025 22:03
 */
@Data
@AllArgsConstructor
public class Location {
  private static final Cache<String, ServerWorld> WORLDS = Caffeine.newBuilder().build();
  private ItemModel displayItem;
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
    if (CobbleUtils.config.isRedisMessaging()) {
      teleportToCrossServer(player);
    } else {
      teleportToNoCrossServer(player);
    }
  }

  public boolean teleportToNoCrossServer(ServerPlayerEntity player) {
    ServerWorld targetWorld = WORLDS.get(world, k -> {
      var worldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.tryParse(world));
      ServerWorld serverWorld = CobbleUtils.server.getWorld(worldKey);
      return Objects.requireNonNullElse(serverWorld, CobbleUtils.server.getOverworld());
    });
    if (targetWorld == null) return false;
    return player.teleport(targetWorld, x, y, z, PositionFlag.ROT, yaw, pitch);
  }

  public void teleportToCrossServer(ServerPlayerEntity player) {

    JsonObject json = new JsonObject();

    json.addProperty("type", "teleport");
    json.addProperty("uuid", player.getUuid().toString());

    JsonObject loc = new JsonObject();
    loc.addProperty("world", this.getWorld());
    loc.addProperty("x", this.getX());
    loc.addProperty("y", this.getY());
    loc.addProperty("z", this.getZ());
    loc.addProperty("yaw", this.getYaw());
    loc.addProperty("pitch", this.getPitch());

    json.add("location", loc);

    json.addProperty("server", server);
    json.addProperty("reason", "cross-server-teleport");

    RedisManager.publish("cobbleutils:teleport", json.toString());
  }


}
