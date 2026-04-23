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
public class DateRangeCondition extends Condition {
  public static final String TYPE = "DATE_RANGE";
  @Builder.Default
  private String startDate = "2025-01-01";
  @Builder.Default
  private String endDate = "2025-12-31";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    LocalDate now = LocalDate.now();
    LocalDate start = LocalDate.parse(startDate);
    LocalDate end = LocalDate.parse(endDate);
    return !now.isBefore(start) && !now.isAfter(end);
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "This action is only available between " + startDate + " and " + endDate;
  }
}

