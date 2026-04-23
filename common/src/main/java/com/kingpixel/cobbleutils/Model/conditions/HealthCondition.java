package com.kingpixel.cobbleutils.Model.conditions;

import lombok.*;
import net.minecraft.server.network.ServerPlayerEntity;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Builder
@Data
public class HealthCondition extends Condition {
  public static final String TYPE = "HEALTH";
  @Builder.Default
  private float minHealth = 0f;
  @Builder.Default
  private float maxHealth = 20f;

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    float health = player.getHealth();
    return health >= minHealth && health <= maxHealth;
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "Your health must be between " + minHealth + " and " + maxHealth + " (current: " + player.getHealth() + ")";
  }
}

