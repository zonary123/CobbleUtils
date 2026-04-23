package com.kingpixel.cobbleutils.Model.conditions;

import lombok.*;
import net.minecraft.server.network.ServerPlayerEntity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Builder
@Data
public class DayOfWeekCondition extends Condition {
  public static final String TYPE = "DAY_OF_WEEK";
  @Builder.Default
  private Set<String> days = Set.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    String today = LocalDate.now().getDayOfWeek().name();
    return days.stream().anyMatch(d -> d.equalsIgnoreCase(today));
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "This action is only available on: " + String.join(", ", days);
  }
}

