package com.kingpixel.cobbleutils.Model;

import ca.landonjw.gooeylibs2.api.button.PlaceholderButton;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import lombok.*;

/**
 * @author Carlos Varas Alonso - 21/02/2025 4:35
 */
@Getter
@Setter
@EqualsAndHashCode
@Data
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

  public int getOccupiedSlots() {
    return length * width;
  }

  public int getStartSlot() {
    return (startRow * 9) + startColumn;
  }

  public int getEndSlot(int rows) {
    int totalSlots = rows * 9;
    int endSlot = getStartSlot() + getOccupiedSlots() - 1;

    return Math.min(endSlot, totalSlots - 1);
  }

  public int getActualOccupiedSlots(int rows) {
    return getEndSlot(rows) - getStartSlot() + 1;
  }


  public int getEndRow() {
    return startRow + length - 1;
  }
  
  public int getEndColumn() {
    return startColumn + width - 1;
  }

}
