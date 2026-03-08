package com.kingpixel.cobbleutils.database.users.models;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 06/10/2025 5:11
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class StoragePokemon extends Storage {
  private final String type = "pokemon";
  private Pokemon pokemon;

  public StoragePokemon(Pokemon pokemon) {
    super();
    this.pokemon = pokemon;
  }

  public StoragePokemon(UUID id, Pokemon pokemon) {
    super(id);
    this.pokemon = pokemon;
  }

  @Override
  public ItemStack getDisplay() {
    ItemStack itemStack = PokemonItem.from(pokemon);
    itemStack.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(
      PokemonUtils.replace(pokemon)
    ));
    itemStack.set(DataComponentTypes.LORE, new LoreComponent(
      AdventureTranslator.toNativeL(PokemonUtils.replaceLore(pokemon))
    ));
    return itemStack;
  }

  @Override
  public CompletableFuture<Boolean> giveToPlayer(@NotNull ServerPlayerEntity player) {
    return CobbleUtils.server.submit(() -> {
      var party = Cobblemon.INSTANCE.getStorage().getParty(player);
      return party.add(pokemon);
    });
  }


}
