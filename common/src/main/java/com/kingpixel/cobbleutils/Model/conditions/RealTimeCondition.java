package com.kingpixel.cobbleutils.Model.conditions;

import lombok.*;
import net.minecraft.server.network.ServerPlayerEntity;

import java.time.LocalTime;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Builder
@Data
public class RealTimeCondition extends Condition {
  public static final String TYPE = "REAL_TIME";
  @Builder.Default
  private String minTime = "00:00";
  @Builder.Default
  private String maxTime = "23:59";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    LocalTime now = LocalTime.now();
    LocalTime min = LocalTime.parse(minTime);
    LocalTime max = LocalTime.parse(maxTime);
    if (min.isBefore(max) || min.equals(max)) {
      return !now.isBefore(min) && !now.isAfter(max);
    } else {
      return !now.isBefore(min) || !now.isAfter(max);
    }
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "This action is only available between " + minTime + " and " + maxTime + " (real time)";
  }
}

