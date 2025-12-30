package com.kingpixel.cobbleutils.Model;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.network.ProxyPacket;
import lombok.Data;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

/**
 * @author Carlos Varas Alonso - 13/10/2025 22:03
 */
@Data
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
    this.server = CobbleUtils.config.getServer();
    this.world = player.getWorld().getRegistryKey().getValue().toString();
    this.x = player.getX();
    this.y = player.getY();
    this.z = player.getZ();
    this.yaw = player.getYaw();
    this.pitch = player.getPitch();
  }

  public void teleportTo(ServerPlayerEntity player) {
    CobbleUtils.server.execute(() -> {
      if (CobbleUtils.config.isRedisMessaging()) {
        teleportToCrossServer(player);
      } else {
        teleportToNoCrossServer(player);
      }
    });
  }

  private void teleportToNoCrossServer(ServerPlayerEntity player) {
    ServerWorld targetWorld = WORLDS.get(world, k -> {
      var worldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.tryParse(world));
      return CobbleUtils.server.getWorld(worldKey);
    });
    if (targetWorld == null) return;
    player.teleport(targetWorld, x, y, z, yaw, pitch);
  }

  private void teleportToCrossServer(ServerPlayerEntity player) {
    ServerPlayNetworking.send(player, new ProxyPacket("Connect", server));
  }


}
