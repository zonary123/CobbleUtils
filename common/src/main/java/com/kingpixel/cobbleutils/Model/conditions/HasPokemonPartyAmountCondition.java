package com.kingpixel.cobbleutils.Model.conditions;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.Model.PokemonBlackList;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.server.network.ServerPlayerEntity;

@EqualsAndHashCode(callSuper = true)
@Data
public class HasPokemonPartyAmountCondition extends Condition {
  public static final String TYPE = "HAS_POKEMON_PARTY_AMOUNT";
  private PokemonBlackList filter = new PokemonBlackList();
  private int size = 1;

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    var party = Cobblemon.INSTANCE.getStorage().getParty(player);
    int count = 0;
    for (Pokemon pokemon : party) {
      if (filter.isBlackListed(pokemon)) count++;
      if (count >= size) return true;
    }
    return count >= size;
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "You need to have a pokemon in your party that matches the filter.";
  }


}
