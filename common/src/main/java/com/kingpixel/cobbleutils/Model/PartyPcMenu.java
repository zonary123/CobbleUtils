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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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

  @Deprecated(forRemoval = true)
  private void openPc(ServerPlayerEntity player,
                      Consumer<PokemonButtonAction> pokemonAction, int pos, Consumer<ButtonAction> closeAction) {
    openPc(player, pokemonAction, pos, closeAction, null);
  }

  private void openPc(ServerPlayerEntity player,
                      Consumer<PokemonButtonAction> pokemonAction, int pos, Consumer<ButtonAction> closeAction,
                      PokemonBlackList blackList) {
    if (isOnCooldown(player)) {
      if (CobbleUtils.config.isDebug())
        CobbleUtils.LOGGER.warn("Player " + player.getName().getString() + " is on cooldown for opening PC menu.");
      return;
    }
    CompletableFuture.runAsync(() -> {
        ChestTemplate template = ChestTemplate
          .builder(rowsPc)
          .build();

        PanelsConfig.applyConfig(template, panelsPc);

        var pc = Cobblemon.INSTANCE.getStorage().getPC(player);
        List<Pokemon> pokemons = new ArrayList<>();
        for (Pokemon pokemon : pc) {
          if (pokemon != null) {
            if (blackList == null) {
              pokemons.add(pokemon);
            } else {
              if (!blackList.isBlackListed(pokemon)) pokemons.add(pokemon);
            }
          }
        }
        int maxSize = pokemons.size();
        if (maxSize == 0) return;
        int start = pos;
        int slotsRectangle = rectanglePc.getWidth() * rectanglePc.getLength();
        int end = Math.min(pos + slotsRectangle, maxSize);

        Rectangle rectangle = rectanglePc;
        int index = 0;

        for (int row = rectangle.getStartRow(); row < rectangle.getLength() + rectangle.getStartRow(); row++) {
          for (int column = rectangle.getStartColumn(); column < rectangle.getWidth() + rectangle.getStartColumn(); column++) {
            int currentIndex = start + index;
            if (currentIndex >= maxSize) break;
            Pokemon pokemon = pokemons.get(currentIndex);
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

            template.set(row, column, button.build());
            index++;
          }
        }

        if (pos > 0) {
          previousPc.applyTemplate(template, previousPc.getButton(action -> {
            openPc(player, pokemonAction, Math.max(0, pos - rectangle.getWidth() * rectangle.getLength()), closeAction, blackList);
          }));
        }

        closePc.applyTemplate(template, closePc.getButton(closeAction));

        if (end < maxSize) {
          nextPc.applyTemplate(template, nextPc.getButton(action -> {
            openPc(player, pokemonAction, Math.min(maxSize, pos + rectangle.getWidth() * rectangle.getLength()),
              closeAction, blackList);
          }));
        }

        GooeyPage page = GooeyPage.builder()
          .title(AdventureTranslator.toNative(titlePc))
          .template(template)
          .build();

        UIManager.openUIForcefully(player, page);
      })
      .orTimeout(5, TimeUnit.SECONDS)
      .exceptionally(e -> {
        if (e instanceof java.util.concurrent.TimeoutException) {
          CobbleUtils.LOGGER.error("Task timed out while opening PC menu.");
        } else {
          CobbleUtils.LOGGER.error("Error while opening PC menu. " + e);
        }
        return null;
      });
  }

  @Deprecated(forRemoval = true)
  public void openParty(ServerPlayerEntity player, Consumer<Template> templateConsumer,
                        Consumer<PokemonButtonAction> pokemonAction, Consumer<ButtonAction> closeActionParty) {
    openParty(player, templateConsumer, pokemonAction, closeActionParty, null);
  }

  public void openParty(ServerPlayerEntity player, Consumer<Template> templateConsumer,
                        Consumer<PokemonButtonAction> pokemonAction, Consumer<ButtonAction> closeActionParty,
                        PokemonBlackList blackList) {
    if (isOnCooldown(player)) {
      if (CobbleUtils.config.isDebug())
        CobbleUtils.LOGGER.warn("Player " + player.getName().getString() + " is on cooldown for opening PC menu.");
      return;
    }
    CompletableFuture.runAsync(() -> {
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
            if (blackList == null) {
              button = GooeyButton.builder()
                .display(PokemonItem.from(pokemon))
                .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNativeComponent(PokemonUtils.replace(pokemon)))
                .with(DataComponentTypes.LORE,
                  new LoreComponent(AdventureTranslator.toNativeL(PokemonUtils.replaceLore(pokemon))))
                .onClick(action -> {
                  pokemonAction.accept(new PokemonButtonAction(action, pokemon));
                });
            } else {
              if (blackList.isBlackListed(pokemon)) {
                button = GooeyButton.builder()
                  .display(CobbleUtils.language.getItemNoPokemon().getItemStack());
              } else {
                button = GooeyButton.builder()
                  .display(PokemonItem.from(pokemon))
                  .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNativeComponent(PokemonUtils.replace(pokemon)))
                  .with(DataComponentTypes.LORE,
                    new LoreComponent(AdventureTranslator.toNativeL(PokemonUtils.replaceLore(pokemon))))
                  .onClick(action -> {
                    pokemonAction.accept(new PokemonButtonAction(action, pokemon));
                  });
              }
            }

          }
          template.set(slot, button.build());
        }

        closeParty.applyTemplate(template, closeParty.getButton(closeActionParty));

        pc.applyTemplate(template, pc.getButton(action -> {
          openPc(player, pokemonAction, 0, closePc -> openParty(player, templateConsumer, pokemonAction, closeActionParty
            , blackList), blackList);
        }));

        GooeyPage page = GooeyPage.builder()
          .title(AdventureTranslator.toNative(titleParty))
          .template(template)
          .build();

        UIManager.openUIForcefully(player, page);
      })
      .orTimeout(5, TimeUnit.SECONDS)
      .exceptionally(e -> {
        if (e instanceof java.util.concurrent.TimeoutException) {
          CobbleUtils.LOGGER.error("Task timed out while opening PC menu.");
        } else {
          CobbleUtils.LOGGER.error("Error while opening PC menu. " + e);
        }
        return null;
      });
  }
}
