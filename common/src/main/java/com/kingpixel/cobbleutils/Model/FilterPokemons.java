package com.kingpixel.cobbleutils.Model;

import ca.landonjw.gooeylibs2.api.button.ButtonAction;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonPropertyExtractor;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.kingpixel.cobbleutils.ui.editor.FilterPokemonsVisualMenu;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Improved version of FilterPokemons class
 * Todo: Improve this system:
 *  - Remove order and use a better system
 *  - Remove whitelist
 *  - Do that the cache is removed every 1h
 *
 * @author
 */
@Data
public class FilterPokemons {
  // Cache <ModId, <Id, List<Pokemon>>>
  private static final Map<String, Map<String, List<Pokemon>>> CACHE = new HashMap<>();


  // BlackList
  public PokemonBlackList blackList;
  // Use Chances
  private boolean useChances;
  private List<AdvancedPokemonChance> pokemonsChances;
  // Old BlackList
  private Set<String> blacklistPokemons;
  private Set<ElementalType> blacklistTypes;
  private Set<String> blacklistLabels;
  private Set<String> blacklistForms;
  private Set<String> blacklistAspects;
  private Set<String> blacklistRarity;
  private Boolean notEvolution;// Also First Evolution
  private boolean legendarys;  // Legendarys

  public FilterPokemons() {

    blackList = new PokemonBlackList();

    // Types
    notEvolution = null;
    legendarys = false;
    useChances = false;
    pokemonsChances = AdvancedPokemonChance.defaultChances();
  }

  @Getter
  @Setter
  public static class AdvancedPokemonChance {
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
      // Calculamos el total de la probabilidad usando un for clásico
      double totalChance = 0;
      for (AdvancedPokemonChance pokemonChance : pokemonChances) {
        totalChance += pokemonChance.getChance();
      }

      double randomValue = Utils.getRandom().nextDouble() * totalChance;

      for (AdvancedPokemonChance pokemonChance : pokemonChances) {
        randomValue -= pokemonChance.getChance();
        if (randomValue <= 0) {
          List<String> pokemons = pokemonChance.getPokemons();
          int size = pokemons.size();
          int index = Utils.getRandom().nextInt(size);
          return PokemonProperties.Companion.parse(pokemons.get(index)).create();
        }
      }

