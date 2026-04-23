package com.kingpixel.cobbleutils.Model.conditions;

import lombok.*;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Builder
@Data
public class EffectCondition extends Condition {
  public static final String TYPE = "EFFECT";
  @Builder.Default
  private Set<String> effects = Set.of("minecraft:speed");
  @Builder.Default
  private int minAmplifier = 0;

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    for (String effectId : effects) {
      Optional<RegistryEntry.Reference<StatusEffect>> entry =
        Registries.STATUS_EFFECT.getEntry(Identifier.of(effectId));
      if (entry.isPresent()) {
        var instance = player.getStatusEffect(entry.get());
        if (instance != null && instance.getAmplifier() >= minAmplifier) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "You need one of these effects: " + String.join(", ", effects)
      + " (min amplifier: " + minAmplifier + ")";
  }
}

