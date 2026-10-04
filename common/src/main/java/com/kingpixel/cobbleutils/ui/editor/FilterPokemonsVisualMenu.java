package com.kingpixel.cobbleutils.ui.editor;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.ButtonAction;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.FilterPokemons;
import com.kingpixel.cobbleutils.Model.Rectangle;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.kingpixel.cobbleutils.util.UIUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Visual Pokedex browser and blacklist toggle grid extracted from {@link FilterPokemons}.
 */
public final class FilterPokemonsVisualMenu {

  private static final Vector4f TINT_BLACK = new Vector4f(0.25f, 0.25f, 0.25f, 1.0f);
  private static final Vector4f TINT_WHITE = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);

  private FilterPokemonsVisualMenu() {
  }

  public static void open(ServerPlayerEntity player, FilterPokemons filter, String modId, String id,
                          Consumer<ButtonAction> pokemonAction) {
    open(player, filter, modId, id, pokemonAction, 0, true, null);
  }

  public static void open(ServerPlayerEntity player, FilterPokemons filter, String modId, String id,
                          Consumer<ButtonAction> pokemonAction, int pos, boolean showBanneds, Runnable onBack) {
    if (player == null || filter == null) return;

    CobbleUtils.runAsync(() -> {
      final int ROWS = 6;
      final int RECTANGLE_SIZE = new Rectangle(ROWS).getTotalSlots();

      ChestTemplate template = ChestTemplate.builder(ROWS).build();
      List<GooeyButton> buttons = new ArrayList<>();

      List<String> lore = new ArrayList<>(CobbleUtils.language != null && CobbleUtils.language.getLorepokemon() != null
        ? CobbleUtils.language.getLorepokemon() : List.of());
      lore.add("&fLeft Click Blacklist pokemon");
      lore.add("&fRight Click Blacklist labels");
      lore.add("&fShift + Left Click Blacklist forms");
      lore.add("&fShift + Right Click Blacklist aspects");

      List<Pokemon> pokemons = showBanneds ? filter.getAllPokemons() : filter.getAllowedPokemons();
      int totalPokemons = pokemons.size();
      int totalPages = (int) Math.ceil((double) totalPokemons / RECTANGLE_SIZE);
      int currentPage = (int) Math.ceil((double) (pos + 1) / RECTANGLE_SIZE);

      int startIndex = Math.min(pos, totalPokemons);
      int endIndex = Math.min(pos + RECTANGLE_SIZE, totalPokemons);
      if (startIndex > endIndex) {
        CobbleUtils.LOGGER_RAW.error("Invalid indices for pagination: startIndex > endIndex");
        return;
      }
      pokemons = pokemons.subList(startIndex, endIndex);

      for (Pokemon pokemon : pokemons) {
        buttons.add(createPokemonButton(player, filter, modId, id, pokemonAction, pokemon, lore, pos, showBanneds, onBack));
      }

      new Rectangle(ROWS).apply(template, buttons);

      // Previous button (Slot 45)
      if (pos > 0) {
        template.set(45, UIUtils.getPreviousButton(buttonAction -> open(player, filter, modId, id, pokemonAction,
          Math.max(pos - RECTANGLE_SIZE, 0), showBanneds, onBack)));
      }

      // Close / Back button (Slot 49)
      template.set(49, UIUtils.getCloseButton(buttonAction -> {
        if (onBack != null) {
          onBack.run();
        } else {
          UIManager.closeUI(buttonAction.getPlayer());
        }
      }));

      // Show Banned toggle (Slot 51)
      template.set(51, GooeyButton.builder()
        .display(Items.PAPER.getDefaultStack())
        .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("Show Banned: " + (showBanneds ?
          CobbleUtils.language.getYes() : CobbleUtils.language.getNo())))
        .onClick(action -> open(player, filter, modId, id, pokemonAction, pos, !showBanneds, onBack))
        .build());

      // Next button (Slot 53)
      if (totalPokemons > endIndex) {
        template.set(53, UIUtils.getNextButton(buttonAction -> open(player, filter, modId, id, pokemonAction,
          Math.min(pos + RECTANGLE_SIZE, totalPokemons), showBanneds, onBack)));
      }

      var page = GooeyPage.builder()
        .template(template)
        .title(AdventureTranslator.toNative("Filter Pokemons " + currentPage + " of " + totalPages))
        .build();

      CobbleUtils.server.execute(() -> UIManager.openUIForcefully(player, page));
    });
  }

  private static GooeyButton createPokemonButton(ServerPlayerEntity player, FilterPokemons filter, String modId,
                                                 String id, Consumer<ButtonAction> pokemonAction, Pokemon pokemon,
                                                 List<String> lore, int pos, boolean showBanneds, Runnable onBack) {
    boolean isAllowed = filter.getAllowedPokemons().contains(pokemon);
    return GooeyButton.builder()
      .display(PokemonItem.from(pokemon, 1, isAllowed ? TINT_WHITE : TINT_BLACK))
      .with(DataComponentTypes.CUSTOM_NAME,
        AdventureTranslator.toNative(pokemon.showdownId() + (isAllowed ? "" : " &c[BLACKLISTED]")))
      .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(PokemonUtils.replace(lore, pokemon))))
      .onClick(action -> handlePokemonClick(player, filter, modId, id, pokemonAction, pokemon, action, pos, showBanneds, onBack))
      .build();
  }

  private static void handlePokemonClick(ServerPlayerEntity player, FilterPokemons filter, String modId,
                                         String id, Consumer<ButtonAction> pokemonAction,
                                         Pokemon pokemon, ButtonAction action, int pos, boolean showBanneds, Runnable onBack) {
    boolean update = false;
    List<String> modified = new ArrayList<>();
    switch (action.getClickType()) {
      case LEFT_CLICK -> {
        String showdownId = pokemon.getForm().showdownId();
        if (filter.getBlackList().getPokemons().contains(showdownId)) {
          filter.getBlackList().getPokemons().remove(showdownId);
          modified.add("&cRemoved Blacklist showdownId: " + showdownId);
        } else {
          filter.getBlackList().getPokemons().add(showdownId);
          modified.add("&aAdded Blacklist showdownId: " + showdownId);
        }
        update = true;
      }
      case RIGHT_CLICK -> {
        for (String label : pokemon.getForm().getLabels()) {
          if (filter.getBlackList().getLabels().contains(label)) {
            filter.getBlackList().getLabels().remove(label);
            modified.add("&cRemoved Blacklist labels: " + label);
          } else {
            filter.getBlackList().getLabels().add(label);
            modified.add("&aAdded Blacklist labels: " + label);
          }
        }
        update = true;
      }
      case SHIFT_LEFT_CLICK -> {
        String formOnlyShowdownId = pokemon.getForm().formOnlyShowdownId();
        if (!formOnlyShowdownId.equals("normal")) {
          if (filter.getBlackList().getForms().contains(formOnlyShowdownId)) {
            filter.getBlackList().getForms().remove(formOnlyShowdownId);
            modified.add("&cRemoved Blacklist form: " + formOnlyShowdownId);
          } else {
            filter.getBlackList().getForms().add(formOnlyShowdownId);
            modified.add("&aAdded Blacklist form: " + formOnlyShowdownId);
          }
          update = true;
        }
      }
      case SHIFT_RIGHT_CLICK -> {
        var aspects = new ArrayList<>(pokemon.getAspects());
        aspects.removeAll(List.of("male", "female", "shiny", "genderless"));
        for (String aspect : aspects) {
          if (filter.getBlackList().getAspects().contains(aspect)) {
            filter.getBlackList().getAspects().remove(aspect);
            modified.add("&cRemoved Blacklist Aspect: " + aspect);
          } else {
            filter.getBlackList().getAspects().add(aspect);
            modified.add("&aAdded  Blacklist Aspect: " + aspect);
          }
        }
        update = true;
      }
      default -> {
        // No action for other clicks
      }
    }
    if (update) {
      PlayerUtils.sendMessage(player, "%prefix% " + String.join(", ", modified), modId, TypeMessage.CHAT);
      if (pokemonAction != null) pokemonAction.accept(action);
      open(player, filter, modId, id, pokemonAction, pos, showBanneds, onBack);
    }
  }
}
