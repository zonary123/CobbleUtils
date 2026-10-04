package com.kingpixel.cobbleutils.ui.editor;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.UIUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Item;
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
 * Visual selector menu for Cobblemon elemental types.
 */
public final class PokemonTypesSelectorMenu {

  private PokemonTypesSelectorMenu() {
  }

  /**
   * Opens the elemental types toggle menu.
   *
   * @param player       the player
   * @param blockedTypes the currently blocked type identifiers
   * @param onSave       callback with the updated set of blocked types
   * @param onBack       callback to return to the parent blacklist editor
   */
  public static void open(ServerPlayerEntity player, Set<String> blockedTypes, Consumer<Set<String>> onSave, Runnable onBack) {
    if (player == null) return;

    Set<String> workingTypes = blockedTypes != null ? new HashSet<>(blockedTypes) : new HashSet<>();

    ChestTemplate template = ChestTemplate.builder(4).build();
    List<ElementalType> allTypes = new ArrayList<>();
    ElementalTypes.all().forEach(allTypes::add);

    int slot = 0;
    for (ElementalType type : allTypes) {
      if (slot >= 27) break;
      String typeId = type.getName().toLowerCase();
      boolean isBlocked = workingTypes.contains(typeId);

      ItemStack icon = new ItemStack(getTypeItem(typeId));
      icon.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative((isBlocked ? "&c" : "&a") + type.getDisplayName().getString()));

      List<String> lore = new ArrayList<>();
      lore.add("&7Status: " + (isBlocked ? "&c[BLACKLISTED]" : "&a[ALLOWED]"));
      lore.add("");
      lore.add("&eClick to toggle.");
      icon.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(lore)));

      template.set(slot++, GooeyButton.builder()
        .display(icon)
        .onClick(action -> {
          if (isBlocked) {
            workingTypes.remove(typeId);
            player.sendMessage(Text.literal("§a[Editor] Allowed type: " + typeId));
          } else {
            workingTypes.add(typeId);
            player.sendMessage(Text.literal("§c[Editor] Blacklisted type: " + typeId));
          }
          open(player, workingTypes, onSave, onBack);
        })
        .build());
    }

    // Back / Cancel (Slot 27)
    template.set(27, UIUtils.getCancelButton(action -> {
      if (onBack != null) onBack.run();
      else UIManager.closeUI(action.getPlayer());
    }));

    // Toggle All (Slot 31)
    ItemStack toggleAllItem = new ItemStack(Items.LEVER);
    toggleAllItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e&lToggle All Types"));
    template.set(31, GooeyButton.builder()
      .display(toggleAllItem)
      .onClick(action -> {
        if (workingTypes.size() >= allTypes.size()) {
          workingTypes.clear();
          player.sendMessage(Text.literal("§a[Editor] Cleared all type blacklists."));
        } else {
          allTypes.forEach(t -> workingTypes.add(t.getName().toLowerCase()));
          player.sendMessage(Text.literal("§c[Editor] Blacklisted all types."));
        }
        open(player, workingTypes, onSave, onBack);
      })
      .build());

    // Save & Return (Slot 35)
    template.set(35, UIUtils.getConfirmButton(action -> {
      if (onSave != null) onSave.accept(workingTypes);
      player.sendMessage(Text.literal("§a[Editor] Type filters saved."));
      if (onBack != null) onBack.run();
      else UIManager.closeUI(action.getPlayer());
    }));

    GooeyPage page = GooeyPage.builder()
      .template(template)
      .title(AdventureTranslator.toNative("&8Blacklist Types"))
      .build();

    CobbleUtils.server.execute(() -> UIManager.openUIForcefully(player, page));
  }

  private static Item getTypeItem(String typeName) {
    return switch (typeName.toLowerCase()) {
      case "fire" -> Items.FIRE_CHARGE;
      case "water" -> Items.WATER_BUCKET;
      case "grass" -> Items.OAK_LEAVES;
      case "electric" -> Items.LIGHTNING_ROD;
      case "ice" -> Items.ICE;
      case "fighting" -> Items.IRON_SWORD;
      case "poison" -> Items.SPIDER_EYE;
      case "ground" -> Items.DIRT;
      case "flying" -> Items.FEATHER;
      case "psychic" -> Items.ENDER_EYE;
      case "bug" -> Items.STRING;
      case "rock" -> Items.COBBLESTONE;
      case "ghost" -> Items.SOUL_SAND;
      case "dragon" -> Items.DRAGON_BREATH;
      case "steel" -> Items.IRON_INGOT;
      case "dark" -> Items.OBSIDIAN;
      case "fairy" -> Items.ALLIUM;
      default -> Items.PAPER;
    };
  }
}
