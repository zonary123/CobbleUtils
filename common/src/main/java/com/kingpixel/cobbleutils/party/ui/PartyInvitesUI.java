package com.kingpixel.cobbleutils.party.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.PlaceholderButton;
import ca.landonjw.gooeylibs2.api.button.linked.LinkType;
import ca.landonjw.gooeylibs2.api.button.linked.LinkedPageButton;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.page.Page;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.Utils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Carlos Varas Alonso - 28/06/2024 6:11
 */
public class PartyInvitesUI {
  public static Page getPartyInvites(ServerPlayerEntity player) {
    ChestTemplate template = ChestTemplate.builder(2).build();
    List<Button> buttons = new ArrayList<>();

    CobbleUtils.partyManager.getInvites(player).forEach((partyData) -> {
      GooeyButton invite = GooeyButton.builder()
        .display(Utils.parseItemId("minecraft:emerald"))
        .with(DataComponentTypes.ITEM_NAME, AdventureTranslator.toNative(partyData.getName()))
        .onClick(action -> {
          CobbleUtils.partyManager.acceptInvite(player, partyData.getId());
          UIManager.openUIForcefully(player, getPartyInvites(player));
        })
        .build();
      buttons.add(invite);
    });

    buttons.removeIf(Objects::isNull);

    LinkedPageButton previus = LinkedPageButton.builder()
      .display(Utils.parseItemId("minecraft:arrow"))
      .with(DataComponentTypes.ITEM_NAME, AdventureTranslator.toNative("Previous Page"))
      .linkType(LinkType.Previous)
      .build();

    LinkedPageButton next = LinkedPageButton.builder()
      .display(Utils.parseItemId("minecraft:arrow"))
      .with(DataComponentTypes.ITEM_NAME, AdventureTranslator.toNative("Next Page"))
      .linkType(LinkType.Next)
      .build();

    ItemStack itemClose = Items.RED_STAINED_GLASS_PANE.getDefaultStack();
    itemClose.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Close"));
    GooeyButton close = GooeyButton.builder()
      .display(itemClose)
      .onClick(action -> {
        UIManager.closeUI(action.getPlayer());
      })
      .build();

    PlaceholderButton placeholder = new PlaceholderButton();

    ItemStack fillItem = Items.GRAY_STAINED_GLASS_PANE.getDefaultStack();
    fillItem.set(DataComponentTypes.ITEM_NAME, Text.empty());

    GooeyButton fill = GooeyButton.builder()
      .display(fillItem).build();
    template.fill(fill)
      .rectangle(0, 0, 2, 9, placeholder)
      .fillFromList(buttons)
      .set(1, 4, close)
      .set(1, 0, previus)
      .set(1, 8, next);

    LinkedPage.Builder linkedPageBuilder = LinkedPage.builder()
      .title(CobbleUtils.partyLang.getTitleinvites());

    LinkedPage firstPage = PaginationHelper.createPagesFromPlaceholders(template, buttons, linkedPageBuilder);
    return firstPage;
  }
}
