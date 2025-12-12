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
import com.kingpixel.cobbleutils.ui.builds.PartyPcMenuBuilder;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Data
public class PartyPcMenu {
  private String titleParty;
  private int rowsParty;
  private Integer[] slotsParty;
  private Long[] customModelDataParty;
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
    this.customModelDataParty = new Long[]{0L, 0L, 0L, 0L, 0L, 0L};
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

  public static PartyPcMenuBuilder builder() {
    return new PartyPcMenuBuilder();
  }

  /**
   * Opens the default party menu if useDefault is enabled.
   * This is the recommended way to open party menus when using CobbleUtils as an API.
   *
   * @param builder The PartyPcMenuBuilder with the configuration
   * @return true if the default menu was used, false if custom implementation should be used
   */
  public static boolean openDefaultParty(PartyPcMenuBuilder builder) {
    if (!CobbleUtils.config.isUseDefault()) {
      return false;
    }
    // Force the use of default menu
    builder.setPartyPcMenu(CobbleUtils.language.getPartyPcMenu());
    if (builder.getConfirmMenu() == null) {
      builder.setConfirmMenu(CobbleUtils.language.getConfirmMenu());
    }
    CobbleUtils.language.getPartyPcMenu().openParty(builder);
    return true;
  }

  /**
   * Opens the default PC menu if useDefault is enabled.
   * This is the recommended way to open PC menus when using CobbleUtils as an API.
   *
   * @param builder The PartyPcMenuBuilder with the configuration
   * @param pos The starting position in the PC
   * @return true if the default menu was used, false if custom implementation should be used
   */
  public static boolean openDefaultPc(PartyPcMenuBuilder builder, int pos) {
    if (!CobbleUtils.config.isUseDefault()) {
      return false;
    }
    // Force the use of default menu
    builder.setPartyPcMenu(CobbleUtils.language.getPartyPcMenu());
    if (builder.getConfirmMenu() == null) {
      builder.setConfirmMenu(CobbleUtils.language.getConfirmMenu());
    }
    CobbleUtils.language.getPartyPcMenu().openPc(builder, pos);
    return true;
  }

  private boolean isBanned(Pokemon pokemon, PartyPcMenuBuilder builder) {
    boolean isBanned = false;
    if (pokemon == null) return true;
    if (builder.getCustomFilter() != null) isBanned = builder.getCustomFilter().test(pokemon);
    if (isBanned) return true;
    if (builder.getBlackList() != null) isBanned = builder.getBlackList().isBlacklisted(pokemon);
    return isBanned;
  }

  public void openPc(PartyPcMenuBuilder builder, int pos) {
    openPc(builder, pos, null);
  }

  public void openPc(PartyPcMenuBuilder builder, int pos, List<Pokemon> pokemons) {
    if (isOnCooldown(builder.getPlayer())) {
      if (CobbleUtils.config.isDebug())
        CobbleUtils.LOGGER.warn("Player " + builder.getPlayer().getName().getString() + " is on cooldown for opening PC menu.");
      return;
    }
    // Si no se pasa la lista, la generamos como antes
    if (pokemons == null) {
      pokemons = new ArrayList<>();
      var pc = Cobblemon.INSTANCE.getStorage().getPC(builder.getPlayer());
      for (Pokemon pokemon : pc) {
        if (pokemon != null && !isBanned(pokemon, builder)) {
          pokemons.add(pokemon);
        }
      }
    }
    int maxSize = pokemons.size();
    if (maxSize == 0) return;
    List<Pokemon> finalPokemons = pokemons;
    CompletableFuture.runAsync(() -> {
        long startTime = System.currentTimeMillis();
        ChestTemplate template = ChestTemplate.builder(rowsPc).build();
        PanelsConfig.applyConfig(template, panelsPc);


        Rectangle rectangle = rectanglePc;
        int index = 0;
        int currentIndex;
        for (int row = rectangle.getStartRow(); row < rectangle.getLength() + rectangle.getStartRow(); row++) {
          for (int column = rectangle.getStartColumn(); column < rectangle.getWidth() + rectangle.getStartColumn(); column++) {
            currentIndex = pos + index;
            if (currentIndex >= maxSize) break;
            Pokemon pokemon = finalPokemons.get(currentIndex);
            GooeyButton.Builder button = createPokemonButton(pokemon, builder, -1);
            template.set(row, column, button.build());
            index++;
          }
        }

        applyPaginationButtons(template, pos, maxSize, rectangle, builder, finalPokemons);

        GooeyPage page = GooeyPage.builder()
          .title(AdventureTranslator.toNative(titlePc))
          .template(template)
          .build();

        UIManager.openUIForcefully(builder.getPlayer(), page);

        long endTime = System.currentTimeMillis();
        if (CobbleUtils.config.isDebug()) {
          CobbleUtils.LOGGER.info("Time taken to open PC menu: " + (endTime - startTime) + "ms");
        }
      }, CobbleUtils.EXECUTOR_COBBLEUTILS)
      .exceptionally(e -> {
        e.printStackTrace();
        return null;
      });
  }


