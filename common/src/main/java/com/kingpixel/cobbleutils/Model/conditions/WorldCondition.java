package com.kingpixel.cobbleutils.Model.conditions;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
public class WorldCondition extends Condition {
  public static final String TYPE = "WORLD";
  private final Set<String> worlds = Set.of("minecraft:overworld");

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    String world = player.getWorld().getRegistryKey().getValue().toString();
    return worlds.contains(world);
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "You need to be in one of the following worlds: " + String.join(", ", worlds);
  }


}
