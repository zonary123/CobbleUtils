package com.kingpixel.cobbleutils.Model.conditions;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
public class BiomeCondition extends Condition {
  public static final String TYPE = "BIOME";
  private final Set<String> biomes = Set.of("minecraft:plains");

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    String biome = player.getWorld().getBiome(player.getBlockPos()).getIdAsString();
    return biomes.contains(biome);
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "You need to be in one of the following biomes: " + String.join(", ", biomes);
  }


}
