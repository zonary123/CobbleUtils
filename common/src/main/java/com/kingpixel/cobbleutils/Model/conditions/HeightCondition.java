package com.kingpixel.cobbleutils.Model.conditions;

import lombok.*;
import net.minecraft.server.network.ServerPlayerEntity;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@ToString(callSuper = true)
@Builder
@Data
public class HeightCondition extends Condition {
  public static final String TYPE = "HEIGHT";
  @Builder.Default
  private final int minHeight = 0;
  @Builder.Default
  private final int maxHeight = 256;

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    int y = player.getBlockPos().getY();
    return y >= minHeight && y <= maxHeight;
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "You need to be between Y levels " + minHeight + " and " + maxHeight;
  }


}
