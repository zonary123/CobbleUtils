package com.kingpixel.cobbleutils.Model;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.ButtonAction;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonPropertyExtractor;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.*;
import lombok.Data;
import lombok.Getter;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Items;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.joml.Vector4f;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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
  private static final Vector4f tintBlack = new Vector4f(0.25f, 0.25f, 0.25f, 1.0f);
  private static final Vector4f tintWhite = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);


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
    return !blackList.isBlacklisted(pokemon);
  }

  /**
   * Opens the filter pokemons
   *
   * @param player the player
   */
  public void open(ServerPlayerEntity player, String modId, String id, Consumer<ButtonAction> pokemonAction) {
    open(player, modId, id, pokemonAction, 0, true);
  }

  public void open(ServerPlayerEntity player, String modId, String id, Consumer<ButtonAction> pokemonAction, int pos,
                   boolean showBanneds) {
    CompletableFuture.runAsync(() -> {
        final int ROWS = 6;
        final int RECTANGLE_SIZE = new Rectangle(ROWS).getTotalSlots();

        ChestTemplate template = ChestTemplate.builder(ROWS).build();
        List<GooeyButton> buttons = new ArrayList<>();

        List<String> lore = new ArrayList<>(CobbleUtils.language.getLorepokemon());
        lore.add("&fLeft Click Blacklist pokemon");
        lore.add("&fRight Click Blacklist labels");
        lore.add("&fShift + Left Click Blacklist forms");
        lore.add("&fShift + Right Click Blacklist aspects");

        //List<Pokemon> pokemons = getCachePokemons(modId, id);
        List<Pokemon> pokemons;
        if (showBanneds) {
          pokemons = getAllPokemons();
        } else {
          pokemons = getAllowedPokemons();
        }
        int totalPokemons = pokemons.size();
        int totalPages = (int) Math.ceil((double) totalPokemons / RECTANGLE_SIZE);
        int currentPage = (int) Math.ceil((double) (pos + 1) / RECTANGLE_SIZE);

        // Validar índices para evitar excepciones
        int startIndex = Math.min(pos, totalPokemons);
        int endIndex = Math.min(pos + RECTANGLE_SIZE, totalPokemons);
        if (startIndex > endIndex) {
          CobbleUtils.LOGGER.error("Invalid indices for pagination: startIndex > endIndex");
          return;
        }
        pokemons = pokemons.subList(startIndex, endIndex);

        // Crear botones para los Pokémon
        for (Pokemon pokemon : pokemons) {
          buttons.add(createPokemonButton(player, modId, id, pokemonAction, pokemon, lore, pos, showBanneds));
        }

        // Aplicar botones al template
        new Rectangle(ROWS).apply(template, buttons);

        // Botón "Anterior"
        if (pos > 0) {
          template.set(45, UIUtils.getPreviousButton(buttonAction -> open(player, modId, id, pokemonAction,
            Math.max(pos - RECTANGLE_SIZE, 0), showBanneds)));
        }

        // Botón "Cerrar"
        template.set(49, UIUtils.getCloseButton(buttonAction -> UIManager.closeUI(buttonAction.getPlayer())));

        template.set(51, GooeyButton
          .builder()
          .display(Items.PAPER.getDefaultStack())
          .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("Show Banned: " + (showBanneds ?
            CobbleUtils.language.getYes() : CobbleUtils.language.getNo())))
          .onClick(action -> {
            open(player, modId, id, pokemonAction, pos, !showBanneds);
          })
          .build()
        );

        // Botón "Siguiente"
        if (totalPokemons > endIndex) {
          template.set(53, UIUtils.getNextButton(buttonAction -> open(player, modId, id, pokemonAction, Math.min(pos + RECTANGLE_SIZE, totalPokemons), showBanneds)));
        }

        // Crear y abrir la página
        var page = GooeyPage.builder()
          .template(template)
          .title(AdventureTranslator.toNative("Filter Pokemons " + currentPage + " of " + totalPages))
          .build();

        UIManager.openUIForcefully(player, page);
      }, CobbleUtils.EXECUTOR_COBBLEUTILS)
      .orTimeout(30, TimeUnit.SECONDS)
      .exceptionally(throwable -> {
        CobbleUtils.LOGGER.error("Error opening filter pokemons -> ");
        throwable.printStackTrace();
        return null;
      });
  }

  // Método auxiliar para crear botones de Pokémon
  private GooeyButton createPokemonButton(ServerPlayerEntity player, String modId,
                                          String id, Consumer<ButtonAction> pokemonAction, Pokemon pokemon,
                                          List<String> lore, int pos, boolean showBanneds) {

    boolean isAllowed = isAllowed(pokemon);
    return GooeyButton.builder()
      .display(PokemonItem.from(pokemon, 1, isAllowed ? tintWhite : tintBlack))
      .with(DataComponentTypes.CUSTOM_NAME,
        AdventureTranslator.toNative(pokemon.showdownId() + (isAllowed ? "" : " &c[BLACKLISTED]")))
      .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(PokemonUtils.replace(lore, pokemon))))
      .onClick(action -> handlePokemonClick(player, modId, id, pokemonAction, pokemon, action, pos, showBanneds))
      .build();
  }

  private void handlePokemonClick(ServerPlayerEntity player, String modId,
                                  String id, Consumer<ButtonAction> pokemonAction,
                                  Pokemon pokemon, ButtonAction action, int pos, boolean showBanneds) {
    boolean update = false;
    List<String> modified = new ArrayList<>();
    switch (action.getClickType()) {
      case LEFT_CLICK -> {
        String showdownId = pokemon.getForm().showdownId();
        if (blackList.getPokemons().contains(showdownId)) {
          blackList.getPokemons().remove(showdownId);
          modified.add("&cRemoved Blacklist showdownId: " + showdownId);
        } else {
          blackList.getPokemons().add(showdownId);
          modified.add("&aAdded Blacklist showdownId: " + showdownId);
        }
        update = true;
      }
      case RIGHT_CLICK -> {
        for (String label : pokemon.getForm().getLabels()) {
          if (blackList.getLabels().contains(label)) {
            blackList.getLabels().remove(label);
            modified.add("&cRemoved Blacklist labels: " + label);
          } else {
            blackList.getLabels().add(label);
            modified.add("&aAdded Blacklist labels: " + label);
          }
        }
        update = true;
      }
      case SHIFT_LEFT_CLICK -> {
        String formOnlyShowdownId = pokemon.getForm().formOnlyShowdownId();
        if (!formOnlyShowdownId.equals("normal")) {
          if (blackList.getForms().contains(formOnlyShowdownId)) {
            blackList.getForms().remove(formOnlyShowdownId);
            modified.add("&cRemoved Blacklist form: " + formOnlyShowdownId);
          } else {
            blackList.getForms().add(formOnlyShowdownId);
            modified.add("&aAdded Blacklist form: " + formOnlyShowdownId);
          }
          update = true;
        }
      }
      case SHIFT_RIGHT_CLICK -> {
        var aspects = new ArrayList<>(pokemon.getAspects());
        aspects.removeAll(List.of("male", "female", "shiny", "genderless"));
        for (String aspect : aspects) {
          if (blackList.getAspects().contains(aspect)) {
            blackList.getAspects().remove(aspect);
            modified.add("&cRemoved Blacklist Aspect: " + aspect);
          } else {
            blackList.getAspects().add(aspect);
            modified.add("&aAdded  Blacklist Aspect: " + aspect);
          }
        }
        update = true;
      }
    }
    if (update) {
      PlayerUtils.sendMessage(player, "%prefix% " + String.join(", ", modified), modId, TypeMessage.CHAT);
      pokemonAction.accept(action);
      open(player, modId, id, pokemonAction, pos, showBanneds);
    }
  }

}