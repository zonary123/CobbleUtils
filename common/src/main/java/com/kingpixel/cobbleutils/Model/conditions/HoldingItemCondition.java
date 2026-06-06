package com.kingpixel.cobbleutils.Model.conditions;

import lombok.*;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Builder
@Data
public class HoldingItemCondition extends Condition {
  public static final String TYPE = "HOLDING_ITEM";
  @Builder.Default
  private Set<String> itemIds = Set.of("minecraft:diamond_sword");
  @Builder.Default
  private boolean mainHand = true;
  @Builder.Default
  private boolean offHand = false;

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    if (mainHand) {
      String id = Registries.ITEM.getId(player.getMainHandStack().getItem()).toString();
      if (itemIds.contains(id)) return true;
    }
    if (offHand) {
      String id = Registries.ITEM.getId(player.getOffHandStack().getItem()).toString();
      return itemIds.contains(id);
    }
    return false;
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "You must be holding one of: " + String.join(", ", itemIds);
  }
}

