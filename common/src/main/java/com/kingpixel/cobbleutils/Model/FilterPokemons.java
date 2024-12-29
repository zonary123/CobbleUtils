package com.kingpixel.cobbleutils.Model;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonPropertyExtractor;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Data;
import lombok.Getter;

import java.util.*;

/**
 * Improved version of FilterPokemons class
 *
 * @author
 */
@Data
public class FilterPokemons {
  // Cache <ModId, <Id, List<Pokemon>>>
  private static final Map<String, Map<String, List<Pokemon>>> cache = new HashMap<>();

  // Also implemented
  private boolean alsoImplemented;
  // Also First Evolution
  private boolean notEvolution;
  // Legendarys
  private boolean legendarys;
  // Order
  private Set<orderFilter> order;

  public enum orderFilter {
    POKEMON,
    TYPE,
    LABEL,
    FORM,
    RARITY,
    LEGENDARY
  }

  // Pokemons
  private Set<String> whitelistPokemons;
  private Set<String> blacklistPokemons;
  // Types
  private boolean allowOneTypeRequired;
  private Set<ElementalType> whitelistTypes;
  private Set<ElementalType> blacklistTypes;
  // Labels
  private Set<String> whitelistLabels;
  private Set<String> blacklistLabels;
  // Forms
  private Set<String> whitelistForms;
  private Set<String> blacklistForms;
  // Aspects
  private Set<String> whitelistAspects;
  private Set<String> blacklistAspects;

  // Rarity
  private Set<String> whitelistRarity;
  private Set<String> blacklistRarity;

  // Use Chances
  private boolean useChances;
  private List<AdvancedPokemonChance> pokemonsChances;

  @Getter
  private static class AdvancedPokemonChance {
    private List<String> pokemons;
    private float chance;

    public AdvancedPokemonChance() {
      this.pokemons = new ArrayList<>();
      this.chance = 1.0f;
    }

    public AdvancedPokemonChance(List<String> pokemons, float chance) {
      this.pokemons = pokemons;
      this.chance = chance;
    }

    public static List<AdvancedPokemonChance> defaultChances() {
      List<AdvancedPokemonChance> chances = new ArrayList<>();
      chances.add(new AdvancedPokemonChance(List.of("rattata",
        "rattata alolan"), 5.0f));
      chances.add(new AdvancedPokemonChance(List.of("pikachu"), 3.0f));
      return chances;
    }

    public static List<Pokemon> getPokemons(List<AdvancedPokemonChance> pokemonChances) {
      List<Pokemon> pokemons = new ArrayList<>();
      for (AdvancedPokemonChance pokemonChance : pokemonChances) {
        int size = pokemonChance.getPokemons().size();
        for (int i = 0; i < size; i++) {
          Pokemon pokemon = PokemonProperties.Companion.parse(pokemonChance.getPokemons().get(i)).create();
          pokemons.add(pokemon);
        }
      }
      return pokemons;
    }

    public static Pokemon getPokemon(List<AdvancedPokemonChance> pokemonChances) {
      double totalChance = pokemonChances.stream().mapToDouble(pokemon -> pokemon.chance).sum();
      double randomValue = Utils.RANDOM.nextDouble() * totalChance;
      for (AdvancedPokemonChance pokemonChance : pokemonChances) {
        randomValue -= pokemonChance.getChance();
        if (randomValue <= 0) {
          int size = pokemonChance.getPokemons().size();
          Pokemon pokemon = PokemonProperties.Companion.parse(pokemonChance.getPokemons().get(Utils.RANDOM.nextInt(size))).create();
          return pokemon;
        }
      }
      // En caso de algún error inesperado, retorna el último Pokémon en la lista
      return PokemonProperties.Companion.parse("rattata").create();
    }
  }

  public FilterPokemons() {
    // Order
    order = new HashSet<>(Arrays.stream(orderFilter.values()).toList());

    // Pokemons
    whitelistPokemons = new HashSet<>();
    blacklistPokemons = Set.of(
      "egg",
      "pokestop"
    );

    // Types
    allowOneTypeRequired = true;
    whitelistTypes = new HashSet<>(ElementalTypes.INSTANCE.all());
    blacklistTypes = new HashSet<>();

    // Labels
    whitelistLabels = new HashSet<>();
    blacklistLabels = Set.of(
      "mega",
      "gmax",
      "fakemon",
      "custom"
    );

    // Forms
    whitelistForms = new HashSet<>();
    blacklistForms = new HashSet<>();

    // Aspects
    blacklistAspects = new HashSet<>();

    // Rarities
    whitelistRarity = new HashSet<>(CobbleUtils.config.getRarity().keySet());
    blacklistRarity = Set.of("Unknown");

    alsoImplemented = true;
    notEvolution = false;
    legendarys = false;

    useChances = false;
    pokemonsChances = AdvancedPokemonChance.defaultChances();
  }

