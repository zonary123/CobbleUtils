package com.kingpixel.cobbleutils.ui.conditions;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.linked.LinkType;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.Model.Rectangle;
import com.kingpixel.cobbleutils.Model.conditions.Condition;
import com.kingpixel.cobbleutils.adapter.ConditionAdapter;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Reusable GUI menu to view, add, edit, and delete Conditions from any mod without duplicating code.
 */
public final class ConditionListMenu {

  private ConditionListMenu() {
  }

  /**
   * Opens the reusable conditions editor menu.
   *
   * @param player the player opening the menu
   * @param conditions the list of conditions to manage
   * @param parentTitle the context title (e.g. "Category Requirements")
   * @param onSave callback invoked when the user clicks Save & Return
   */
  public static void open(ServerPlayerEntity player, List<Condition> conditions, String parentTitle, Consumer<List<Condition>> onSave) {
    if (player == null) return;
    ChestTemplate template = ChestTemplate.builder(6).build();
    List<Button> buttons = new ArrayList<>();

    if (conditions != null) {
      for (int i = 0; i < conditions.size(); i++) {
        final int index = i;
        Condition condition = conditions.get(i);
        if (condition == null) continue;

        ItemStack item = ConditionEditorRegistry.getIcon(condition);
        item.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("§b§lCondition #" + (index + 1) + " §7(" + condition.getType() + ")"));

        List<String> lore = new ArrayList<>();
        List<String> details = ConditionEditorRegistry.getDetails(condition);
        if (details != null && !details.isEmpty()) {
          lore.addAll(details);
        }

        lore.add("");
        lore.add("§eLeft-click §7to Edit.");
        lore.add("§cRight-click §7to Delete.");

        item.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(lore)));

        GooeyButton btn = GooeyButton.builder()
          .display(item)
          .onClick(action -> {
            switch (action.getClickType()) {
              case RIGHT_CLICK, SHIFT_RIGHT_CLICK -> {
                conditions.remove(index);
                player.sendMessage(Text.literal("§a[Editor] Condition removed."));
                open(player, conditions, parentTitle, onSave);
              }
              default -> ConditionEditorRegistry.openEdit(player, condition, () -> open(player, conditions, parentTitle, onSave));
            }
          })
          .build();

        buttons.add(btn);
      }
    }

    // Add Condition Button (Slot 49)
    ItemStack addIcon = new ItemStack(Items.EMERALD);
    addIcon.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("§a§l[+] Add New Condition"));
    addIcon.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(List.of(
      "§7Add a requirement condition.",
      "§eClick to select condition type."
    ))));

    GooeyButton addBtn = GooeyButton.builder()
      .display(addIcon)
      .onClick(action -> ConditionTypeSelectorMenu.open(player, conditions, parentTitle, onSave))
      .build();
    template.set(49, addBtn);

    // Save & Return Button (Slot 51)
    ItemStack backIcon = new ItemStack(Items.BARRIER);
    backIcon.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("§a◀ Save & Return"));
    GooeyButton backBtn = GooeyButton.builder()
      .display(backIcon)
      .onClick(action -> {
        if (onSave != null) onSave.accept(conditions);
      })
      .build();
    template.set(51, backBtn);

    // Pagination Controls
    ItemModel prevPageModel = new ItemModel(45, "minecraft:arrow", "§e◀ Previous Page", List.of(), 0);
    prevPageModel.applyTemplate(template, prevPageModel.getLinkedPageButton(LinkType.Previous));

    ItemModel nextPageModel = new ItemModel(53, "minecraft:arrow", "§eNext Page ▶", List.of(), 0);
    nextPageModel.applyTemplate(template, nextPageModel.getLinkedPageButton(LinkType.Next));

    Rectangle rectangle = new Rectangle(6);
    rectangle.apply(template);

    LinkedPage page = PaginationHelper.createPagesFromPlaceholders(
      template,
      buttons,
      LinkedPage.builder()
        .title(AdventureTranslator.toNative("§6" + (parentTitle != null ? parentTitle : "Conditions")))
    );

    UIManager.openUIForcefully(player, page);
  }

  /**
   * Submenu allowing the player to select from all registered Condition types.
   */
  private static final class ConditionTypeSelectorMenu {
    public static void open(ServerPlayerEntity player, List<Condition> conditions, String parentTitle, Consumer<List<Condition>> onSave) {
      ChestTemplate template = ChestTemplate.builder(6).build();
      List<Button> buttons = new ArrayList<>();

      Set<String> types = ConditionAdapter.getRegisteredTypes().keySet();

      for (String type : types) {
        ItemStack item = new ItemStack(Items.PAPER);
        item.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("§e§l" + type));
        item.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(List.of(
          "§7Condition Type: §f" + type,
          "",
          "§eClick to add and configure."
        ))));

        GooeyButton btn = GooeyButton.builder()
          .display(item)
          .onClick(action -> {
            try {
              Class<? extends Condition> clazz = ConditionAdapter.getRegisteredTypes().get(type);
              if (clazz != null) {
                Condition newCond = clazz.getDeclaredConstructor().newInstance();
                conditions.add(newCond);
                player.sendMessage(Text.literal("§a[Editor] Added condition: " + type));
                ConditionEditorRegistry.openEdit(player, newCond, () -> ConditionListMenu.open(player, conditions, parentTitle, onSave));
                return;
              }
            } catch (Exception e) {
              player.sendMessage(Text.literal("§c[Editor] Error creating condition: " + e.getMessage()));
            }
            ConditionListMenu.open(player, conditions, parentTitle, onSave);
          })
          .build();

        buttons.add(btn);
      }

      // Previous Page
      ItemModel selectorPrevPage = new ItemModel(45, "minecraft:arrow", "§e◀ Previous Page", List.of(), 0);
      selectorPrevPage.applyTemplate(template, selectorPrevPage.getLinkedPageButton(LinkType.Previous));

      // Next Page
      ItemModel selectorNextPage = new ItemModel(53, "minecraft:arrow", "§eNext Page ▶", List.of(), 0);
      selectorNextPage.applyTemplate(template, selectorNextPage.getLinkedPageButton(LinkType.Next));

      // Back to Condition List (Slot 49)
      ItemStack backIcon = new ItemStack(Items.BARRIER);
      backIcon.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("§c◀ Cancel"));
      GooeyButton backBtn = GooeyButton.builder()
        .display(backIcon)
        .onClick(action -> ConditionListMenu.open(player, conditions, parentTitle, onSave))
        .build();
      template.set(49, backBtn);

      Rectangle rectangle = new Rectangle(6);
      rectangle.apply(template);

      LinkedPage page = PaginationHelper.createPagesFromPlaceholders(
        template,
        buttons,
        LinkedPage.builder()
          .title(AdventureTranslator.toNative("§6Select Condition Type"))
      );

      UIManager.openUIForcefully(player, page);
    }
  }
}
