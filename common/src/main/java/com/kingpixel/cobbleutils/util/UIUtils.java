package com.kingpixel.cobbleutils.util;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.ButtonAction;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.PlaceholderButton;
import ca.landonjw.gooeylibs2.api.button.linked.LinkType;
import ca.landonjw.gooeylibs2.api.button.linked.LinkedPageButton;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.action.PokemonButtonAction;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author Carlos Varas Alonso - 28/06/2024 10:31
 */
public class UIUtils {
  /**
   * Create a button with the pokemon data
   *
   * @param pokemon       The pokemon to get the data
   * @param actionpokemon The action to do when the button is clicked
   *
   * @return The button with the pokemon data
   */
  public static GooeyButton createButtonPokemon(Pokemon pokemon, Consumer<PokemonButtonAction> actionpokemon) {
    if (pokemon == null) {
      return GooeyButton.builder()
        .display(CobbleUtils.language.getItemNoPokemon().getItemStack())
        .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(CobbleUtils.language.getItemNoPokemon().getDisplayname()))
        .with(DataComponentTypes.LORE,
          new LoreComponent(AdventureTranslator.toNativeL(CobbleUtils.language.getItemNoPokemon().getLore())))
        .build();
    }

    return GooeyButton.builder()
      .display(PokemonItem.from(pokemon))
      .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(PokemonUtils.replace(CobbleUtils.language.getPokemonnameformat(), pokemon)))
      .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(lorepokemon(pokemon))))
      .onClick(action -> actionpokemon.accept(new PokemonButtonAction(action, pokemon)))
      .build();
  }


  /**
   * Check if the slot is inside the inventory
   *
   * @param itemModel The item model to check
   * @param rows      The rows of the inventory
   *
   * @return If the slot is inside the inventory
   */
  public static boolean isInside(ItemModel itemModel, int rows) {
    int max = rows * 9 - 1;
    return itemModel.getSlot() >= 0 && itemModel.getSlot() <= max;
  }

  /**
   * Check if the slot is inside the inventory
   *
   * @param slot The slot to check
   * @param rows The rows of the inventory
   *
   * @return If the slot is inside the inventory
   */
  public static boolean isInside(int slot, int rows) {
    int max = rows * 9 - 1;
    return slot >= 0 && slot <= max;
  }

  /**
   * Create a button with the pokemon data
   *
   * @param actionConfirm The action to do when the button is clicked
   * @param closeaction   The action to do when the button is clicked
   *
   * @return The button with the pokemon data
   */
  public static GooeyPage confirmMenu(Consumer<ButtonAction> actionConfirm, Consumer<ButtonAction> closeaction) {
    ChestTemplate template = ChestTemplate.builder(3).build();
    GooeyButton fill = GooeyButton.builder()
      .display(Utils.parseItemId(CobbleUtils.config.getFill())).build();
    template
      .fill(fill)
      .set(1, 2, getConfirmButton(actionConfirm))
      .set(1, 6, getCancelButton(closeaction));
    return GooeyPage.builder()
      .template(template)
      .title(AdventureTranslator.toNative(CobbleUtils.language.getTitleconfirm()))
      .onClose(pageAction -> SoundUtil.playSound(CobbleUtils.language.getSoundclose(), pageAction.getPlayer()))
      .onOpen(pageAction -> SoundUtil.playSound(CobbleUtils.language.getSoundopen(), pageAction.getPlayer()))
      .build();
  }

  /**
   * Create a button with the pokemon data
   *
   * @param pokemon       The pokemon to get the data
   * @param lore          The lore to replace
   * @param add           If add the default lore
   * @param actionpokemon The action to do when the button is clicked
   *
   * @return The button with the pokemon data
   */
  public static GooeyButton createButtonPokemon(Pokemon pokemon, List<String> lore, boolean add,
                                                Consumer<PokemonButtonAction> actionpokemon) {
    if (pokemon == null) {
      return GooeyButton.builder()
        .display(CobbleUtils.language.getItemNoPokemon().getItemStack())
        .with(DataComponentTypes.CUSTOM_NAME,
          AdventureTranslator.toNative(CobbleUtils.language.getItemNoPokemon().getDisplayname()))
        .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(CobbleUtils.language.getItemNoPokemon().getLore())))
        .build();
    }

    return GooeyButton.builder()
      .display(PokemonItem.from(pokemon))
      .with(DataComponentTypes.CUSTOM_NAME,
        AdventureTranslator.toNative(PokemonUtils.replace(CobbleUtils.language.getPokemonnameformat(),
          pokemon)))
      .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(lorepokemon(pokemon, lore, add))))
      .onClick(action -> actionpokemon.accept(new PokemonButtonAction(action, pokemon)))
      .build();
  }

  /**
   * Create a button with the pokemon data
   *
   * @param pokemon The pokemon to get the data
   * @param lore    The lore to replace
   * @param add     If add the default lore
   *
   * @return The button with the pokemon data
   */
  public static List<String> lorepokemon(Pokemon pokemon, List<String> lore, boolean add) {
    List<String> finalLore = new ArrayList<>();
    if (add)
      lore.addAll(new ArrayList<>(CobbleUtils.language.getLorepokemon()));
    for (String s : lore) {
      String replaced = PokemonUtils.replace(s, pokemon);
      for (int i = 0; i < 4; i++) {
        replaced = replaced.replace("%move" + (i + 1) + "%", move(pokemon.getMoveSet().get(i)));
      }

      finalLore.add(replaced);
    }
    return finalLore;
  }

  /**
   * Create a button with the pokemon data
   *
   * @param pokemon The pokemon to get the data
   * @param lore    The lore to replace
   *
   * @return The button with the pokemon data
   */
  public static List<String> lorepokemon(Pokemon pokemon, List<String> lore) {
    List<String> finalLore = new ArrayList<>();
    lore.addAll(new ArrayList<>(CobbleUtils.language.getLorepokemon()));
    for (String s : lore) {
      String replaced = PokemonUtils.replace(s, pokemon);
      for (int i = 0; i < 4; i++) {
        replaced = replaced.replace("%move" + (i + 1) + "%", move(pokemon.getMoveSet().get(i)));
      }

      finalLore.add(replaced);
    }
    return finalLore;
  }

  /**
   * Create a button with the pokemon data
   *
   * @param pokemon The pokemon to get the data
   *
   * @return The button with the pokemon data
   */
  public static List<String> lorepokemon(Pokemon pokemon) {
    List<String> lore = new ArrayList<>(CobbleUtils.language.getLorepokemon());
    List<String> finalLore = new ArrayList<>();
    for (String s : lore) {
      String replaced = PokemonUtils.replace(s, pokemon);
      for (int i = 0; i < 4; i++) {
        replaced = replaced.replace("%move" + (i + 1) + "%", move(pokemon.getMoveSet().get(i)));
      }

      finalLore.add(replaced);
    }
    return finalLore;
  }

  /**
   * Get the move name
   *
   * @param move The move to get the name
   *
   * @return The move name
   */
  private static String move(Move move) {
    if (move != null) {
      return PokemonUtils.getMoveTranslate(move);
    }
    return CobbleUtils.language.getNone();
  }

  /**
   * Create a button with the pokemon data
   *
   * @param command The command to execute
   * @param action  The action to do when the button is clicked
   *
   * @return The button with the pokemon data
   */
  public static GooeyButton createButtonCommand(String command, Consumer<ButtonAction> action) {
    AtomicReference<GooeyButton> button = new AtomicReference<>();
    CobbleUtils.config.getItemsCommands().forEach((c, i) -> {
      if (command.startsWith(c) && button.get() == null) {
        button.set(GooeyButton.builder()
          .display(i.getItemStack())
          .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(i.getDisplayname()))
          .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(i.getLore())))
          .onClick(action)
          .build());
      }
    });
    if (button.get() != null)
      return button.get();
    return GooeyButton.builder()
      .display(CobbleUtils.language.getItemCommand().getItemStack())
      .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(command))
      .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(CobbleUtils.language.getItemCommand().getLore())))
      .onClick(action)
      .build();
  }

  /**
   * Create a button with the item data
   *
   * @param itemStack The item to get the data
   * @param action    The action to do when the button is clicked
   *
   * @return The button with the item data
   */
  public static GooeyButton createButtonItem(ItemStack itemStack, Consumer<ButtonAction> action) {
    Text title;
    ItemStack item = itemStack.copy();
    if (item.get(DataComponentTypes.CUSTOM_DATA) != null) {
      title = item.get(DataComponentTypes.CUSTOM_NAME);
    } else {
      title = AdventureTranslator.toNative(CobbleUtils.language.getColoritem() + item.getName().getString());
    }
    return GooeyButton.builder()
      .display(item)
      .with(DataComponentTypes.CUSTOM_NAME, title)
      .onClick(action)
      .build();
  }

  /**
   * Create a button with the item data
   *
   * @param pcStore       The pcStore to get the data
   * @param actionpokemon The action to do when the button is clicked
   * @param closeaction   The action to do when the button is clicked
   * @param titlemenu     The title of the menu
   *
   * @return The button with the item data
   *
   * @throws ExecutionException   If the computation threw an exception
   * @throws InterruptedException If the current thread was interrupted
   */
  public static GooeyPage createPagePc(PCStore pcStore,
                                       Consumer<PokemonButtonAction> actionpokemon,
                                       Consumer<ButtonAction> closeaction,
                                       String titlemenu) throws ExecutionException, InterruptedException {
    ChestTemplate template = ChestTemplate
      .builder(6)
      .build();

    // Lista de futuros para almacenar los resultados de CompletableFuture
    List<Button> buttons = new ArrayList<>();

    // Crear CompletableFuture para cada Pokemon en pcStore
    for (Pokemon pokemon : pcStore) {
      Button buttonFuture = UIUtils.createButtonPokemon(pokemon,
        action -> actionpokemon.accept(new PokemonButtonAction(action.getAction(), pokemon)));
      buttons.add(buttonFuture);
    }


    template.fill(GooeyButton.of(Utils.parseItemId(CobbleUtils.config.getFill())))
      .rectangle(0, 0, 5, 9, new PlaceholderButton())
      .set(5, 4, getCloseButton(closeaction))
      .set(5, 0, getLinkedPageButton(CobbleUtils.language.getItemPrevious(), LinkType.Previous))
      .set(5, 8, getLinkedPageButton(CobbleUtils.language.getItemNext(), LinkType.Next));

    Text title = AdventureTranslator.toNative(titlemenu);
    LinkedPage firstPage = PaginationHelper.createPagesFromPlaceholders(template, buttons, null);
    firstPage.setTitle(title);
    return firstPage;
    //return PaginationHelper.createPagesFromPlaceholders(template, buttons, linkedPageBuilder);
  }

  /**
   * Create a button with the item data
   *
   * @param partyStore    The partyStore to get the data
   * @param actionpokemon The action to do when the button is clicked
   * @param titlemenu     The title of the menu
   *
   * @return The button with the item data
   *
   * @throws ExecutionException   If the computation threw an exception
   * @throws InterruptedException If the current thread was interrupted
   */
  public static GooeyPage createPageParty(PlayerPartyStore partyStore,
                                          Consumer<PokemonButtonAction> actionpokemon,
                                          String titlemenu) throws ExecutionException, InterruptedException {
    ChestTemplate template = ChestTemplate.builder(4).build();

    List<CompletableFuture<Void>> slotFutures = new ArrayList<>();

    for (int i = 0; i < partyStore.size(); i++) {
      int slotIndex = i;
      CompletableFuture<Void> slotFuture = CompletableFuture.runAsync(() -> {
        GooeyButton slot;
        Pokemon pokemon = partyStore.get(slotIndex);
        slot = UIUtils.createButtonPokemon(pokemon, actionpokemon);
        int row = slotIndex / 3 + 1;
        int col = slotIndex % 3 + 3;
        template.set(row, col, slot);
      });
      slotFutures.add(slotFuture);
    }

    CompletableFuture<Void> allSlotsFuture = CompletableFuture.allOf(
      slotFutures.toArray(new CompletableFuture[0]));

    return allSlotsFuture.thenApplyAsync(v -> {
      template.fill(GooeyButton.of(Utils.parseItemId(CobbleUtils.config.getFill())));
      return GooeyPage.builder()
        .template(template)
        .title(AdventureTranslator.toNative(CobbleUtils.language.getTitleparty()))
        .onClose(pageAction -> SoundUtil.playSound(CobbleUtils.language.getSoundclose(), pageAction.getPlayer()))
        .onOpen(pageAction -> SoundUtil.playSound(CobbleUtils.language.getSoundopen(), pageAction.getPlayer()))
        .build();
    }).get();
  }

  /**
   * Create a button with the item data
   *
   * @param player        The player to get the data
   * @param actionpokemon The action to do when the button is clicked
   *
   * @return The button with the item data
   *
   * @throws ExecutionException   If the computation threw an exception
   * @throws InterruptedException If the current thread was interrupted
   */
  public static GooeyPage createPageParty(ServerPlayerEntity player, Consumer<PokemonButtonAction> actionpokemon,
                                          Consumer<ButtonAction> actionclose) throws ExecutionException,
    InterruptedException {
    ChestTemplate template = ChestTemplate.builder(4).build();
    PlayerPartyStore partyStore = null;
    partyStore = Cobblemon.INSTANCE.getStorage().getParty(player);
    List<CompletableFuture<Void>> slotFutures = new ArrayList<>();

    for (int i = 0; i < partyStore.size(); i++) {
      int slotIndex = i;
      PlayerPartyStore finalPartyStore = partyStore;
      CompletableFuture<Void> slotFuture = CompletableFuture.runAsync(() -> {
        GooeyButton slot;
        Pokemon pokemon = finalPartyStore.get(slotIndex);
        slot = UIUtils.createButtonPokemon(pokemon, actionpokemon);
        int row = slotIndex / 3 + 1;
        int col = slotIndex % 3 + 3;
        template.set(row, col, slot);
      });
      slotFutures.add(slotFuture);
    }

    CompletableFuture<Void> allSlotsFuture = CompletableFuture.allOf(
      slotFutures.toArray(new CompletableFuture[0]));

    return allSlotsFuture.thenApplyAsync(v -> {
      template.fill(GooeyButton.of(Utils.parseItemId(CobbleUtils.config.getFill())));
      template.set(0, 4, getPcButton(player, actionpokemon, actionclose));

      return GooeyPage.builder()
        .template(template)
        .title(AdventureTranslator.toNative(CobbleUtils.language.getTitleparty()))
        .onClose(pageAction -> SoundUtil.playSound(CobbleUtils.language.getSoundclose(), pageAction.getPlayer()))
        .onOpen(pageAction -> SoundUtil.playSound(CobbleUtils.language.getSoundopen(), pageAction.getPlayer()))
        .build();
    }).get();
  }

  /**
   * Get the linked page button
   *
   * @param itemModel The item model to get the button
   * @param linkType  The type of link
   *
   * @return The linked page button
   */
  public static LinkedPageButton getLinkedPageButton(ItemModel itemModel, LinkType linkType) {
    return LinkedPageButton.builder()
      .display(itemModel.getItemStack())
      .onClick(action -> {
        SoundUtil.playSound(CobbleUtils.language.getSoundopen(), action.getPlayer());
      })
      .linkType(linkType)
      .build();
  }

  /**
   * Get the linked page button
   *
   * @param itemModel The item model to get the button
   * @param linkType  The type of link
   * @param action    The action to do when the button is clicked
   *
   * @return The linked page button
   */
  public static LinkedPageButton getLinkedPageButton(ItemModel itemModel, LinkType linkType,
                                                     Consumer<ButtonAction> action) {
    return LinkedPageButton.builder()
      .display(itemModel.getItemStack())
      .linkType(linkType)
      .onClick(action)
      .build();
  }

  /**
   * Get the close button
   *
   * @param action The action to do when the button is clicked
   *
   * @return The close button
   */
  public static GooeyButton getCloseButton(Consumer<ButtonAction> action) {
    ItemModel itemModel = CobbleUtils.language.getItemClose();
    return GooeyButton.builder()
      .display(itemModel.getItemStack())
      .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNativeWithOutPrefix(itemModel.getDisplayname()))
      .onClick(action)
      .build();
  }

  /**
   * Get the pc button
   *
   * @param player        The player to get the data
   * @param actionpokemon The action to do when the button is clicked
   * @param closeaction   The action to do when the button is clicked
   *
   * @return The pc button
   */
  public static GooeyButton getPcButton(ServerPlayerEntity player, Consumer<PokemonButtonAction> actionpokemon,
                                        Consumer<ButtonAction> closeaction) {
    ItemModel itemModel = CobbleUtils.language.getItemPc();
    PCStore pcStore = Cobblemon.INSTANCE.getStorage().getPC(player);
    return GooeyButton.builder()
      .display(itemModel.getItemStack())
      .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(itemModel.getDisplayname()))
      .onClick(action -> {
        try {
          UIManager.openUIForcefully(action.getPlayer(), createPagePc(pcStore, actionpokemon,
            closeaction, CobbleUtils.language.getTitlepc()));
        } catch (ExecutionException | InterruptedException e) {
          e.printStackTrace();
        }
      })
      .build();
  }

  /**
   * Get the confirm button
   *
   * @param action The action to do when the button is clicked
   *
   * @return The confirm button
   */
  public static GooeyButton getConfirmButton(Consumer<ButtonAction> action) {
    ItemModel itemModel = CobbleUtils.language.getItemConfirm();
    return GooeyButton.builder()
      .display(itemModel.getItemStack())
      .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(itemModel.getDisplayname()))
      .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(itemModel.getLore())))
      .onClick(action)
      .build();
  }

  /**
   * Get the back button
   *
   * @param pokemon       The pokemon to get the data
   * @param actionPokemon The action to do when the button is clicked
   *
   * @return The confirm button
   */
  public static GooeyButton getConfirmButton(Pokemon pokemon, Consumer<PokemonButtonAction> actionPokemon) {
    ItemModel itemModel = CobbleUtils.language.getItemConfirm();
    return GooeyButton.builder()
      .display(itemModel.getItemStack())
      .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(itemModel.getDisplayname()))
      .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(itemModel.getLore())))
      .onClick(action -> actionPokemon.accept(new PokemonButtonAction(action, pokemon)))
      .build();
  }

  /**
   * Get the cancel button
   *
   * @param action The action to do when the button is clicked
   *
   * @return The cancel button
   */
  public static GooeyButton getCancelButton(Consumer<ButtonAction> action) {
    ItemModel itemModel = CobbleUtils.language.getItemCancel();
    return GooeyButton.builder()
      .display(itemModel.getItemStack())
      .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNativeWithOutPrefix(itemModel.getDisplayname()))
      .onClick(action)
      .build();
  }

  /**
   * Get the back button
   *
   * @param pokemon       The pokemon to get the data
   * @param actionPokemon The action to do when the button is clicked
   *
   * @return The confirm button
   */
  public static GooeyButton getCancelButton(Pokemon pokemon, Consumer<PokemonButtonAction> actionPokemon) {
    ItemModel itemModel = CobbleUtils.language.getItemCancel();
    return GooeyButton.builder()
      .display(itemModel.getItemStack())
      .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNativeWithOutPrefix(itemModel.getDisplayname()))
      .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(itemModel.getLore())))
      .onClick(action -> actionPokemon.accept(new PokemonButtonAction(action, pokemon)))
      .build();
  }

  /**
   * Get the previous button
   *
   * @param action The action to do when the button is clicked
   *
   * @return The previous button
   */
  public static LinkedPageButton getPreviousButton(Consumer<ButtonAction> action) {
    return LinkedPageButton.builder()
      .display(CobbleUtils.language.getItemPrevious().getItemStack())
      .linkType(LinkType.Previous)
      .onClick(action)
      .build();
  }

  /**
   * Get the next button
   *
   * @param action The action to do when the button is clicked
   *
   * @return The next button
   */
  public static LinkedPageButton getNextButton(Consumer<ButtonAction> action) {
    ItemModel itemModel = CobbleUtils.language.getItemNext();
    ItemStack itemStack = itemModel.getItemStack();
    itemStack.set(DataComponentTypes.CUSTOM_NAME,
      AdventureTranslator.toNative(itemModel.getDisplayname()));
    return LinkedPageButton.builder()
      .display(itemStack)
      .linkType(LinkType.Next)
      .onClick(action)
      .build();
  }

}
