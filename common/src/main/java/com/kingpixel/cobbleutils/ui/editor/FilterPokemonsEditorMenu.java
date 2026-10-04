package com.kingpixel.cobbleutils.ui.editor;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.FilterPokemons;
import com.kingpixel.cobbleutils.Model.PokemonBlackList;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.UIUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Main Hub GUI to configure any {@link FilterPokemons} instance.
 * Links to blacklist settings, interactive visual Pokedex filter,
 * mode toggles, and generation tests.
 */
public final class FilterPokemonsEditorMenu {

  private FilterPokemonsEditorMenu() {
  }

  /**
   * Opens the FilterPokemons configuration hub.
   *
   * @param player  the player opening the editor
   * @param initial the filter instance to edit
   * @param modId   context modId for caching and messages
   * @param id      context identifier
   * @param onSave  callback when saving the filter
   * @param onBack  callback to return to previous menu
   */
  public static void open(ServerPlayerEntity player, FilterPokemons initial, String modId, String id,
                          Consumer<FilterPokemons> onSave, Runnable onBack) {
    if (player == null) return;

    FilterPokemons working = initial != null ? initial : new FilterPokemons();
    if (working.getBlackList() == null) {
      working.setBlackList(new PokemonBlackList());
    }

    ChestTemplate template = ChestTemplate.builder(4).build();

    // 1. Mode Toggle: Blacklist vs Chances (Slot 10)
    boolean useChances = working.isUseChances();
    ItemStack modeItem = new ItemStack(useChances ? Items.GOLD_INGOT : Items.SHIELD);
    modeItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e&lGeneration Mode"));
    List<String> modeLore = new ArrayList<>();
    modeLore.add("&7Current: " + (useChances ? "&6Weighted Chances" : "&bFilter & Blacklist"));
    modeLore.add("");
    modeLore.add("&eClick to toggle mode.");
    modeItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(modeLore)));

    template.set(10, GooeyButton.builder()
      .display(modeItem)
      .onClick(action -> {
        working.setUseChances(!useChances);
        open(player, working, modId, id, onSave, onBack);
      })
      .build());

    // 2. Blacklist Configuration Hub (Slot 12)
    ItemStack blacklistHubItem = new ItemStack(Items.DRAGON_EGG);
    blacklistHubItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e&lBlacklist Settings"));
    List<String> blLore = new ArrayList<>();
    blLore.add("&7Configure blocked species, types, forms,");
    blLore.add("&7labels, rarities, and properties.");
    blLore.add("");
    blLore.add("&eClick to open Blacklist Editor.");
    blacklistHubItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(blLore)));

    template.set(12, GooeyButton.builder()
      .display(blacklistHubItem)
      .onClick(action -> PokemonBlackListEditorMenu.open(
        player,
        working.getBlackList(),
        updatedBl -> working.setBlackList(updatedBl),
        () -> open(player, working, modId, id, onSave, onBack)
      ))
      .build());

    // 3. Visual Pokedex Grid (Slot 14)
    ItemStack visualGridItem = new ItemStack(Items.FILLED_MAP);
    visualGridItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e&lVisual Pokédex Filter"));
    List<String> visualLore = new ArrayList<>();
    visualLore.add("&7Interactive visual grid of all Pokémon.");
    visualLore.add("&7Click Pokémon to toggle blacklist rules directly.");
    visualLore.add("");
    visualLore.add("&eClick to browse visually.");
    visualGridItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(visualLore)));

    template.set(14, GooeyButton.builder()
      .display(visualGridItem)
      .onClick(action -> FilterPokemonsVisualMenu.open(
        player,
        working,
        modId,
        id,
        null,
        0,
        true,
        () -> open(player, working, modId, id, onSave, onBack)
      ))
      .build());

    // 4. Toggle: Allow Legendaries (Slot 16)
    boolean legendarys = working.isLegendarys();
    ItemStack legItem = new ItemStack(legendarys ? Items.NETHER_STAR : Items.FIREWORK_STAR);
    legItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e&lAllow Legendaries"));
    List<String> legLore = new ArrayList<>();
    legLore.add("&7Status: " + (legendarys ? "&aAllowed" : "&cBlocked"));
    legLore.add("");
    legLore.add("&eClick to toggle.");
    legItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(legLore)));

    template.set(16, GooeyButton.builder()
      .display(legItem)
      .onClick(action -> {
        working.setLegendarys(!legendarys);
        open(player, working, modId, id, onSave, onBack);
      })
      .build());

    // 5. Test Random Generation (Slot 22)
    ItemStack testItem = new ItemStack(Items.DISPENSER);
    testItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&6&lTest Random Generation"));
    List<String> testLore = new ArrayList<>();
    testLore.add("&7Simulates random generation using this filter.");
    testLore.add("");
    testLore.add("&eClick to test generate a Pokémon.");
    testItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(testLore)));

    template.set(22, GooeyButton.builder()
      .display(testItem)
      .onClick(action -> {
        try {
          Pokemon generated = working.generateRandomPokemon(modId, id);
          if (generated != null) {
            player.sendMessage(Text.literal("§6[Filter Test] §aGenerated: §e" +
              generated.getDisplayName(false).getString() + " §7(" + generated.showdownId() + ")"));
          } else {
            player.sendMessage(Text.literal("§c[Filter Test] Generation returned null."));
          }
        } catch (Exception e) {
          player.sendMessage(Text.literal("§c[Filter Test] Error generating: " + e.getMessage()));
        }
      })
      .build());

    // 6. Cancel / Back (Slot 27)
    template.set(27, UIUtils.getCancelButton(action -> {
      if (onBack != null) onBack.run();
      else UIManager.closeUI(action.getPlayer());
    }));

    // 7. Save & Confirm (Slot 35)
    template.set(35, UIUtils.getConfirmButton(action -> {
      if (onSave != null) onSave.accept(working);
      player.sendMessage(Text.literal("§a[Editor] Filter saved successfully!"));
      if (onBack != null) onBack.run();
      else UIManager.closeUI(action.getPlayer());
    }));

    GooeyPage page = GooeyPage.builder()
      .template(template)
      .title(AdventureTranslator.toNative("&8Filter Pokemons Editor"))
      .build();

    CobbleUtils.server.execute(() -> UIManager.openUIForcefully(player, page));
  }
}
