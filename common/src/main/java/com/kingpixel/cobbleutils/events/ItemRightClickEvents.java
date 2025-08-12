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

import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 25/06/2024 19:38
 */
public class ItemRightClickEvents {
  public static CompoundEventResult register(PlayerEntity player, Hand hand) {
    CompletableFuture.runAsync(() -> {
        if (player.isInPose(EntityPose.CROUCHING)) return;
        ItemStack itemStack = player.getStackInHand(hand);
        if (itemStack.isEmpty()) return;
        NbtComponent tag = itemStack.get(DataComponentTypes.CUSTOM_DATA);
        if (tag == null) return;
        if (tag.contains("shinytoken") && itemStack.getItem() == CobbleUtils.config.getShinytoken().getItemStack().getItem()) {
          if (!CobbleUtils.config.isActiveshinytoken()) return;
          open(PlayerUtils.castPlayer(player), itemStack);
        }
      }, CobbleUtils.EXECUTOR_COBBLEUTILS)
      .exceptionally(e -> {
        e.printStackTrace();
        return null;
      });
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
      .setPartyPcMenu(CobbleUtils.language.getPartyPcMenu())
      .setConfirmMenu(CobbleUtils.language.getConfirmMenu())
      .build();
    CobbleUtils.language.getPartyPcMenu().openParty(builder);
  }
}
