package com.kingpixel.cobbleutils.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.PlaceholderButton;
import ca.landonjw.gooeylibs2.api.button.linked.LinkedPageButton;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.page.Page;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.JsonObject;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemObject;
import com.kingpixel.cobbleutils.Model.RewardsData;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.UIUtils;
import com.kingpixel.cobbleutils.util.Utils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Carlos Varas Alonso - 28/06/2024 6:11
 */
public class RewardsUI {
  public static Page getRewards(ServerPlayerEntity player) {
    try {
      ChestTemplate template = ChestTemplate.builder(6).build();
      List<Button> buttons = new ArrayList<>();

      RewardsData rewardsData = CobbleUtils.rewardsManager.getRewardsData().get(player.getUuid());

      rewardsData.getPokemons().forEach(
        pokemon -> buttons.add(UIUtils.createButtonPokemon(Pokemon.Companion.loadFromJSON(DynamicRegistryManager.EMPTY,
            pokemon),
          action -> {
            if (Cobblemon.INSTANCE.getStorage().getParty(player)
              .add(Pokemon.Companion.loadFromJSON(DynamicRegistryManager.EMPTY, pokemon))) {
              rewardsData.getPokemons().remove(pokemon);
              rewardsData.writeInfo();
            }
            UIManager.openUIForcefully(action.getAction().getPlayer(), getRewards(player));
          })));

      rewardsData.getItems().forEach(item -> {
        ItemStack itemStack = ItemObject.toItemStack(item.getItem());
        buttons.add(UIUtils.createButtonItem(itemStack, action -> {
          if (action.getPlayer().getInventory().insertStack(itemStack)) {
            rewardsData.getItems().remove(item);
            rewardsData.writeInfo();
            UIManager.openUIForcefully(action.getPlayer(), getRewards(player));
          }
        }));
      });

      rewardsData.getCommands().forEach(command -> {
        buttons.add(UIUtils.createButtonCommand(command, action -> {
          if (player.getInventory().getEmptySlot() == -1)
            return;
          CommandDispatcher<ServerCommandSource> disparador = CobbleUtils.server.getCommandManager().getDispatcher();
          try {
            ServerCommandSource serverSource = CobbleUtils.server.getCommandSource();
            ParseResults<ServerCommandSource> parse = disparador.parse(command, serverSource);
            disparador.execute(parse);
          } catch (CommandSyntaxException e) {
            System.err.println("Error al ejecutar el comando: " + command);
            e.printStackTrace();
          }
          rewardsData.getCommands().remove(command);
          rewardsData.writeInfo();
          UIManager.openUIForcefully(action.getPlayer(), getRewards(player));

        }));
      });

      buttons.removeIf(Objects::isNull);

      GooeyButton getAllRewards = GooeyButton.builder()
        .display(Utils.parseItemId("minecraft:chest"))
        .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&7Get all rewards"))
        .onClick(action -> {
          List<ItemObject> itemsToRemove = new ArrayList<>();
          for (ItemObject item : rewardsData.getItems()) {
            ItemStack itemStack = ItemObject.toItemStack(item.getItem());
            if (action.getPlayer().getInventory().insertStack(itemStack)) {
              itemsToRemove.add(item);
            }
          }
          rewardsData.getItems().removeAll(itemsToRemove);

          List<JsonObject> pokemonsToRemove = new ArrayList<>();
          for (JsonObject pokemon : rewardsData.getPokemons()) {
            if (Cobblemon.INSTANCE.getStorage().getParty(action.getPlayer())
              .add(Pokemon.Companion.loadFromJSON(DynamicRegistryManager.EMPTY, pokemon))) {
              pokemonsToRemove.add(pokemon);
            }
          }
          rewardsData.getPokemons().removeAll(pokemonsToRemove);

          rewardsData.getCommands().removeIf(command -> PlayerUtils.executeCommand(command, action.getPlayer()));
          rewardsData.writeInfo();
          UIManager.openUIForcefully(action.getPlayer(), getRewards(player));
        })
        .build();

      template.set(47, getAllRewards);

      LinkedPageButton previus = UIUtils.getPreviousButton(action -> {
      });

      LinkedPageButton next = UIUtils.getNextButton(action -> {
      });

      GooeyButton close = GooeyButton.builder()
        .display(Items.RED_STAINED_GLASS_PANE.getDefaultStack())
        .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(CobbleUtils.language.getClose()))
        .onClick(action -> {
          action.getPlayer().closeHandledScreen();
        })
        .build();

      PlaceholderButton placeholder = new PlaceholderButton();

      ItemStack fillItem = Items.GRAY_STAINED_GLASS_PANE.getDefaultStack();
      fillItem.set(DataComponentTypes.CUSTOM_NAME, Text.literal(""));

      GooeyButton fill = GooeyButton.builder()
        .display(fillItem).build();
      template.fill(fill)
        .rectangle(0, 0, 5, 9, placeholder)
        .fillFromList(buttons)
        .set(5, 4, close)
        .set(5, 0, previus)
        .set(5, 8, next);

      LinkedPage.Builder linkedPageBuilder = LinkedPage.builder()
        .title(AdventureTranslator.toNative(CobbleUtils.language.getTitlemenurewards()));

      LinkedPage firstPage = PaginationHelper.createPagesFromPlaceholders(template, buttons, linkedPageBuilder);
      return firstPage;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

}
