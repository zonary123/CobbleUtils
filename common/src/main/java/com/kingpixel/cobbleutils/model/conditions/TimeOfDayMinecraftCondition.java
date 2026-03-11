package com.kingpixel.cobbleutils.model.conditions;


import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.server.network.ServerPlayerEntity;

@EqualsAndHashCode(callSuper = true)
@Data
public class TimeOfDayMinecraftCondition extends Condition {

  public static final String TYPE = "time";

  private long minTime = 0;
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