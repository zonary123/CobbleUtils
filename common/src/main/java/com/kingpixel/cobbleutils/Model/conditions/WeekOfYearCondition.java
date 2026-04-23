package com.kingpixel.cobbleutils.Model.conditions;

import lombok.*;
import net.minecraft.server.network.ServerPlayerEntity;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Builder
@Data
public class WeekOfYearCondition extends Condition {
  public static final String TYPE = "WEEK_OF_YEAR";
  @Builder.Default
  private int minWeek = 1;
  @Builder.Default
  private int maxWeek = 52;

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    int week = LocalDate.now().getDayOfYear() / 7 + 1;
    return week >= minWeek && week <= maxWeek;
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "This action is only available between weeks " + minWeek + " and " + maxWeek;
  }
}

