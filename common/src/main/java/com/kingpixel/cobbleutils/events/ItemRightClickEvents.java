package com.kingpixel.cobbleutils.events;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.ui.ShinyTokenUI;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import dev.architectury.event.CompoundEventResult;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

/**
 * @author Carlos Varas Alonso - 25/06/2024 19:38
 */
public class ItemRightClickEvents {
  public static CompoundEventResult register(PlayerEntity player, Hand hand) {
    ItemStack itemStack = player.getStackInHand(hand);
    if (itemStack.isEmpty()) return CompoundEventResult.pass();
    NbtComponent tag = itemStack.get(DataComponentTypes.CUSTOM_DATA);
    if (tag == null) return CompoundEventResult.pass();
    if (tag.contains("shinytoken") && itemStack.getItem() == CobbleUtils.config.getShinytoken().getItemStack().getItem()) {
      if (!CobbleUtils.config.isActiveshinytoken()) return CompoundEventResult.pass();
      try {
        ShinyTokenUI.openmenu((ServerPlayerEntity) player);
      } catch (ClassCastException ignored) {
        ShinyTokenUI.openmenu(PlayerUtils.castPlayer(player));
      }
    }
    return CompoundEventResult.pass();

  }
}
