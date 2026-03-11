package com.kingpixel.cobbleutils.model.conditions;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
public class NearBlockCondition extends Condition {

  public static final String TYPE = "NEAR_BLOCK";

  private Set<String> blockIds = new HashSet<>();
  private int radius = 5;

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    if (blockIds == null || blockIds.isEmpty()) return false;

    BlockPos center = player.getBlockPos();

    for (BlockPos pos : BlockPos.iterateOutwards(center, radius, radius, radius)) {
      var block = player.getWorld().getBlockState(pos).getBlock();
      var id = Registries.BLOCK.getId(block).toString();

      if (blockIds.contains(id)) return true;
    }

    return false;
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "You must be near one of the following blocks: " + String.join(", ", blockIds);
  }
}