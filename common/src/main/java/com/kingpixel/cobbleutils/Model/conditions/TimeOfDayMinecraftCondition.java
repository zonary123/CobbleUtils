package com.kingpixel.cobbleutils.Model.conditions;


import lombok.*;
import net.minecraft.server.network.ServerPlayerEntity;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Builder
@Data
public class TimeOfDayMinecraftCondition extends Condition {

  public static final String TYPE = "TIME_OF_DAY_MINECRAFT";
  @Builder.Default
  private long minTime = 0;
  @Builder.Default
  private long maxTime = 24000;


  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    long time = player.getWorld().getTimeOfDay() % 24000;
    return time >= minTime && time <= maxTime;
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "Time condition not met.";
  }
}