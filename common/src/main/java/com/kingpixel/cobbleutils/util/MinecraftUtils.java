package com.kingpixel.cobbleutils.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.database.DataBaseFactory;
import net.minecraft.entity.Entity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 23/11/2025 21:04
 */
public class MinecraftUtils {
  private static final Cache<String, ServerWorld> WORLD_CACHE = Caffeine.newBuilder()
    .build();

  public static @Nullable ServerWorld getServerWorld(String worldName) {
    ServerWorld cachedWorld = WORLD_CACHE.getIfPresent(worldName);
    if (cachedWorld != null) return cachedWorld;
    var worlds = CobbleUtils.server.getWorlds();
    for (ServerWorld serverWorld : worlds) {
      if (serverWorld.getRegistryKey().getValue().toString().equals(worldName)) {
        return WORLD_CACHE.get(worldName, world -> serverWorld);
      }
    }
    return null;
  }

  public static String getWorldTranslate(World world) {
    var id = world.getRegistryKey().getValue().toString();
    return CobbleUtils.language.getWorlds().getOrDefault(id, id);
  }

  public static String getBiomesTranslate(RegistryEntry<Biome> biome) {
    var biomeId = biome.getIdAsString();

    if (CobbleUtils.config.isDebug()) {
      CobbleUtils.LOGGER_RAW.info("Biome ID: " + biomeId + " | Translation Key: " + biome.getKey().get().getValue().toTranslationKey());
    }
    return CobbleUtils.language.getBiomes().getOrDefault(biomeId, "<lang:biome." + biome.getKey().get().getValue().toTranslationKey() + ">");
  }

  public static List<UUID> getOnlinePlayerUUIDs() {
    if (CobbleUtils.config.isRedisMessaging()) return DataBaseFactory.dataBaseUsers.getOnlinePlayers();
    return CobbleUtils.server.getPlayerManager().getPlayerList().stream()
      .map(Entity::getUuid)
      .toList();
  }


}
