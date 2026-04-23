package com.kingpixel.cobbleutils.Model.conditions;

import lombok.*;
import net.minecraft.server.network.ServerPlayerEntity;

import java.time.Year;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Builder
@Data
public class YearCondition extends Condition {
  public static final String TYPE = "YEAR";
  @Builder.Default
  private Set<Integer> years = Set.of(2025, 2026);

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    int year = Year.now().getValue();
    return years.contains(year);
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "This action is only available in years: " + years;
  }
}