      // Fallback en caso de que no se seleccione nada
      return PokemonProperties.Companion.parse("rattata").create();
    }

  }


  private void checker() {
    if (blacklistPokemons != null) {
      Iterator<String> iteratorPokemons = blacklistPokemons.iterator();
      while (iteratorPokemons.hasNext()) {
        String pokemon = iteratorPokemons.next();
        blackList.getPokemons().add(pokemon);
        iteratorPokemons.remove();
      }
    }


    if (blacklistLabels != null) {
      Iterator<String> iteratorLabels = blacklistLabels.iterator();
      while (iteratorLabels.hasNext()) {
        String label = iteratorLabels.next();
        blackList.getLabels().add(label);
        iteratorLabels.remove();
      }
    }
    if (blacklistForms != null) {
      Iterator<String> iteratorForms = blacklistForms.iterator();
      while (iteratorForms.hasNext()) {
        String form = iteratorForms.next();
        blackList.getForms().add(form);
        iteratorForms.remove();
      }
    }

    if (blacklistAspects != null) {
      Iterator<String> iteratorAspects = blacklistAspects.iterator();
      while (iteratorAspects.hasNext()) {
        String aspect = iteratorAspects.next();
        blackList.getAspects().add(aspect);
        iteratorAspects.remove();
      }
    }
    if (blacklistTypes != null) {
      Iterator<ElementalType> iteratorTypes = blacklistTypes.iterator();
      while (iteratorTypes.hasNext()) {
        ElementalType type = iteratorTypes.next();
        blackList.getTypes().add(type.getName().toLowerCase());
        iteratorTypes.remove();
      }
    }

    if (blacklistRarity != null) {
      Iterator<String> iteratorRarity = blacklistRarity.iterator();
      while (iteratorRarity.hasNext()) {
        String rarity = iteratorRarity.next();
        blackList.getRarities().add(rarity);
        iteratorRarity.remove();
      }
    }

    blacklistPokemons = null;
    blacklistForms = null;
    blacklistLabels = null;
    blacklistAspects = null;
    blacklistTypes = null;
    blacklistRarity = null;
    if (notEvolution != null) blackList.setAllowEvolutions(!notEvolution);

    blackList.fix();
  }

  public static void removeCache(String modid) {
    CACHE.remove(modid);
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
      return AdvancedPokemonChance.getPokemons(getPokemonsChances());
    } else {
      List<Pokemon> allowedPokemons;
      if (CACHE.containsKey(modId) && CACHE.get(modId).containsKey(id)) {
        if (CACHE.get(modId).get(id).isEmpty()) {
          allowedPokemons = getAllowedPokemons();
          CACHE.get(modId).put(id, allowedPokemons);
        }
        allowedPokemons = CACHE.get(modId).get(id);
      } else {
        checker();
        allowedPokemons = getAllowedPokemons();
        CACHE.putIfAbsent(modId, new HashMap<>());
        CACHE.get(modId).put(id, allowedPokemons);
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
  private Pokemon clonePokemon(Pokemon pokemon) {
    Pokemon copy = pokemon.clone(true, DynamicRegistryManager.EMPTY);
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
      return clonePokemon(AdvancedPokemonChance.getPokemon(getPokemonsChances()));
    } else {
      List<Pokemon> allowedPokemons = getCachePokemons(modId, id);
      return clonePokemon(allowedPokemons.get(Utils.getRandom().nextInt(allowedPokemons.size())));
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
        pokemons.add(clonePokemon(allowedPokemons.get(Utils.getRandom().nextInt(allowedPokemons.size()))));
      }
      return pokemons;
    }
  }

  /**
   * Obtiene todos los Pokémon disponibles.
   *
   * @return la lista de todos los Pokémon.
   */
  public List<Pokemon> getAllPokemons() {
    List<Pokemon> allPokemons = new ArrayList<>();
    Set<String> uniquePokemonIds = new HashSet<>();
    // 1.7 List<Species> species = new ArrayList<>(PokemonSpecies.getSpecies().stream().toList());
    List<Species> species = new ArrayList<>(PokemonSpecies.getSpecies().stream().toList());
    species.sort(Comparator.comparing(Species::getNationalPokedexNumber));

    species.forEach(pokemon -> {
      List<FormData> forms = pokemon.getForms();
      if (forms.isEmpty()) {
        Pokemon p = pokemon.create(1);
        if (uniquePokemonIds.add(p.getForm().showdownId())) {
          allPokemons.add(p);
        }
      } else {
        forms.forEach(form -> {
          List<String> aspects = form.getAspects();
          if (aspects.isEmpty()) {
            Pokemon p = pokemon.create(1);
            if (uniquePokemonIds.add(p.getForm().showdownId())) {
              allPokemons.add(p);
            }
          } else {
            aspects.forEach(aspect -> {
              String formattedAspect = aspect.replace("-", "_");
              int lastUnderscore = formattedAspect.lastIndexOf("_");
              if (lastUnderscore != -1) {
                formattedAspect = formattedAspect.substring(0, lastUnderscore) + "=" + formattedAspect.substring(lastUnderscore + 1);
              }
              Pokemon p = PokemonProperties.Companion.parse(pokemon.showdownId() + " " + formattedAspect).create();
              if (uniquePokemonIds.add(p.getForm().showdownId())) {
                allPokemons.add(p);
              }
            });
          }
        });
      }
    });
    return allPokemons;
  }

  /**
   * Obtiene todos los Pokémon permitidos.
   *
   * @return la lista de Pokémon permitidos.
   */
  public List<Pokemon> getAllowedPokemons() {
    List<Pokemon> allPokemons = getAllPokemons();
    List<Pokemon> allowedPokemons = new ArrayList<>();

    for (Pokemon pokemon : allPokemons) {
      if (isAllowed(pokemon)) allowedPokemons.add(pokemon);
    }
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
    if (!legendarys && pokemon.isLegendary()) return false;
    return !blackList.isBlackListed(pokemon);
  }

  /**
   * Opens the filter pokemons visual menu.
   *
   * @deprecated Use {@link FilterPokemonsVisualMenu#open(ServerPlayerEntity, FilterPokemons, String, String, Consumer)}
   * or {@link com.kingpixel.cobbleutils.ui.editor.FilterPokemonsEditorMenu#open(ServerPlayerEntity, FilterPokemons, String, String, Consumer, Runnable)}
   */
  @Deprecated
  public void open(ServerPlayerEntity player, String modId, String id, Consumer<ButtonAction> pokemonAction) {
    FilterPokemonsVisualMenu.open(player, this, modId, id, pokemonAction);
  }

  /**
   * Opens the filter pokemons visual menu.
   *
   * @deprecated Use {@link FilterPokemonsVisualMenu#open(ServerPlayerEntity, FilterPokemons, String, String, Consumer, int, boolean, Runnable)}
   */
  @Deprecated
  public void open(ServerPlayerEntity player, String modId, String id, Consumer<ButtonAction> pokemonAction, int pos,
                   boolean showBanneds) {
    FilterPokemonsVisualMenu.open(player, this, modId, id, pokemonAction, pos, showBanneds, null);
  }
}