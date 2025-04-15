package com.kingpixel.cobbleutils.ui;

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
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.Model.PanelsConfig;
import com.kingpixel.cobbleutils.Model.PokemonBlackList;
import com.kingpixel.cobbleutils.Model.Rectangle;
import com.kingpixel.cobbleutils.action.PokemonButtonAction;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import lombok.Data;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Data
public class PartyPcMenu {
  private String titleParty;
  private int rowsParty;
  private Integer[] slotsParty;
  private ItemModel pc;
  private ItemModel closeParty;
  private List<PanelsConfig> panelsParty;

  private String titlePc;
  private int rowsPc;
  private Rectangle rectanglePc;
  private ItemModel previousPc;
  private ItemModel closePc;
  private ItemModel nextPc;
  private List<PanelsConfig> panelsPc;

  public PartyPcMenu() {
    this.titleParty = "&bParty";
    this.rowsParty = 3;
    this.slotsParty = new Integer[]{10, 11, 12, 14, 15, 16};
    this.pc = new ItemModel("cobblemon:pc", "PC");
    this.pc.setSlot(13);
    this.closeParty = new ItemModel("minecraft:barrier", "&cClose");
    closeParty.setSlot(22);
    this.panelsParty = new ArrayList<>();
    this.panelsParty.add(new PanelsConfig(new ItemModel("minecraft:light_blue_stained_glass_pane"), rowsParty));
    this.titlePc = "&bPC";
    this.rowsPc = 6;
    this.rectanglePc = new Rectangle(rowsPc);
    rectanglePc.setStartRow(1);
    rectanglePc.setStartColumn(1);
    rectanglePc.setWidth(7);
    rectanglePc.setLength(4);

    this.previousPc = new ItemModel("minecraft:arrow", "&aPrevious");
    previousPc.setSlot(45);
    this.closePc = new ItemModel("minecraft:barrier", "&cClose");
    closePc.setSlot(49);
    this.nextPc = new ItemModel("minecraft:arrow", "&aNext");
    nextPc.setSlot(53);
    this.panelsPc = new ArrayList<>();
    this.panelsPc.add(new PanelsConfig(new ItemModel("minecraft:light_blue_stained_glass_pane"), rowsPc));
  }

  private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
  private static final long COOLDOWN_TIME_MS = 250;

  private boolean isOnCooldown(ServerPlayerEntity player) {
    UUID playerId = player.getUuid();
    long currentTime = System.currentTimeMillis();
    Long lastExecutionTime = cooldowns.get(playerId);

    if (lastExecutionTime != null && (currentTime - lastExecutionTime) < COOLDOWN_TIME_MS) return true;

    cooldowns.put(playerId, currentTime);
    return false;
  }