  private void checker() {
    if (blacklistPokemons == null || blacklistPokemons.isEmpty()) {
      blacklistPokemons = Set.of(
        "egg",
        "pokestop"
      );
    }
    if (!blacklistPokemons.contains("egg")) {
      blacklistPokemons.add("egg");
    }
    if (!blacklistPokemons.contains("pokestop")) {
      blacklistPokemons.add("pokestop");
    }

    if (whitelistTypes == null) {
      whitelistTypes = new HashSet<>(ElementalTypes.INSTANCE.all());
    }

    if (blacklistTypes == null) {
      blacklistTypes = new HashSet<>();
    }

    if (whitelistTypes.contains(null)) {
      whitelistTypes.remove(null);
    }
    if (blacklistTypes.contains(null)) {
      blacklistTypes.remove(null);
    }
  }

  public static void removeCache(String modid) {
    cache.remove(modid);
  }

  /**
   * Gets the cache of the pokemons
   *
   * @param modId the mod id
   * @param id    the id
   *
   * @return the list of pokemons
   */
  public List<Pokemon> getCachePokemons(String modId, String id) {
    if (isUseChances()) {
      List<Pokemon> allowedPokemons = AdvancedPokemonChance.getPokemons(getPokemonsChances());
      allowedPokemons = allowedPokemons.stream().map(this::getPokemon).toList();
      return allowedPokemons;
    } else {
      List<Pokemon> allowedPokemons;
      if (cache.containsKey(modId) && cache.get(modId).containsKey(id)) {
        if (cache.get(modId).get(id).isEmpty()) {
          allowedPokemons = getAllowedPokemons();
          cache.get(modId).put(id, allowedPokemons);
        }
        allowedPokemons = cache.get(modId).get(id);
      } else {
        checker();
        allowedPokemons = getAllowedPokemons();
        cache.putIfAbsent(modId, new HashMap<>());
        cache.get(modId).put(id, allowedPokemons);
      }
      return allowedPokemons;
    }
  }

  /**
   * Gets a pokemon with properties
   *
   * @param pokemon the pokemon
   *
   * @return the pokemon
   */
  private Pokemon getPokemon(Pokemon pokemon) {
    Pokemon copy = pokemon.clone(true);
    copy.createPokemonProperties(List.of(
      PokemonPropertyExtractor.NATURE,
      PokemonPropertyExtractor.IVS,
      PokemonPropertyExtractor.GENDER,
      PokemonPropertyExtractor.POKEBALL
    )).apply(copy);
    return copy;
  }

  /**
   * Generates a random pokemon
   *
   * @param modId the mod id
   * @param id    the id
   *
   * @return the pokemon
   */
  public Pokemon generateRandomPokemon(String modId, String id) {
    if (isUseChances()) {
      return getPokemon(AdvancedPokemonChance.getPokemon(getPokemonsChances()));
    } else {
      List<Pokemon> allowedPokemons = getCachePokemons(modId, id);
      return getPokemon(allowedPokemons.get(Utils.RANDOM.nextInt(allowedPokemons.size())));
    }

  }


  /**
   * Generates a list of random pokemons
   *
   * @param modId the mod id
   * @param id    the id
   * @param size  the size of the list
   *
   * @return the list of pokemons
   */
  public List<Pokemon> generateRandomPokemons(String modId, String id, int size) {
    if (isUseChances()) {
      List<Pokemon> pokemons = new ArrayList<>();
      for (int i = 0; i < size; i++) {
        pokemons.add(AdvancedPokemonChance.getPokemon(getPokemonsChances()));
      }
      return pokemons;
    } else {
      List<Pokemon> allowedPokemons = getCachePokemons(modId, id);

      List<Pokemon> pokemons = new ArrayList<>();
      for (int i = 0; i < size; i++) {
        pokemons.add(getPokemon(allowedPokemons.get(Utils.RANDOM.nextInt(allowedPokemons.size()))));
      }
      return pokemons;
    }
  }

