package com.kingpixel.cobbleutils.Model.conditions;


import lombok.*;
import net.minecraft.server.network.ServerPlayerEntity;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Builder
@Data
public class WeatherCondition extends Condition {

  public static final String TYPE = "WEATHER";
  @Builder.Default
  private boolean requiresRaining = true;
  @Builder.Default
  private boolean requiresThundering = false;

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    if (requiresThundering) {
      return player.getWorld().isThundering();
    }
    return requiresRaining == player.getWorld().isRaining();
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "Weather condition not met.";
  }
}
