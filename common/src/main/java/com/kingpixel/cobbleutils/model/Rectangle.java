package com.kingpixel.cobbleutils.model;

import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.PlaceholderButton;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import lombok.*;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 21/02/2025 4:35
 */
@Getter
@Setter
@EqualsAndHashCode
@Data
@Builder
@ToString
public class Rectangle {
  private int startRow;
  private int startColumn;
  private int length;
  private int width;

  public Rectangle() {
    this.startRow = 0;
    this.startColumn = 0;
    this.length = 5;
    this.width = 9;
  }

  public Rectangle(int rows) {
    this.startRow = 0;
    this.startColumn = 0;
    this.length = rows - 1;
    this.width = 9;
  }

  public Rectangle(int startRow, int startColumn, int length, int width) {
    this.startRow = startRow;
    this.startColumn = startColumn;
    this.length = length;
    this.width = width;
  }

  public void apply(ChestTemplate template) {
    template.rectangle(startRow, startColumn, length, width, new PlaceholderButton());
  }

  public int getSlotsFree(int rows) {
    int totalSlots = rows * 9;
    int occupiedSlots = length * width;

    int startSlot = (startRow * 9) + startColumn; // Calcula el índice inicial
    int endSlot = startSlot + occupiedSlots - 1;  // Calcula el índice final ocupado

    if (endSlot >= totalSlots) {
      endSlot = totalSlots - 1;
    }

    int actualOccupiedSlots = endSlot - startSlot + 1; // Slots realmente ocupados

    return totalSlots - actualOccupiedSlots;
  }

  public void apply(ChestTemplate template, List<GooeyButton> buttons) {
    int max = buttons.size();
    int index = 0;
    int maxRow = startRow + length;
    int maxColumn = startColumn + width;
    for (int row = startRow; row < maxRow; row++) {
      for (int column = startColumn; column < maxColumn; column++) {
        if (index >= max) return;
        template.set(row, column, buttons.get(index++));
      }
    }
  }

  public int getTotalSlots() {
    return length * width;
  }

}
