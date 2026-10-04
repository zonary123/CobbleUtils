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
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.Model.PokemonChance;
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
import java.util.List;
import java.util.function.Consumer;

/**
 * Reusable paginated GUI to manage, add, configure weights, simulate rolls,
 * and remove entries from a {@link PokemonChance} list.
 */
public final class PokemonChanceListEditorMenu {

  private PokemonChanceListEditorMenu() {
  }

  /**
   * Opens the PokemonChance list editor.
   *
   * @param player  the player opening the editor
   * @param initial list of chances to edit
   * @param onSave  callback with the updated list of chances
   * @param onBack  callback to return to previous menu
   */
  public static void open(ServerPlayerEntity player, List<PokemonChance> initial, Consumer<List<PokemonChance>> onSave, Runnable onBack) {
    if (player == null) return;

    List<PokemonChance> working = initial != null ? new ArrayList<>(initial) : new ArrayList<>();

    double totalWeight = 0;
    for (PokemonChance pc : working) {
      totalWeight += pc.getChance();
    }
    final double finalTotal = Math.max(1.0, totalWeight);

    ChestTemplate template = ChestTemplate.builder(6).build();
    template.rectangle(0, 0, 5, 9, new PlaceholderButton());

    List<Button> buttons = new ArrayList<>();

    for (int i = 0; i < working.size(); i++) {
      final int index = i;
      PokemonChance chanceEntry = working.get(i);
      if (chanceEntry == null) continue;

      ItemStack displayStack;
      try {
        Pokemon pokemon = PokemonProperties.Companion.parse(chanceEntry.getPokemon()).create();
        displayStack = PokemonItem.from(pokemon);
      } catch (Exception e) {
        displayStack = new ItemStack(Items.PAPER);
      }

      displayStack.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e" + chanceEntry.getPokemon()));
      double percentage = (chanceEntry.getChance() / finalTotal) * 100.0;

      List<String> lore = new ArrayList<>();
      lore.add("&7Weight: &a" + chanceEntry.getChance());
      lore.add("&7Probability: &b" + String.format("%.2f%%", percentage));
      lore.add("");
      lore.add("&eLeft-Click &7to edit weight.");
      lore.add("&eRight-Click &7to edit Pokémon string.");
      lore.add("&cShift + Right-Click &7to remove.");
      displayStack.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(lore)));

