package com.kingpixel.cobbleutils.ui.editor;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.PlaceholderButton;
import ca.landonjw.gooeylibs2.api.button.linked.LinkType;
import ca.landonjw.gooeylibs2.api.button.linked.LinkedPageButton;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.ChatPrompt;
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
 * Reusable paginated GUI to manage, add, and remove entries from any string set.
 */
public final class StringSetEditorMenu {

  private StringSetEditorMenu() {
  }

  /**
   * Opens the string set editor menu.
   *
   * @param player      the player opening the editor
   * @param menuTitle   title displayed in the menu header
   * @param initialSet  the initial set of strings (cloned internally to prevent unintended state mutation)
   * @param addPrompt   prompt instructions shown in chat when adding new items
   * @param onSave      callback invoked when saving the modified set
   * @param onBack      callback invoked when navigating back
   */
  public static void open(ServerPlayerEntity player, String menuTitle, Set<String> initialSet, String addPrompt,
                          Consumer<Set<String>> onSave, Runnable onBack) {
    if (player == null) return;

    Set<String> workingSet = initialSet != null ? new HashSet<>(initialSet) : new HashSet<>();

    ChestTemplate template = ChestTemplate.builder(6).build();
    template.rectangle(0, 0, 5, 9, new PlaceholderButton());

    List<Button> buttons = new ArrayList<>();
    List<String> sortedList = new ArrayList<>(workingSet);
    sortedList.sort(String.CASE_INSENSITIVE_ORDER);

    for (String entry : sortedList) {
      ItemStack item = new ItemStack(Items.PAPER);
      item.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e" + entry));
      List<String> lore = new ArrayList<>();
      lore.add("&7Value: &f" + entry);
      lore.add("");
      lore.add("&cClick to remove this entry.");
      item.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(lore)));

      GooeyButton button = GooeyButton.builder()
        .display(item)
        .onClick(action -> {
          workingSet.remove(entry);
          player.sendMessage(Text.literal("§c[Editor] Removed: " + entry));
          open(player, menuTitle, workingSet, addPrompt, onSave, onBack);
        })
        .build();
      buttons.add(button);
    }

    // Previous Page Button (Slot 45)
    ItemModel itemPrevious = CobbleUtils.language != null ? CobbleUtils.language.getItemPrevious() : null;
    ItemStack prevStack = itemPrevious != null ? itemPrevious.getItemStack() : new ItemStack(Items.ARROW);
    template.set(45, LinkedPageButton.builder()
      .display(prevStack)
      .linkType(LinkType.Previous)
      .build());

    // Add Entry Button (Slot 48)
    ItemStack addItem = new ItemStack(Items.EMERALD);
    addItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&a&l+ Add Entry"));
    List<String> addLore = new ArrayList<>();
    addLore.add("&7Click to add one or more entries via chat.");
    addLore.add("&7(Separate multiple entries with commas)");
    addItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(addLore)));

    template.set(48, GooeyButton.builder()
      .display(addItem)
      .onClick(action -> {
        String msg = addPrompt != null && !addPrompt.isBlank() ? addPrompt : "Enter entry to add (comma-separated for multiple):";
        ChatPrompt.prompt(player, msg, input -> {
          if (input != null && !input.trim().isEmpty()) {
            String[] parts = input.split(",");
            int addedCount = 0;
            for (String part : parts) {
              String clean = part.trim().toLowerCase();
              if (!clean.isEmpty()) {
                if (workingSet.add(clean)) {
                  addedCount++;
                }
              }
            }
            player.sendMessage(Text.literal("§a[Editor] Added " + addedCount + " entry(ies)."));
          }
          open(player, menuTitle, workingSet, addPrompt, onSave, onBack);
        });
      })
      .build());

    // Clear All Button (Slot 50)
    ItemStack clearItem = new ItemStack(Items.LAVA_BUCKET);
    clearItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&c&lClear All"));
    List<String> clearLore = List.of("&7Click to remove all entries from this set.");
    clearItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(clearLore)));

    template.set(50, GooeyButton.builder()
      .display(clearItem)
      .onClick(action -> {
        workingSet.clear();
        player.sendMessage(Text.literal("§e[Editor] Cleared all entries."));
        open(player, menuTitle, workingSet, addPrompt, onSave, onBack);
      })
      .build());

    // Back / Save Button (Slot 49)
    template.set(49, UIUtils.getConfirmButton(action -> {
      if (onSave != null) {
        onSave.accept(workingSet);
      }
      player.sendMessage(Text.literal("§a[Editor] Changes saved."));
      if (onBack != null) {
        onBack.run();
      } else {
        UIManager.closeUI(action.getPlayer());
      }
    }));

    // Next Page Button (Slot 53)
    ItemModel itemNext = CobbleUtils.language != null ? CobbleUtils.language.getItemNext() : null;
    ItemStack nextStack = itemNext != null ? itemNext.getItemStack() : new ItemStack(Items.ARROW);
    template.set(53, LinkedPageButton.builder()
      .display(nextStack)
      .linkType(LinkType.Next)
      .build());

    LinkedPage.Builder pageBuilder = LinkedPage.builder()
      .title(AdventureTranslator.toNative(menuTitle != null ? menuTitle : "Edit List"));

    CobbleUtils.server.execute(() -> {
      if (buttons.isEmpty()) {
        GooeyPage page = GooeyPage.builder()
          .template(template)
          .title(AdventureTranslator.toNative(menuTitle != null ? menuTitle : "Edit List"))
          .build();
        UIManager.openUIForcefully(player, page);
      } else {
        UIManager.openUIForcefully(player, PaginationHelper.createPagesFromPlaceholders(template, buttons, pageBuilder));
      }
    });
  }
}
