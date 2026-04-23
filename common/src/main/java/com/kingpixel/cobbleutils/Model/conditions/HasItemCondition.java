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
public class HasItemCondition extends Condition {
  public static final String TYPE = "HAS_ITEM";
  @Builder.Default
  private Set<String> itemIds = Set.of("minecraft:diamond");
  @Builder.Default
  private int minAmount = 1;

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    for (String itemId : itemIds) {
      int count = 0;
      for (int i = 0; i < player.getInventory().size(); i++) {
        var stack = player.getInventory().getStack(i);
        if (Registries.ITEM.getId(stack.getItem()).toString().equals(itemId)) {
          count += stack.getCount();
        }
      }
      if (count >= minAmount) return true;
    }
    return false;
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "You need at least " + minAmount + " of one of: " + String.join(", ", itemIds);
  }
}

