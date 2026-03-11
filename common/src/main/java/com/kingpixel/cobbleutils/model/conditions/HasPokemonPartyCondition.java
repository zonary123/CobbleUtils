package com.kingpixel.cobbleutils.model.conditions;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.model.PokemonBlackList;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.server.network.ServerPlayerEntity;

@EqualsAndHashCode(callSuper = true)
@Data
public class HasPokemonPartyCondition extends Condition {
  public static final String TYPE = "HAS_POKEMON_PARTY";
  private PokemonBlackList filter = new PokemonBlackList();

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    var party = Cobblemon.INSTANCE.getStorage().getParty(player);
    for (Pokemon pokemon : party) {
      if (filter.isBlackListed(pokemon)) return true;
    }
    return false;
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "You need to have a pokemon in your party that matches the filter.";
  }


}
