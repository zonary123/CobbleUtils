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
public class MonthCondition extends Condition {
  public static final String TYPE = "MONTH";
  @Builder.Default
  private Set<Integer> months = Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    int month = LocalDate.now().getMonthValue();
    return months.contains(month);
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "This action is only available in months: " + months;
  }
}

