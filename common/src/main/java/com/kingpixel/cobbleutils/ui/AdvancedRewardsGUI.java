package com.kingpixel.cobbleutils.ui;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.Model.PanelsConfig;
import lombok.Data;

import java.util.List;

/**
 * Configuration for Advanced Rewards GUI (pool display)
 * @author Carlos Varas Alonso
 */
@Data
public class AdvancedRewardsGUI {
  private int rows;
  private ItemModel close;
  private ItemModel next;
  private ItemModel previous;
  private List<PanelsConfig> panels;

  public AdvancedRewardsGUI() {
    this.rows = 6;

    // Initialize buttons from language if available, otherwise use defaults
    if (CobbleUtils.language != null) {
      this.close = CobbleUtils.language.getItemClose();
      this.next = CobbleUtils.language.getItemNext();
      this.previous = CobbleUtils.language.getItemPrevious();
    } else {
      this.close = new ItemModel("minecraft:barrier", "&cClose");
      this.close.setSlot(49);
      this.next = new ItemModel("minecraft:arrow", "&eNext");
      this.next.setSlot(53);
      this.previous = new ItemModel("minecraft:arrow", "&ePrevious");
      this.previous.setSlot(45);
    }

    // Initialize panels with default fill
    this.panels = List.of(
      new PanelsConfig(rows)
    );

    // Remove invalid slots
    int totalSlots = rows * 9;
    for (PanelsConfig panel : panels) {
      panel.getSlots().removeIf(slot -> slot < 0 || slot >= totalSlots);
    }
  }
}
