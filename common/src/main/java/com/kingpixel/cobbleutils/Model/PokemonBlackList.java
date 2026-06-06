package com.kingpixel.cobbleutils.Model;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.util.NbtUtils;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Container and processor class for managing blacklists and filters
 * applied to Cobblemon Pokémon entities.
 * <p>
 * Designed to support multi-threaded environments (Thread-safe) due to the
 * asynchronous nature of entity spawning in Minecraft, preventing concurrent
 * modification exceptions by using a {@link ConcurrentHashMap}.
 * </p>
 *
 * @author Carlos Varas Alonso - 13/01/2025 2:17
 * @version 1.1.0
 */
@Getter
@Setter
public class PokemonBlackList {

  /**
   * Thread-safe results cache.
   * Prevents expensive recalculations during massive checks or server ticks.
   */
  private transient final Map<String, Boolean> resultsCache = new ConcurrentHashMap<>();

  /**
   * Defines whether only completely implemented Pokémon in the mod should be allowed.
   */
  private boolean onlyImplemented;

  /**
   * Defines the restrictive behavior with evolutions.
   * If {@code false}, it will block any form that is not the base pre-evolution of the Pokémon.
   */
  private boolean allowEvolutions = true;

  /**
   * Set of advanced property strings in Cobblemon format (e.g., "shiny=true").
   */
  private Set<String> properties;

  /**
   * Set of blocked Pokémon identifiers (e.g., "pikachu", "egg", or the global wildcard "*").
   */
  private Set<String> pokemons;

  /**
   * Set of specific blocked forms (e.g., "alolan", "galarian").
   */
  private Set<String> forms;

  /**
   * Set of blocked cosmetic aspects or statuses (e.g., "gmax", "alpha").
   */
  private Set<String> aspects;

  /**
   * Set of built-in Cobblemon labels (e.g., "legendary", "mythical", "ultra_beast").
   */
  private Set<String> labels;

  /**
   * Set of blocked elemental types (e.g., "water", "fire").
   */
  private Set<String> types;

  /**
   * Set of blocked custom or system rarities (e.g., "common", "legendary").
   */
  private Set<String> rarities;

  /**
   * Map of expected keys and values within the Pokémon's persistent NBT data for filtering.
   */
  private Map<String, List<Object>> persistentDataMap;

  /**
   * Set of prohibited egg groups (Egg Groups).
   */
  private Set<EggGroup> eggGroups;

  /**
   * Default constructor. Initializes all data structures and defines
   * default example values for initial configuration file generation.
   */
  public PokemonBlackList() {
    this.properties = new HashSet<>();
    this.pokemons = new HashSet<>();
    this.pokemons.add("egg");
    this.pokemons.add("pokestop");

    this.labels = new HashSet<>();
    this.labels.add("legendary");
    this.labels.add("mythical");
    this.labels.add("ultra_beast");

    this.types = new HashSet<>();
    this.types.add("water_example");

    this.forms = new HashSet<>();
    this.forms.add("hisuian_example");

    this.aspects = new HashSet<>();
    this.aspects.add("gmax_example");

    this.persistentDataMap = new HashMap<>();
    this.persistentDataMap.put("example_1", List.of("example_value_1", "example_value_2"));
    this.persistentDataMap.put("example_2", List.of(1, 2, 3));

    this.eggGroups = new HashSet<>();
    this.rarities = new HashSet<>();
  }

  /**
   * Static factory method to create a completely clean blacklist instance.
   *
   * @return A new empty instance of {@link PokemonBlackList}.
   */
  public static PokemonBlackList createBlackList() {
    PokemonBlackList blackList = new PokemonBlackList();
    blackList.clear();
    return blackList;
  }

  /**
   * Static factory method to create a restrictive default filter
   * initialized with the global wildcard block.
   *
   * @return A new instance of {@link PokemonBlackList} that blocks everything by default.
   */
  public static PokemonBlackList createFilter() {
    PokemonBlackList filter = new PokemonBlackList();
    filter.clear();
    filter.getPokemons().add("*");
    return filter;
  }

  /**
   * Normalizes all string values within the collections to lowercase
   * and removes leading/trailing whitespaces. Ensures that user configuration
   * input does not fail due to formatting differences (case-insensitive).
   */
  public void fix() {
    List<String> pokemonsBanneds = List.of("egg", "pokestop");
    this.pokemons.addAll(pokemonsBanneds);

    this.pokemons = this.pokemons.stream().map(String::toLowerCase).map(String::trim).collect(Collectors.toSet());
    this.forms = this.forms.stream().map(String::toLowerCase).map(String::trim).collect(Collectors.toSet());
    this.aspects = this.aspects.stream().map(String::toLowerCase).map(String::trim).collect(Collectors.toSet());
    this.labels = this.labels.stream().map(String::toLowerCase).map(String::trim).collect(Collectors.toSet());
    this.types = this.types.stream().map(String::toLowerCase).map(String::trim).collect(Collectors.toSet());
    this.rarities = this.rarities.stream().map(String::toLowerCase).map(String::trim).collect(Collectors.toSet());
  }

  /**
   * Completely resets all exclusion collections and safely flushes the
   * results cache, preventing cache leaks during hot-reloads (/reload).
   */
  public void clear() {
    this.properties.clear();
    this.pokemons.clear();
    this.forms.clear();
    this.aspects.clear();
    this.labels.clear();
    this.types.clear();
    this.rarities.clear();
    this.persistentDataMap.clear();
    this.eggGroups.clear();
    this.resultsCache.clear();
  }

