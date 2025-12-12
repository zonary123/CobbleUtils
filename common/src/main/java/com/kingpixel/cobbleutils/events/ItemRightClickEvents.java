package com.kingpixel.cobbleutils.events;

import ca.landonjw.gooeylibs2.api.UIManager;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.ui.PartyPcMenu;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import dev.architectury.event.CompoundEventResult;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

/**
 * @author Carlos Varas Alonso - 25/06/2024 19:38
 */
public class ItemRightClickEvents {
  public static CompoundEventResult register(PlayerEntity player, Hand hand) {
    if (player.isInPose(EntityPose.CROUCHING))
      return CompoundEventResult.pass();
    ItemStack itemStack = player.getStackInHand(hand);
    if (itemStack.isEmpty()) return CompoundEventResult.pass();
    NbtComponent tag = itemStack.get(DataComponentTypes.CUSTOM_DATA);
    if (tag == null) return CompoundEventResult.pass();
    if (tag.contains("shinytoken")) {
      if (!CobbleUtils.config.isActiveshinytoken()) return CompoundEventResult.pass();
      open(PlayerUtils.castPlayer(player), itemStack);
      return CompoundEventResult.pass();
    }

    return CompoundEventResult.pass();
  }

  private static void open(ServerPlayerEntity player, ItemStack itemStack) {
    var builder = PartyPcMenu.builder()
      .setPlayer(player)
      .setTemplateConsumer(template -> {
      })
      .setCloseAction(close -> {
        open(player, itemStack);
      })
      .setPokemonAction(action -> {
        var pokemon = action.getPokemon();
        if (!pokemon.getShiny()) {
          pokemon.setShiny(true);
          itemStack.decrement(1);
          UIManager.closeUI(action.getAction().getPlayer());
        }
      })
      .setBlackList(CobbleUtils.config.getShinytokenBlacklist())
      .build();
    // Use the default menu API - this will force default menus if useDefault is true
    PartyPcMenu.openDefaultParty(builder);
  }
}
