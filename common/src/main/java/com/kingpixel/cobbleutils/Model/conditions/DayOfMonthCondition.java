package com.kingpixel.cobbleutils.Model.conditions;

import lombok.*;
import net.minecraft.server.network.ServerPlayerEntity;

import java.time.LocalDate;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Builder
@Data
public class DayOfMonthCondition extends Condition {
  public static final String TYPE = "DAY_OF_MONTH";
  @Builder.Default
  private Set<Integer> days = Set.of(1, 15);

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    int today = LocalDate.now().getDayOfMonth();
    return days.contains(today);
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "This action is only available on days: " + days;
  }
}

