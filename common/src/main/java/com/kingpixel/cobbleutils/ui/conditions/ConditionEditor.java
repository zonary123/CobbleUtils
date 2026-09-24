package com.kingpixel.cobbleutils.ui.conditions;

import com.kingpixel.cobbleutils.Model.conditions.Condition;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * Presentation-layer adapter for a specific Condition type.
 * Decouples GUI rendering and editing interaction from the core domain Condition model.
 *
 * @param <T> the condition type
 */
public interface ConditionEditor<T extends Condition> {

  /**
   * Representative icon item for this condition type in menus.
   *
   * @param condition the condition instance
   * @return representative item stack
   */
  default ItemStack getIcon(T condition) {
    return new ItemStack(Items.PAPER);
  }

  /**
   * Key details or formatted parameter lines to display in the item lore.
   *
   * @param condition the condition instance
   * @return list of formatted strings describing current values
   */
  List<String> getDetails(T condition);

  /**
   * Triggered when the player clicks to edit this condition.
   *
   * @param player the player interacting with the editor
   * @param condition the condition instance to edit
   * @param onUpdated callback to re-render the parent menu when updated
   */
  void openEdit(ServerPlayerEntity player, T condition, Runnable onUpdated);
}
