package com.kingpixel.cobbleutils.util;

import com.kingpixel.cobbleutils.CobbleUtils;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

/**
 * @author Carlos Varas Alonso - 23/11/2025 21:04
 */
public class MinecraftUtils {
  public static String getWorldTranslate(World world) {
    var id = world.getRegistryKey().getValue().toString();
    return CobbleUtils.language.getWorlds().getOrDefault(id, id);
  }

  public static String getBiomesTranslate(RegistryEntry<Biome> biome) {
    var biomeId = biome.getIdAsString();
    if (CobbleUtils.config.isDebug()) {
      CobbleUtils.LOGGER.info("Biome ID: " + biomeId + " | Translation Key: " + biome.getKey().get().getValue().toTranslationKey());
    }
    return CobbleUtils.language.getBiomes().getOrDefault(biomeId, "<lang:biome." + biome.getKey().get().getValue().toTranslationKey() + ">");
  }
}
