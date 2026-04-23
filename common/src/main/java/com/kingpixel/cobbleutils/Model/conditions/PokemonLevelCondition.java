package com.kingpixel.cobbleutils.Model.conditions;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import lombok.*;
import net.minecraft.server.network.ServerPlayerEntity;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Builder
@Data
public class PokemonLevelCondition extends Condition {
  public static final String TYPE = "POKEMON_LEVEL";
  @Builder.Default
  private int minLevel = 1;
  @Builder.Default
  private int maxLevel = 100;
  @Builder.Default
  private boolean anyInParty = true;

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    var party = Cobblemon.INSTANCE.getStorage().getParty(player);
    for (Pokemon pokemon : party) {
      int level = pokemon.getLevel();
      if (level >= minLevel && level <= maxLevel) {
        if (anyInParty) return true;
      } else {
        if (!anyInParty) return false;
      }
    }
    return !anyInParty;
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    if (anyInParty) {
      return "You need at least one Pokémon between level " + minLevel + " and " + maxLevel;
    }
    return "All Pokémon in your party must be between level " + minLevel + " and " + maxLevel;
  }
}