      GooeyButton btn = GooeyButton.builder()
        .display(displayStack)
        .onClick(action -> {
          switch (action.getClickType()) {
            case SHIFT_RIGHT_CLICK, SHIFT_LEFT_CLICK -> {
              working.remove(index);
              player.sendMessage(Text.literal("§c[Editor] Removed chance entry: " + chanceEntry.getPokemon()));
              open(player, working, onSave, onBack);
            }
            case RIGHT_CLICK -> ChatPrompt.prompt(player, "Enter new Pokémon name/properties (current: " + chanceEntry.getPokemon() + "):", val -> {
              if (val != null && !val.trim().isEmpty()) {
                working.set(index, new PokemonChance(val.trim(), chanceEntry.getChance()));
                player.sendMessage(Text.literal("§a[Editor] Updated entry Pokémon to: " + val.trim()));
              }
              open(player, working, onSave, onBack);
            });
            default -> ChatPrompt.prompt(player, "Enter new weight for " + chanceEntry.getPokemon() + " (current: " + chanceEntry.getChance() + "):", val -> {
              if (val != null && !val.trim().isEmpty()) {
                try {
                  int w = Integer.parseInt(val.trim());
                  working.set(index, new PokemonChance(chanceEntry.getPokemon(), Math.max(1, w)));
                  player.sendMessage(Text.literal("§a[Editor] Updated weight to: " + w));
                } catch (NumberFormatException ignored) {
                  player.sendMessage(Text.literal("§c[Editor] Invalid weight number."));
                }
              }
              open(player, working, onSave, onBack);
            });
          }
        })
        .build();
      buttons.add(btn);
    }

    // Previous Page (Slot 45)
    ItemModel itemPrevious = CobbleUtils.language != null ? CobbleUtils.language.getItemPrevious() : null;
    ItemStack prevStack = itemPrevious != null ? itemPrevious.getItemStack() : new ItemStack(Items.ARROW);
    template.set(45, LinkedPageButton.builder()
      .display(prevStack)
      .linkType(LinkType.Previous)
      .build());

    // Simulator Button (Slot 47)
    ItemStack simItem = new ItemStack(Items.DISPENSER);
    simItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&6&lSimulate Roll"));
    List<String> simLore = new ArrayList<>();
    simLore.add("&7Tests random generation from the list.");
    simLore.add("&7Total entries: &e" + working.size());
    simLore.add("&7Total weight: &e" + (int) totalWeight);
    simLore.add("");
    simLore.add("&eClick to roll a Pokémon!");
    simItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(simLore)));

    template.set(47, GooeyButton.builder()
      .display(simItem)
      .onClick(action -> {
        if (working.isEmpty()) {
          player.sendMessage(Text.literal("§c[Simulator] The chance list is empty."));
        } else {
          String rolled = PokemonChance.getPokemon(working);
          player.sendMessage(Text.literal("§6[Simulator] §aRolled outcome: §e" + rolled));
        }
      })
      .build());

    // Add Chance Entry Button (Slot 48)
    ItemStack addItem = new ItemStack(Items.EMERALD);
    addItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&a&l+ Add Pokemon Chance"));
    List<String> addLore = List.of(
      "&7Format: '<pokemon> <chance>'",
      "&7Example: 'pikachu 50' or 'charizard shiny=true 10'"
    );
    addItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(addLore)));

    template.set(48, GooeyButton.builder()
      .display(addItem)
      .onClick(action -> ChatPrompt.prompt(player, "Enter Pokémon and chance (e.g. 'pikachu 50'):", input -> {
        if (input != null && !input.trim().isEmpty()) {
          String trimmed = input.trim();
          int lastSpace = trimmed.lastIndexOf(' ');
          String pokeStr;
          int chanceVal = 100;
          if (lastSpace != -1) {
            String maybeNum = trimmed.substring(lastSpace + 1);
            try {
              chanceVal = Integer.parseInt(maybeNum);
              pokeStr = trimmed.substring(0, lastSpace).trim();
            } catch (NumberFormatException e) {
              pokeStr = trimmed;
            }
          } else {
            pokeStr = trimmed;
          }

          working.add(new PokemonChance(pokeStr, Math.max(1, chanceVal)));
          player.sendMessage(Text.literal("§a[Editor] Added: " + pokeStr + " with weight " + chanceVal));
        }
        open(player, working, onSave, onBack);
      }))
      .build());

    // Cancel / Back Button (Slot 49)
    template.set(49, UIUtils.getCancelButton(action -> {
      if (onBack != null) onBack.run();
      else UIManager.closeUI(action.getPlayer());
    }));

    // Save Button (Slot 51)
    template.set(51, UIUtils.getConfirmButton(action -> {
      if (onSave != null) onSave.accept(working);
      player.sendMessage(Text.literal("§a[Editor] Pokemon chances saved successfully!"));
      if (onBack != null) onBack.run();
      else UIManager.closeUI(action.getPlayer());
    }));

    // Next Page (Slot 53)
    ItemModel itemNext = CobbleUtils.language != null ? CobbleUtils.language.getItemNext() : null;
    ItemStack nextStack = itemNext != null ? itemNext.getItemStack() : new ItemStack(Items.ARROW);
    template.set(53, LinkedPageButton.builder()
      .display(nextStack)
      .linkType(LinkType.Next)
      .build());

    LinkedPage.Builder pageBuilder = LinkedPage.builder()
      .title(AdventureTranslator.toNative("&8Pokemon Chances Editor"));

    CobbleUtils.server.execute(() -> {
      if (buttons.isEmpty()) {
        GooeyPage page = GooeyPage.builder()
          .template(template)
          .title(AdventureTranslator.toNative("&8Pokemon Chances Editor"))
          .build();
        UIManager.openUIForcefully(player, page);
      } else {
        UIManager.openUIForcefully(player, PaginationHelper.createPagesFromPlaceholders(template, buttons, pageBuilder));
      }
    });
  }
}
