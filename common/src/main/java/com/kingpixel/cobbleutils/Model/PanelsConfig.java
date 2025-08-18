package com.kingpixel.cobbleutils.Model;

import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobbleutils.util.UIUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.Unit;

import java.util.ArrayList;
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

  public PanelsConfig(int rows) {
    this.fill = new ItemModel("minecraft:gray_stained_glass_pane");
    this.slots = new ArrayList<>();
    int size = rows * 9;
    for (int i = 0; i < size; i++) {
      this.slots.add(i);
    }
  }

  public PanelsConfig(ItemModel fill, List<Integer> slots) {
    this.fill = fill;
    this.slots = slots;
  }

  public PanelsConfig(ItemModel fill, int rows) {
    this.fill = fill;
    this.slots = new ArrayList<>();
    int size = rows * 9;
    for (int i = 0; i < size; i++) {
      this.slots.add(i);
    }
  }

  public static void applyConfig(ChestTemplate template, List<PanelsConfig> panelsConfigs) {
    applyConfig(template, panelsConfigs, 6);
  }

  public static void applyConfig(ChestTemplate template, List<PanelsConfig> panelsConfigs, int rows) {
    for (PanelsConfig panelsConfig : panelsConfigs) {
      GooeyButton button = GooeyButton.builder()
        .display(panelsConfig.getFill().getItemStack())
        .with(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE)
        .with(DataComponentTypes.HIDE_TOOLTIP, Unit.INSTANCE)
        .build();
      int size = panelsConfig.getSlots().size();
      for (int i = 0; i < size; i++) {
        Integer slot = panelsConfig.getSlots().get(i);
        if (UIUtils.isInside(slot, rows))
          template.set(slot, button);
      }
    }
  }
}
