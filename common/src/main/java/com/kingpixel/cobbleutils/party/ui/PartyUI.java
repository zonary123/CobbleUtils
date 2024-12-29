package com.kingpixel.cobbleutils.party.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.Utils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Carlos Varas Alonso - 28/06/2024 2:16
 */
public class PartyUI {
  public static GooeyPage getPartyMenu() {
    ChestTemplate template = ChestTemplate.builder(3).build();

    GooeyButton invites = GooeyButton.builder()
      .display(Utils.parseItemId("minecraft:emerald"))
      .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("Invites"))
      .onClick(action -> {
        ServerPlayerEntity player = action.getPlayer();
        if (!CobbleUtils.partyManager.isPlayerInParty(player)) {
          UIManager.openUIForcefully(player, PartyInvitesUI.getPartyInvites(player));
        } else {
          player.sendMessage(AdventureTranslator.toNative(CobbleUtils.partyLang.getPartyAlreadyInParty()));
        }
      })
      .build();

    GooeyButton members = GooeyButton.builder()
      .display(Utils.parseItemId("minecraft:emerald"))
      .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("Members"))
      .onClick(action -> {
        ServerPlayerEntity player = action.getPlayer();
        if (CobbleUtils.partyManager.isPlayerInParty(player)) {
          UIManager.openUIForcefully(player,
            PartyMembersUI.getPartyMembers(player));
        } else {
          player.sendMessage(AdventureTranslator.toNative(CobbleUtils.partyLang.getPartynotInParty()));
        }
      })
      .build();

    template.set(1, 1, invites);
    template.set(1, 3, members);

    GooeyPage menu = GooeyPage.builder()
      .template(template)
      .title(AdventureTranslator.toNative(CobbleUtils.partyLang.getTitlemenu()))
      .build();
    return menu;
  }
}