  /**
   * Gets all the allowed pokemons
   *
   * @return the list of allowed pokemons
   */
  public List<Pokemon> getAllowedPokemons() {
    if (order == null || order.contains(null)) {
      this.order = new HashSet<>(Arrays.stream(orderFilter.values()).toList());
    }
    List<Pokemon> allowedPokemons = new ArrayList<>();
    List<String> pokemonIds = new ArrayList<>();
    PokemonSpecies.INSTANCE.getSpecies().forEach(pokemon -> {
      List<FormData> forms = pokemon.getForms();
      if (forms.isEmpty()) {
        Pokemon p = pokemon.create(1);
        if (pokemonIds.contains(p.showdownId())) return;
        if (isAllowed(p)) {
          pokemonIds.add(p.showdownId());
          allowedPokemons.add(p);
        }
      } else {
        List<String> yetAspects = new ArrayList<>();
        forms.forEach(form -> {
          Pokemon p;
          List<String> aspects = form.getAspects();
          if (aspects.isEmpty()) {
            p = pokemon.create(1);
          } else {
            String aspect = aspects.get(0);
            aspect = aspect.replace("-", "_");

            int lastUnderscore = aspect.lastIndexOf("_");
            if (lastUnderscore != -1) {
              aspect = aspect.substring(0, lastUnderscore) + "=" + aspect.substring(lastUnderscore + 1);
            }

            if (blacklistAspects.contains(aspect)) return;

            p = PokemonProperties.Companion.parse(pokemon.showdownId() + " " + aspect).create();
            if (yetAspects.contains(p.showdownId())) return;
            yetAspects.add(p.showdownId());
          }
          if (pokemonIds.contains(p.showdownId())) return;
          if (isAllowed(p)) {
            allowedPokemons.add(p);
            pokemonIds.add(p.showdownId());
          }
        });
      }
    });

    return allowedPokemons;
  }

  private boolean isFirstEvolution(Pokemon pokemon) {
    return pokemon.getPreEvolution() != null;
  }

  private boolean canEgg(Pokemon pokemon) {
    return !pokemon.getForm().getEggGroups().contains(EggGroup.DITTO) || !pokemon.getForm().getEggGroups().contains(EggGroup.UNDISCOVERED);
  }

  /**
   * Checks if a pokemon is allowed
   *
   * @param pokemon the pokemon
   *
   * @return true if the pokemon is allowed
   */
  private boolean isAllowed(Pokemon pokemon) {
    if (notEvolution && isFirstEvolution(pokemon)) return false;
    if (alsoImplemented && !pokemon.getSpecies().getImplemented()) return false;
    if (order == null || order.isEmpty()) return true;
    // Precalcular tipos
    ElementalType primaryType = pokemon.getPrimaryType();
    ElementalType secondaryType = pokemon.getSecondaryType();
    String showdownId = pokemon.showdownId();
    String rarity = PokemonUtils.getRarityS(pokemon);
    boolean allowed = false;

    for (orderFilter filter : order) {
      if (filter == null) continue;
      switch (filter) {
        case POKEMON:
          if (blacklistPokemons.contains(showdownId) || blacklistPokemons.contains("*")) {
            return false;
          }
          allowed |= whitelistPokemons.contains("*") || whitelistPokemons.contains(showdownId);
          break;
        case TYPE:
          if (blacklistTypes.contains(primaryType) || blacklistTypes.contains(secondaryType)) {
            if (allowOneTypeRequired && (whitelistTypes.contains(primaryType) || whitelistTypes.contains(secondaryType))) {
              allowed = true;
            } else {
              return false;
            }
          } else {
            allowed |= whitelistTypes.contains(primaryType) || whitelistTypes.contains(secondaryType);
          }
          break;
        case LABEL:
          boolean hasBlacklistedLabel = blacklistLabels.stream().anyMatch(label -> pokemon.getForm().getLabels().contains(label));
          if (hasBlacklistedLabel || blacklistLabels.contains("*")) {
            return false;
          }
          allowed |= whitelistLabels.contains("*") || whitelistLabels.stream().anyMatch(label -> pokemon.getForm().getLabels().contains(label));
          break;

        case FORM:
          if (blacklistForms.contains(pokemon.getForm().formOnlyShowdownId()) || blacklistForms.contains("*")) {
            return false;
          }
          allowed |= whitelistForms.contains("*") || whitelistForms.contains(pokemon.getForm().formOnlyShowdownId());
          break;

        case RARITY:
          if (blacklistRarity.contains(rarity) || blacklistRarity.contains("*")) {
            return false;
          }
          allowed |= whitelistRarity.contains("*") || whitelistRarity.contains(rarity);
          break;
        case LEGENDARY:
          if (!legendarys && pokemon.isLegendary()) {
            return false;
          }
          allowed = true;
          break;
      }
    }
    return allowed;
  }
}