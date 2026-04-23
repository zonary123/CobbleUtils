package com.kingpixel.cobbleutils.Model.conditions;

import lombok.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.LightType;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Builder
@Data
public class LightLevelCondition extends Condition {
  public static final String TYPE = "LIGHT_LEVEL";
  @Builder.Default
  private int minLight = 0;
  @Builder.Default
  private int maxLight = 15;
  @Builder.Default
  private String lightType = "BLOCK";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    int light;
    if ("SKY".equalsIgnoreCase(lightType)) {
      light = player.getWorld().getLightLevel(LightType.SKY, player.getBlockPos());
    } else if ("BLOCK".equalsIgnoreCase(lightType)) {
      light = player.getWorld().getLightLevel(LightType.BLOCK, player.getBlockPos());
    } else {
      light = player.getWorld().getLightLevel(player.getBlockPos());
    }
    return light >= minLight && light <= maxLight;
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "Light level must be between " + minLight + " and " + maxLight + " (" + lightType + ")";
  }
}

