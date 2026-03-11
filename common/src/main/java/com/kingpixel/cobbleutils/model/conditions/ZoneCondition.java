package com.kingpixel.cobbleutils.model.conditions;

import com.kingpixel.cobbleutils.model.zones.zoneshapes.CuboidShape;
import com.kingpixel.cobbleutils.model.zones.zoneshapes.ZoneShape;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.server.network.ServerPlayerEntity;

@EqualsAndHashCode(callSuper = true)
@Data
public class ZoneCondition extends Condition implements VisualizableCondition {
  public static final String TYPE = "ZONE";
  private ZoneShape zone = new CuboidShape();

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    return zone.contains(player.getBlockPos());
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "";
  }


  @Override
  public void render(ServerPlayerEntity player) {
    zone.spawnParticles(player.getServerWorld(), player);
  }
}
