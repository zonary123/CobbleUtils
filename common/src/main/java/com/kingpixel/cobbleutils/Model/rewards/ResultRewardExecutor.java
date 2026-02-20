package com.kingpixel.cobbleutils.Model.rewards;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultRewardExecutor {
  @Builder.Default
  private boolean success = false;
  @Builder.Default
  private String message = "";
}
