package com.kingpixel.cobbleutils.model.conditions;


import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.server.network.ServerPlayerEntity;

@EqualsAndHashCode(callSuper = true)
@Data
public class WeatherCondition extends Condition {

  public static final String TYPE = "WEATHER";

  private final boolean requiresRaining = true;

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    boolean raining = player.getWorld().isRaining();
    return requiresRaining == raining;
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "Weather condition not met.";
  }
}
