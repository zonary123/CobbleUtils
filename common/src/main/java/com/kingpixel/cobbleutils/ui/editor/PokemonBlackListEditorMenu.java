package com.kingpixel.cobbleutils.ui.editor;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobbleutils.CobbleUtils;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Reusable Hub GUI to configure any {@link PokemonBlackList}.
 * Provides direct navigation to edit species, types, forms, aspects,
 * labels, rarities, properties, and restrictive toggles.
 */
public final class PokemonBlackListEditorMenu {

  private PokemonBlackListEditorMenu() {
  }

  /**
   * Opens the blacklist configuration menu.
   *
   * @param player   the player opening the menu
   * @param initial  the blacklist instance to edit
   * @param onSave   callback invoked when saving changes
   * @param onBack   callback invoked when navigating back
   */
  public static void open(ServerPlayerEntity player, PokemonBlackList initial, Consumer<PokemonBlackList> onSave, Runnable onBack) {
    if (player == null) return;

    PokemonBlackList working = initial != null ? initial : new PokemonBlackList();

    ChestTemplate template = ChestTemplate.builder(4).build();

    // 1. Species / Showdown IDs (Slot 10)
    int pokemonCount = working.getPokemons() != null ? working.getPokemons().size() : 0;
    ItemStack speciesItem = new ItemStack(Items.DRAGON_EGG);
    speciesItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e&lSpecies & IDs"));
    List<String> speciesLore = new ArrayList<>();
    speciesLore.add("&7Blocked Species: &b" + pokemonCount);
    speciesLore.add("");
    speciesLore.add("&eClick to edit species list.");
    speciesItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(speciesLore)));

    template.set(10, GooeyButton.builder()
      .display(speciesItem)
      .onClick(action -> StringSetEditorMenu.open(
        player,
        "Blacklist Species",
        working.getPokemons(),
        "Enter Pokémon showdownId to blacklist (e.g. pikachu, ditto):",
        updatedSet -> working.setPokemons(updatedSet),
        () -> open(player, working, onSave, onBack)
      ))
      .build());

    // 2. Elemental Types (Slot 11)
    int typesCount = working.getTypes() != null ? working.getTypes().size() : 0;
    ItemStack typesItem = new ItemStack(Items.WATER_BUCKET);
    typesItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e&lElemental Types"));
    List<String> typesLore = new ArrayList<>();
    typesLore.add("&7Blocked Types: &b" + typesCount);
    typesLore.add("");
    typesLore.add("&eClick to toggle types visually.");
    typesItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(typesLore)));

    template.set(11, GooeyButton.builder()
      .display(typesItem)
      .onClick(action -> PokemonTypesSelectorMenu.open(
        player,
        working.getTypes(),
        updatedSet -> working.setTypes(updatedSet),
        () -> open(player, working, onSave, onBack)
      ))
      .build());

    // 3. Forms (Slot 12)
    int formsCount = working.getForms() != null ? working.getForms().size() : 0;
    ItemStack formsItem = new ItemStack(Items.ANVIL);
    formsItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e&lForms"));
    List<String> formsLore = new ArrayList<>();
    formsLore.add("&7Blocked Forms: &b" + formsCount);
    formsLore.add("");
    formsLore.add("&eClick to edit blocked forms.");
    formsItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(formsLore)));

    template.set(12, GooeyButton.builder()
      .display(formsItem)
      .onClick(action -> StringSetEditorMenu.open(
        player,
        "Blacklist Forms",
        working.getForms(),
        "Enter form showdownId to blacklist (e.g. alolan, galarian, hisuian):",
        updatedSet -> working.setForms(updatedSet),
        () -> open(player, working, onSave, onBack)
      ))
      .build());

    // 4. Cosmetic Aspects (Slot 13)
    int aspectsCount = working.getAspects() != null ? working.getAspects().size() : 0;
    ItemStack aspectsItem = new ItemStack(Items.AMETHYST_SHARD);
    aspectsItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e&lCosmetic Aspects"));
    List<String> aspectsLore = new ArrayList<>();
    aspectsLore.add("&7Blocked Aspects: &b" + aspectsCount);
    aspectsLore.add("");
    aspectsLore.add("&eClick to edit aspects.");
    aspectsItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(aspectsLore)));

    template.set(13, GooeyButton.builder()
      .display(aspectsItem)
      .onClick(action -> StringSetEditorMenu.open(
        player,
        "Blacklist Aspects",
        working.getAspects(),
        "Enter aspect to blacklist (e.g. gmax, alpha, totem):",
        updatedSet -> working.setAspects(updatedSet),
        () -> open(player, working, onSave, onBack)
      ))
      .build());

    // 5. Cobblemon Labels (Slot 14)
    int labelsCount = working.getLabels() != null ? working.getLabels().size() : 0;
    ItemStack labelsItem = new ItemStack(Items.NAME_TAG);
    labelsItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e&lLabels"));
    List<String> labelsLore = new ArrayList<>();
    labelsLore.add("&7Blocked Labels: &b" + labelsCount);
    labelsLore.add("");
    labelsLore.add("&eClick to edit labels (e.g. legendary, mythical, ultra_beast).");
    labelsItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(labelsLore)));

    template.set(14, GooeyButton.builder()
      .display(labelsItem)
      .onClick(action -> StringSetEditorMenu.open(
        player,
        "Blacklist Labels",
        working.getLabels(),
        "Enter label to blacklist (e.g. legendary, mythical, ultra_beast, paradox):",
        updatedSet -> working.setLabels(updatedSet),
        () -> open(player, working, onSave, onBack)
      ))
      .build());

    // 6. Rarities (Slot 15)
    int raritiesCount = working.getRarities() != null ? working.getRarities().size() : 0;
    ItemStack raritiesItem = new ItemStack(Items.NETHER_STAR);
    raritiesItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e&lRarities"));
    List<String> raritiesLore = new ArrayList<>();
    raritiesLore.add("&7Blocked Rarities: &b" + raritiesCount);
    raritiesLore.add("");
    raritiesLore.add("&eClick to edit rarities.");
    raritiesItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(raritiesLore)));

    template.set(15, GooeyButton.builder()
      .display(raritiesItem)
      .onClick(action -> StringSetEditorMenu.open(
        player,
        "Blacklist Rarities",
        working.getRarities(),
        "Enter rarity to blacklist (e.g. common, rare, epic, legendary):",
        updatedSet -> working.setRarities(updatedSet),
        () -> open(player, working, onSave, onBack)
      ))
      .build());

    // 7. Cobblemon Property Strings (Slot 16)
    int propsCount = working.getProperties() != null ? working.getProperties().size() : 0;
    ItemStack propsItem = new ItemStack(Items.ENCHANTED_BOOK);
    propsItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e&lProperties"));
    List<String> propsLore = new ArrayList<>();
    propsLore.add("&7Blocked Properties: &b" + propsCount);
    propsLore.add("");
    propsLore.add("&eClick to edit Cobblemon properties.");
    propsItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(propsLore)));

    template.set(16, GooeyButton.builder()
      .display(propsItem)
      .onClick(action -> StringSetEditorMenu.open(
        player,
        "Blacklist Properties",
        working.getProperties(),
        "Enter property string to blacklist (e.g. shiny=true, gender=female):",
        updatedSet -> working.setProperties(updatedSet),
        () -> open(player, working, onSave, onBack)
      ))
      .build());

    // 8. Toggle: Allow Evolutions (Slot 19)
    boolean allowEvolutions = working.isAllowEvolutions();
    ItemStack evolutionsItem = new ItemStack(allowEvolutions ? Items.LIME_DYE : Items.RED_DYE);
    evolutionsItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e&lAllow Evolutions"));
    List<String> evoLore = new ArrayList<>();
    evoLore.add("&7Allow evolved forms: " + (allowEvolutions ? "&aYes" : "&cNo (Base only)"));
    evoLore.add("");
    evoLore.add("&eClick to toggle.");
    evolutionsItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(evoLore)));

    template.set(19, GooeyButton.builder()
      .display(evolutionsItem)
      .onClick(action -> {
        working.setAllowEvolutions(!allowEvolutions);
        open(player, working, onSave, onBack);
      })
      .build());

    // 9. Toggle: Only Implemented (Slot 21)
    boolean onlyImplemented = working.isOnlyImplemented();
    ItemStack implItem = new ItemStack(onlyImplemented ? Items.LIME_DYE : Items.RED_DYE);
    implItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e&lOnly Implemented"));
    List<String> implLore = new ArrayList<>();
    implLore.add("&7Restrict to fully implemented species: " + (onlyImplemented ? "&aYes" : "&cNo"));
    implLore.add("");
    implLore.add("&eClick to toggle.");
    implItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(implLore)));

    template.set(21, GooeyButton.builder()
      .display(implItem)
      .onClick(action -> {
        working.setOnlyImplemented(!onlyImplemented);
        open(player, working, onSave, onBack);
      })
      .build());

    // 10. Toggle Wildcard All '*' (Slot 23)
    boolean hasWildcard = working.getPokemons() != null && working.getPokemons().contains("*");
    ItemStack wildItem = new ItemStack(hasWildcard ? Items.BARRIER : Items.STRUCTURE_VOID);
    wildItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&c&lBlock All Wildcard (*)"));
    List<String> wildLore = new ArrayList<>();
    wildLore.add("&7Blocks EVERYTHING if enabled: " + (hasWildcard ? "&cEnabled (*)" : "&aDisabled"));
    wildLore.add("");
    wildLore.add("&eClick to toggle.");
    wildItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(wildLore)));

    template.set(23, GooeyButton.builder()
      .display(wildItem)
      .onClick(action -> {
        Set<String> set = working.getPokemons() != null ? new HashSet<>(working.getPokemons()) : new HashSet<>();
        if (hasWildcard) {
          set.remove("*");
        } else {
          set.add("*");
        }
        working.setPokemons(set);
        open(player, working, onSave, onBack);
      })
      .build());

    // 11. Clear Filter (Slot 25)
    ItemStack clearItem = new ItemStack(Items.LAVA_BUCKET);
    clearItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&c&lClear Blacklist"));
    clearItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(List.of(
      "&7Resets all blacklist categories to empty."
    ))));

    template.set(25, GooeyButton.builder()
      .display(clearItem)
      .onClick(action -> {
        working.clear();
        player.sendMessage(Text.literal("§e[Editor] Blacklist cleared."));
        open(player, working, onSave, onBack);
      })
      .build());

    // 12. Cancel / Back (Slot 27)
    template.set(27, UIUtils.getCancelButton(action -> {
      if (onBack != null) onBack.run();
      else UIManager.closeUI(action.getPlayer());
    }));

    // 13. Save & Return (Slot 35)
    template.set(35, UIUtils.getConfirmButton(action -> {
      working.fix();
      if (onSave != null) onSave.accept(working);
      player.sendMessage(Text.literal("§a[Editor] Blacklist saved successfully!"));
      if (onBack != null) onBack.run();
      else UIManager.closeUI(action.getPlayer());
    }));

    GooeyPage page = GooeyPage.builder()
      .template(template)
      .title(AdventureTranslator.toNative("&8Pokemon Blacklist Editor"))
      .build();

    CobbleUtils.server.execute(() -> UIManager.openUIForcefully(player, page));
  }
}
