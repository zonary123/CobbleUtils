package com.kingpixel.cobbleutils.Model;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.ButtonAction;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.Template;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.action.PokemonButtonAction;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import lombok.Data;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Carlos Varas Alonso - 05/04/2025 21:01
 */
@Data
public class PartyPcMenu {
  // Party
  private String titleParty;
  private int rowsParty;
  private Integer[] slotsParty;
  private ItemModel pc;
  private ItemModel closeParty;
  private List<PanelsConfig> panelsParty;
  // PC
  private String titlePc;
  private int rowsPc;
  private Rectangle rectanglePc;
  private ItemModel previousPc;
  private ItemModel closePc;
  private ItemModel nextPc;
  private List<PanelsConfig> panelsPc;

  public PartyPcMenu() {
    // Party
    this.titleParty = "&bParty";
    this.rowsParty = 3;
    this.slotsParty = new Integer[]{10, 11, 12, 14, 15, 16};
    this.pc = new ItemModel("cobblemon:pc", "PC");
    this.pc.setSlot(13);
    this.closeParty = new ItemModel("minecraft:barrier", "&cClose");
    closeParty.setSlot(22);
    this.panelsParty = new ArrayList<>();
    this.panelsParty.add(new PanelsConfig(new ItemModel("minecraft:light_blue_stained_glass_pane"), rowsParty));
    // Pc
    this.titlePc = "&bPC";
    this.rowsPc = 6;
    this.rectanglePc = new Rectangle(rowsParty);
    rectanglePc.setStartRow(1);
    rectanglePc.setStartColumn(1);
    rectanglePc.setWidth(7);
    this.previousPc = new ItemModel("minecraft:arrow", "&aPrevious");
    previousPc.setSlot(45);
    this.closePc = new ItemModel("minecraft:barrier", "&cClose");
    closePc.setSlot(49);
    this.nextPc = new ItemModel("minecraft:arrow", "&aNext");
    nextPc.setSlot(53);
    this.panelsPc = new ArrayList<>();
    this.panelsPc.add(new PanelsConfig(new ItemModel("minecraft:light_blue_stained_glass_pane"), rowsPc));
  }

  private void openPc(ServerPlayerEntity player,
                      Consumer<PokemonButtonAction> pokemonAction, int pos, Consumer<ButtonAction> closeAction) {
    ChestTemplate template = ChestTemplate
      .builder(rowsPc)
      .build();

    PanelsConfig.applyConfig(template, panelsPc);

    var pc = Cobblemon.INSTANCE.getStorage().getPC(player);
    List<Pokemon> pokemons = new ArrayList<>();
    for (Pokemon pokemon : pc) {
      if (pokemon != null) {
        pokemons.add(pokemon);
      }
    }
    int maxSize = pokemons.size();
    int start = pos;
    int end = Math.min(pos + rectanglePc.getSlotsFree(rowsPc), maxSize);

    Rectangle rectangle = rectanglePc;
    int slots = rowsPc * 9;
    int slotIndex = 0;

    for (int row = rectangle.getStartRow(); row < rectangle.getStartRow() + rectangle.getLength(); row++) {
      for (int column = rectangle.getStartColumn(); column < rectangle.getStartColumn() + rectangle.getWidth(); column++) {
        if (slotIndex - 2 >= end - start || start + slotIndex >= maxSize) break;
        Pokemon pokemon = pokemons.get(start + slotIndex);
        GooeyButton.Builder button;
        if (pokemon == null) {
          button = GooeyButton.builder()
            .display(CobbleUtils.language.getItemNoPokemon().getItemStack());
        } else {
          button = GooeyButton.builder()
            .display(PokemonItem.from(pokemon))
            .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNativeComponent(PokemonUtils.replace(pokemon)))
            .with(DataComponentTypes.LORE,
              new LoreComponent(AdventureTranslator.toNativeL(PokemonUtils.replaceLore(pokemon))));
        }
        button.onClick(action -> {
          pokemonAction.accept(new PokemonButtonAction(action, pokemon));
        });

        int slot = row * 9 + column;
        template.set(slot, button.build());
        slotIndex++;
      }
    }

    if (pos > 0) {
      previousPc.applyTemplate(template, previousPc.getButton(action -> {
        openPc(player, pokemonAction, Math.max(0, pos - rectangle.getWidth() * rectangle.getLength()), closeAction);
      }));
    }

    closePc.applyTemplate(template, closePc.getButton(closeAction));

    if (end < maxSize - 2) {
      nextPc.applyTemplate(template, nextPc.getButton(action -> {
        openPc(player, pokemonAction, Math.min(maxSize, pos + rectangle.getWidth() * rectangle.getLength()), closeAction);
      }));
    }

    GooeyPage page = GooeyPage.builder()
      .title(AdventureTranslator.toNative(titlePc))
      .template(template)
      .build();

    UIManager.openUIForcefully(player, page);
  }

  public void openParty(ServerPlayerEntity player, Consumer<Template> templateConsumer,
                        Consumer<PokemonButtonAction> pokemonAction, Consumer<ButtonAction> closeActionParty) {
    ChestTemplate template = ChestTemplate
      .builder(rowsParty)
      .build();

    PanelsConfig.applyConfig(template, panelsParty);
    if (templateConsumer != null) {
      templateConsumer.accept(template);
    }

    var party = Cobblemon.INSTANCE.getStorage().getParty(player);

    for (int i = 0; i < slotsParty.length; i++) {
      int slot = slotsParty[i];
      Pokemon pokemon = party.get(i);
      GooeyButton.Builder button;
      if (pokemon == null) {
        button = GooeyButton.builder()
          .display(CobbleUtils.language.getItemNoPokemon().getItemStack());
      } else {
        button = GooeyButton.builder()
          .display(PokemonItem.from(pokemon))
          .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNativeComponent(PokemonUtils.replace(pokemon)))
          .with(DataComponentTypes.LORE,
            new LoreComponent(AdventureTranslator.toNativeL(PokemonUtils.replaceLore(pokemon))));
      }
      template.set(slot, button.build());
    }

    closeParty.applyTemplate(template, closeParty.getButton(closeActionParty));

    pc.applyTemplate(template, pc.getButton(action -> {
      openPc(player, pokemonAction, 0, closePc -> openParty(player, templateConsumer, pokemonAction, closeActionParty));
    }));

    GooeyPage page = GooeyPage.builder()
      .title(AdventureTranslator.toNative(titleParty))
      .template(template)
      .build();

    UIManager.openUIForcefully(player, page);
  }
}
