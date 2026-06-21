package com.kingpixel.cobbleutils.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.database.DataBaseFactory;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stat;
import net.minecraft.stat.StatHandler;
import net.minecraft.stat.StatType;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

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

  public static final Map<StatType<?>, Function<Identifier, Object>> RESOLVERS = new HashMap<>();

  public static Map<StatType<?>, Function<Identifier, Object>> getResolvers() {
    if (RESOLVERS.isEmpty()) {
      RESOLVERS.putAll(Map.of(
        Stats.MINED, Registries.BLOCK::get,
        Stats.CRAFTED, Registries.ITEM::get,
        Stats.USED, Registries.ITEM::get,
        Stats.BROKEN, Registries.ITEM::get,
        Stats.PICKED_UP, Registries.ITEM::get,
        Stats.DROPPED, Registries.ITEM::get,
        Stats.KILLED, Registries.ENTITY_TYPE::get,
        Stats.KILLED_BY, Registries.ENTITY_TYPE::get,
        Stats.CUSTOM, id -> id // CUSTOM stats solo usan el Identifier
      ));
    }
    return RESOLVERS;
  }


  /**
   * Devuelve el valor crudo del stat
   */
  public static int getStatValue(ServerPlayerEntity player, String stat_type, String stat) {
    StatHandler handler = player.getStatHandler();

    Identifier typeId = Identifier.of(stat_type);
    Identifier statId = Identifier.of(stat);

    StatType<?> statType = Registries.STAT_TYPE.get(typeId);
    if (statType == null) {
      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER_RAW.info("Stat type not found: " + stat_type);
      }
      return 0;
    }

    Function<Identifier, Object> resolver = getResolvers().get(statType);
    if (resolver == null) {
      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER_RAW.info("No resolver found for stat type: " + stat_type);
      }
      return 0;
    }

    Object value = resolver.apply(statId);
    if (value == null) {
      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER_RAW.info("Stat resolver returned null for stat: " + stat + " of type: " + stat_type);
      }
      return 0;
    }

    try {
      @SuppressWarnings("unchecked")
      StatType<Object> castedType = (StatType<Object>) statType;
      Stat<Object> statObj = castedType.getOrCreateStat(value);
      int rawValue = handler.getStat(statObj);

      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER_RAW.info("Stat " + stat + " of type " + stat_type +
          " = raw(" + rawValue + ") formatted(" + statObj.format(rawValue) + ")");
      }

      return rawValue;

    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.warn("Failed to create Stat object for stat: {} of type: {}", stat, stat_type, e);
      return 0;
    }
  }

}