  private GooeyButton.Builder createPokemonButton(Pokemon pokemon, List<String> lorePokemon,
                                                  BiConsumer<Pokemon, List<String>> loreModifier,
                                                  Consumer<PokemonButtonAction> pokemonAction,
                                                  PokemonBlackList blackList, ConfirmMenu confirmMenu, Consumer<ButtonAction> closeAction) {

    if (pokemon == null || (blackList != null && blackList.isBlackListed(pokemon))) {
      return GooeyButton.builder()
        .display(CobbleUtils.language.getItemNoPokemon().getItemStack());
    }

    List<String> lore;
    if (lorePokemon != null) {
      lore = new ArrayList<>(PokemonUtils.replace(lorePokemon, pokemon));
      if (loreModifier != null) {
        loreModifier.accept(pokemon, lore);
      }
    } else {
      lore = PokemonUtils.replaceLore(pokemon);
    }

    ItemStack itemStack = PokemonItem.from(pokemon);
    itemStack.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(PokemonUtils.replace(pokemon)));
    itemStack.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(lore)));
    return GooeyButton.builder()
      .display(itemStack)
      .onClick(action -> {
        if (confirmMenu != null) {
          confirmMenu.open(action.getPlayer(), itemStack, confirmAction -> {
            // Acción final al confirmar
            pokemonAction.accept(new PokemonButtonAction(action, pokemon));
          }, closeAction);
        } else {
          pokemonAction.accept(new PokemonButtonAction(action, pokemon));
        }
      });
  }

  private void applyPaginationButtons(ChestTemplate template, int pos, int maxSize, Rectangle rectangle,
                                      ServerPlayerEntity player, Consumer<PokemonButtonAction> pokemonAction,
                                      Consumer<ButtonAction> closeAction, PokemonBlackList blackList,
                                      List<String> lorePokemon, BiConsumer<Pokemon, List<String>> loreModifier,
                                      ConfirmMenu confirmMenu) {
    if (pos > 0) {
      previousPc.applyTemplate(template, previousPc.getButton(action -> {
        openPc(player, pokemonAction, Math.max(0, pos - rectangle.getWidth() * rectangle.getLength()),
          closeAction, blackList, lorePokemon, loreModifier, confirmMenu);
      }));
    }

    closePc.applyTemplate(template, closePc.getButton(closeAction));

    if (pos + rectangle.getWidth() * rectangle.getLength() < maxSize) {
      nextPc.applyTemplate(template, nextPc.getButton(action -> {
        openPc(player, pokemonAction, Math.min(maxSize, pos + rectangle.getWidth() * rectangle.getLength()),
          closeAction, blackList, lorePokemon, loreModifier, confirmMenu);
      }));
    }
  }

  public void openPc(ServerPlayerEntity player, Consumer<PokemonButtonAction> pokemonAction, int pos,
                     Consumer<ButtonAction> closeAction, PokemonBlackList blackList, List<String> lorePokemon,
                     BiConsumer<Pokemon, List<String>> loreModifier, ConfirmMenu confirmMenu) {
    if (isOnCooldown(player)) {
      if (CobbleUtils.config.isDebug())
        CobbleUtils.LOGGER.warn("Player " + player.getName().getString() + " is on cooldown for opening PC menu.");
      return;
    }

    CompletableFuture.runAsync(() -> {
      ChestTemplate template = ChestTemplate.builder(rowsPc).build();
      PanelsConfig.applyConfig(template, panelsPc);

      var pc = Cobblemon.INSTANCE.getStorage().getPC(player);
      List<Pokemon> pokemons = new ArrayList<>();
      for (Pokemon pokemon : pc) {
        if (pokemon != null && (blackList == null || !blackList.isBlackListed(pokemon))) {
          pokemons.add(pokemon);
        }
      }

      int maxSize = pokemons.size();
      if (maxSize == 0) return;

      Rectangle rectangle = rectanglePc;
      int index = 0;
      int currentIndex;
      for (int row = rectangle.getStartRow(); row < rectangle.getLength() + rectangle.getStartRow(); row++) {
        for (int column = rectangle.getStartColumn(); column < rectangle.getWidth() + rectangle.getStartColumn(); column++) {
          currentIndex = pos + index;
          if (currentIndex >= maxSize) break;
          Pokemon pokemon = pokemons.get(currentIndex);
          GooeyButton.Builder button = createPokemonButton(pokemon, lorePokemon, loreModifier, pokemonAction,
            blackList, confirmMenu, closeAction);
          template.set(row, column, button.build());
          index++;
        }
      }

      applyPaginationButtons(template, pos, maxSize, rectangle, player, pokemonAction, closeAction, blackList, lorePokemon, loreModifier, confirmMenu);

      GooeyPage page = GooeyPage.builder()
        .title(AdventureTranslator.toNative(titlePc))
        .template(template)
        .build();

      UIManager.openUIForcefully(player, page);
    }).orTimeout(5, TimeUnit.SECONDS).exceptionally(e -> {
      CobbleUtils.LOGGER.error("Error while opening PC menu: " + e);
      return null;
    });
  }

  public void openParty(ServerPlayerEntity player, Consumer<Template> templateConsumer,
                        Consumer<PokemonButtonAction> pokemonAction, Consumer<ButtonAction> closeActionParty,
                        PokemonBlackList blackList, List<String> lorePokemon,
                        BiConsumer<Pokemon, List<String>> loreModifier, ConfirmMenu confirmMenu) {
    if (isOnCooldown(player)) {
      if (CobbleUtils.config.isDebug())
        CobbleUtils.LOGGER.warn("Player " + player.getName().getString() + " is on cooldown for opening Party menu.");
      return;
    }

    CompletableFuture.runAsync(() -> {
      ChestTemplate template = ChestTemplate.builder(rowsParty).build();
      PanelsConfig.applyConfig(template, panelsParty);

      if (templateConsumer != null) {
        templateConsumer.accept(template);
      }

      var party = Cobblemon.INSTANCE.getStorage().getParty(player);

      for (int i = 0; i < slotsParty.length; i++) {
        int slot = slotsParty[i];
        Pokemon pokemon = party.get(i);
        GooeyButton.Builder button = createPokemonButton(pokemon, lorePokemon, loreModifier, pokemonAction, blackList, confirmMenu, closeActionParty);
        template.set(slot, button.build());
      }

      closeParty.applyTemplate(template, closeParty.getButton(closeActionParty));

      pc.applyTemplate(template, pc.getButton(action -> {
        openPc(player, pokemonAction, 0, closePc -> openParty(player, templateConsumer, pokemonAction, closeActionParty,
          blackList, lorePokemon, loreModifier, confirmMenu), blackList, lorePokemon, loreModifier, confirmMenu);
      }));

      GooeyPage page = GooeyPage.builder()
        .title(AdventureTranslator.toNative(titleParty))
        .template(template)
        .build();

      UIManager.openUIForcefully(player, page);
    }).orTimeout(5, TimeUnit.SECONDS).exceptionally(e -> {
      CobbleUtils.LOGGER.error("Error while opening Party menu: " + e);
      return null;
    });
  }
}