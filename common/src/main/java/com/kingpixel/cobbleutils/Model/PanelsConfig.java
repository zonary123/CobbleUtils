package com.kingpixel.cobbleutils.Model;

import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 05/01/2025 22:00
 */
@Getter
@Setter
public class PanelsConfig {
  private ItemModel fill;
  private List<Integer> slots;

  public PanelsConfig() {
    this.fill = new ItemModel();
    this.slots = List.of();
  }

  public PanelsConfig(ItemModel fill, List<Integer> slots) {
    this.fill = fill;
    this.slots = slots;
  }

  public static void apllyConfig(ChestTemplate template, List<PanelsConfig> panelsConfigs) {
    for (PanelsConfig panelsConfig : panelsConfigs) {
      GooeyButton button = GooeyButton.builder()
        .display(panelsConfig.getFill().getItemStack())
        .build();
      for (Integer slot : panelsConfig.slots) {
        template.set(slot, button);
      }
    }
  }
}
