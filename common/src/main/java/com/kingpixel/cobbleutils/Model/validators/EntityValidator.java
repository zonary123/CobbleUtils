package com.kingpixel.cobbleutils.Model.validators;

import lombok.*;
import net.minecraft.entity.Entity;

import java.util.HashSet;
import java.util.Set;

/**
 * Validator for Minecraft entities.
 * <p>
 * Checks whether an entity is valid based on a list of entity IDs and a blacklist.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true)
public class EntityValidator extends AbstractRegistryValidator<Entity> {

  /**
   * List of allowed entity IDs. Supports wildcards and regex.
   */
  @Builder.Default
  private Set<String> entityIds = new HashSet<>(Set.of(
    "*",
    "regex:.*",
    "minecraft:pig"
  ));

  @Override
  protected Set<String> getIdSet() {
    return entityIds;
  }

  @Override
  protected Set<String> getTagSet() {
    return Set.of();
  }

  @Override
  protected String getId(@NonNull Entity entity) {
    if (entity instanceof com.cobblemon.mod.common.entity.pokemon.PokemonEntity pokemonEntity) {
      if (pokemonEntity.getPokemon() != null) {
        return "cobblemon:" + pokemonEntity.getPokemon().getSpecies().getName().toLowerCase();
      }
    }
    return entity.getSavedEntityId();
  }

  @Override
  public boolean isValid(@NonNull Entity entity) {
    if (super.isValid(entity)) return true;
    String id = getId(entity);
    if (id != null && id.contains(":")) {
      String path = id.substring(id.indexOf(':') + 1);
      if (ValidatorUtil.match(path, this.getIdSet()) && !ValidatorUtil.match(path, this.getBlacklist())) {
        return true;
      }
    }
    if (entity instanceof com.cobblemon.mod.common.entity.pokemon.PokemonEntity) {
      String genericId = entity.getSavedEntityId();
      if (genericId != null && ValidatorUtil.match(genericId, this.getIdSet()) && !ValidatorUtil.match(genericId, this.getBlacklist())) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected boolean isInTag(@NonNull Entity entity) {
    return false;
  }
}