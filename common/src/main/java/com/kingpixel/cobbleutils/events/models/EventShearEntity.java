package com.kingpixel.cobbleutils.events.models;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collections;
import java.util.List;

/**
 * Event model fired when an entity (vanilla mob or Cobblemon Pokemon) is sheared.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventShearEntity {
  private ServerPlayerEntity player;
  private Entity entity;
  private ItemStack tool;
  private List<ItemStack> drops;

  public List<ItemStack> getDrops() {
    if (drops == null) return Collections.emptyList();
    return drops;
  }

  public boolean isPokemon() {
    return entity instanceof PokemonEntity;
  }

  public Pokemon getPokemon() {
    if (entity instanceof PokemonEntity pokemonEntity) {
      return pokemonEntity.getPokemon();
    }
    return null;
  }

  public PokemonEntity getPokemonEntity() {
    if (entity instanceof PokemonEntity pokemonEntity) {
      return pokemonEntity;
    }
    return null;
  }
}