  /**
   * Evaluates if a Pokémon is blacklisted. Bridge method compatible with legacy calls.
   *
   * @param pokemon The Pokémon to evaluate.
   *
   * @return {@code true} if it is blacklisted; {@code false} otherwise.
   *
   * @see #isBlacklisted(Pokemon)
   */
  public boolean isBlackListed(Pokemon pokemon) {
    return isBlacklisted(pokemon);
  }

  /**
   * Exhaustively evaluates whether a Pokémon instance meets the restriction
   * criteria of the blacklist.
   * Evaluation operations are optimized via early conditional returns and caching.
   *
   * @param pokemon The Cobblemon {@link Pokemon} to evaluate.
   *
   * @return {@code true} if the Pokémon matches any blocking parameter; {@code false} if allowed.
   */
  public boolean isBlacklisted(Pokemon pokemon) {
    if (pokemon == null) return false;

    // 1. Quick check for the global wildcard block
    if (this.pokemons.contains("*")) return true;

    // 2. Advanced Cobblemon property checks (Not cacheable due to dynamism)
    if (!this.properties.isEmpty()) {
      for (String property : this.properties) {
        if (PokemonProperties.Companion.parse(property).matches(pokemon)) return true;
      }
    }

    // 3. Dynamic and forced aspect checks (Not cacheable due to volatility)
    if (!this.aspects.isEmpty()) {
      for (String aspect : pokemon.getAspects()) {
        if (this.aspects.contains(aspect)) return true;
      }
      for (String forcedAspect : pokemon.getForcedAspects()) {
        if (this.aspects.contains(forcedAspect)) return true;
      }
    }

    // 4. Persistent NBT data check
    if (!this.persistentDataMap.isEmpty()) {
      NbtCompound persistentData = pokemon.getPersistentData();
      for (String key : persistentData.getKeys()) {
        List<Object> list = this.persistentDataMap.get(key);
        if (list == null || list.isEmpty()) continue;

        NbtElement element = persistentData.get(key);
        Object convertedNbt = NbtUtils.convertNbtValue(element);

        // Note: If using GSON/JSON, remember that integers might deserialize into Doubles (e.g., 1 to 1.0)
        if (list.contains("*") || list.contains(convertedNbt)) return true;
      }
    }

    // 5. Cache check for properties based on species/static form
    String showdownId = pokemon.showdownId();
    Boolean cached = this.resultsCache.get(showdownId);
    if (cached != null) return cached;

    // 6. Restriction based on implementation status in the mod
    if (this.onlyImplemented && !pokemon.getSpecies().getImplemented()) {
      return cacheResult(showdownId, true);
    }

    var form = pokemon.getForm();
    String formShowdownId = form.showdownId();

    // 7. Global restriction to prevent evolutions (Allows only first evolutionary forms)
    if (!this.allowEvolutions) {
      Pokemon firstEvolution = PokemonUtils.getFirstEvolution(pokemon);
      if (!firstEvolution.getForm().showdownId().equals(showdownId)) {
        return cacheResult(showdownId, true);
      }
    }

    // 8. Filtering by Egg Groups
    if (!this.eggGroups.isEmpty()) {
      for (EggGroup eggGroup : form.getEggGroups()) {
        if (this.eggGroups.contains(eggGroup)) {
          return cacheResult(showdownId, true);
        }
      }
    }

    // 9. Filtering by direct match of species ID or form ID
    if (this.pokemons.contains(formShowdownId) || this.pokemons.contains(showdownId)) {
      return cacheResult(showdownId, true);
    }

    // 10. Filtering by Labels
    if (!this.labels.isEmpty()) {
      for (String label : form.getLabels()) {
        if (this.labels.contains(label)) {
          return cacheResult(showdownId, true);
        }
      }
    }

    // 11. Filtering by unique internal form name
    if (!this.forms.isEmpty() && this.forms.contains(form.formOnlyShowdownId())) {
      return cacheResult(showdownId, true);
    }

    // 12. Filtering by elemental types associated with the form
    if (!this.types.isEmpty()) {
      for (ElementalType type : form.getTypes()) {
        String typeName = type.getName().toLowerCase();
        if (this.types.contains(typeName)) {
          return cacheResult(showdownId, true);
        }
      }
    }

    // 13. Filtering by specified rarity levels
    if (!this.rarities.isEmpty()) {
      String rarity = PokemonUtils.getRarityS(pokemon);
      if (rarity != null && this.rarities.contains(rarity.toLowerCase().trim())) {
        return cacheResult(showdownId, true);
      }
    }

    // If it successfully passes all filters, the Pokémon is allowed
    return cacheResult(showdownId, false);
  }

  /**
   * Safely stores an entry in the internal results cache.
   *
   * @param showdownId Unique showdown identifier corresponding to the species/form evaluated.
   * @param result     Logical result of the filtering ({@code true} = Blacklisted, {@code false} = Allowed).
   *
   * @return The exact same value assigned to the {@code result} parameter.
   */
  public boolean cacheResult(String showdownId, boolean result) {
    this.resultsCache.putIfAbsent(showdownId, result);
    return result;
  }
}