  public void openParty(PartyPcMenuBuilder builder) {
    CompletableFuture.runAsync(() -> {
        if (isOnCooldown(builder.getPlayer())) {
          if (CobbleUtils.config.isDebug())
            CobbleUtils.LOGGER.warn("Player " + builder.getPlayer().getName().getString() + " is on cooldown for opening Party menu.");
          return;
        }
        long startTime = System.currentTimeMillis();
        ChestTemplate template = ChestTemplate
          .builder(rowsParty)
          .build();

        PanelsConfig.applyConfig(template, panelsParty);

        if (builder.getTemplateConsumer() != null) {
          builder.getTemplateConsumer().accept(template);
        }

        var party = Cobblemon.INSTANCE.getStorage().getParty(builder.getPlayer());

        for (int i = 0; i < slotsParty.length; i++) {
          int slot = slotsParty[i];
          Pokemon pokemon = party.get(i);
          GooeyButton.Builder button = createPokemonButton(pokemon, builder, i);
          template.set(slot, button.build());
        }

        closeParty.applyTemplate(template, closeParty.getButton(close -> UIManager.closeUI(close.getPlayer())));

        pc.applyTemplate(template, pc.getButton(action -> {
          openPc(builder, 0);
        }));

        GooeyPage page = GooeyPage.builder()
          .title(AdventureTranslator.toNative(titleParty))
          .template(template)
          .build();

        UIManager.openUIForcefully(builder.getPlayer(), page);
        long endTime = System.currentTimeMillis();
        if (CobbleUtils.config.isDebug()) {
          CobbleUtils.LOGGER.info("Time taken to open Party menu: " + (endTime - startTime) + "ms");
        }
      }, CobbleUtils.EXECUTOR_COBBLEUTILS)
      .exceptionally(e -> {
        e.printStackTrace();
        return null;
      });
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

  private GooeyButton.Builder createPokemonButton(Pokemon pokemon, PartyPcMenuBuilder builder, int slotIndex) {
    var blackList = builder.getBlackList();
    var pokemonAction = builder.getPokemonAction();
    var closeAction = builder.getCloseAction();
    var confirmMenu = builder.getConfirmMenu();
    var lorePokemon = builder.getLorePokemon();
    var loreModifier = builder.getLoreModifier();

    if (pokemon == null || isBanned(pokemon, builder)) {
      ItemModel noPokemonItem = new ItemModel(CobbleUtils.language.getItemNoPokemon());
      if (slotIndex >= 0 && slotIndex < customModelDataParty.length && customModelDataParty[slotIndex] != 0) {
        noPokemonItem.setCustomModelData(customModelDataParty[slotIndex]);
      }
      return GooeyButton.builder()
        .display(noPokemonItem.getItemStack());
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

    ItemStack itemStack;
    if (builder.getItemStackProvider() != null) {
      itemStack = builder.getItemStackProvider().apply(pokemon);
    } else {
      itemStack = PokemonItem.from(pokemon);
    }
    if (slotIndex >= 0 && slotIndex < customModelDataParty.length && customModelDataParty[slotIndex] != 0) {
      itemStack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new net.minecraft.component.type.CustomModelDataComponent(customModelDataParty[slotIndex].intValue()));
    }
    itemStack.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(PokemonUtils.replace(pokemon)));
    itemStack.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(lore)));
    return GooeyButton.builder()
      .display(itemStack)
      .onClick(action -> {
        if (confirmMenu != null) {
          confirmMenu.open(action.getPlayer(), itemStack, confirmAction -> {
            pokemonAction.accept(new PokemonButtonAction(action, pokemon));
          }, close -> {
            if (closeAction == null) {
              UIManager.closeUI(close.getPlayer());
            } else {
              closeAction.accept(close);
            }
          });
        } else {
          pokemonAction.accept(new PokemonButtonAction(action, pokemon));
        }
      });
  }

  private void applyPaginationButtons(ChestTemplate template, int pos, int maxSize, Rectangle rectangle,
                                      PartyPcMenuBuilder builder, List<Pokemon> pokemons) {
    if (pos > 0) {
      previousPc.applyTemplate(template, previousPc.getButton(action -> {
        openPc(builder, Math.max(0, pos - rectangle.getWidth() * rectangle.getLength()), pokemons);
      }));
    }

    closePc.applyTemplate(template, closePc.getButton(action -> {
      openParty(builder);
    }));

    if (pos + rectangle.getWidth() * rectangle.getLength() < maxSize) {
      nextPc.applyTemplate(template, nextPc.getButton(action -> {
        openPc(builder, Math.min(maxSize, pos + rectangle.getWidth() * rectangle.getLength()), pokemons);
      }));
    }
  }

  public void openParty(ServerPlayerEntity player, Consumer<Template> templateConsumer,
                        Consumer<PokemonButtonAction> pokemonAction, Consumer<ButtonAction> closeActionConfirmMenu,
                        PokemonBlackList blackList, List<String> lorePokemon,
                        BiConsumer<Pokemon, List<String>> loreModifier, ConfirmMenu confirmMenu) {
    var builder = PartyPcMenu.builder()
      .setPlayer(player)
      .setTemplateConsumer(templateConsumer)
      .setPokemonAction(pokemonAction)
      .setBlackList(blackList)
      .setLorePokemon(lorePokemon)
      .setLoreModifier(loreModifier)
      .setConfirmMenu(confirmMenu)
      .setCloseAction(closeActionConfirmMenu)
      .build();
    openParty(builder);
  }


}