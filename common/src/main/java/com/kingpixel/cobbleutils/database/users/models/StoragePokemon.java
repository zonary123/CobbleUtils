package com.kingpixel.cobbleutils.database.users.models;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 06/10/2025 5:11
 */
@EqualsAndHashCode(callSuper = true) @Data
public class StoragePokemon extends Storage {
  private String type = "pokemon";
  private Pokemon pokemon;

  public StoragePokemon(UUID id, Pokemon pokemon) {
    this.setId(id);
    this.pokemon = pokemon;
  }

  @Override public ItemStack getDisplay() {
    return PokemonItem.from(pokemon);
  }

  @Override public void giveToPlayer(ServerPlayerEntity player) {
    CobbleUtils.server.execute(() -> Cobblemon.INSTANCE.getStorage().getParty(player).add(pokemon));
  }


}
