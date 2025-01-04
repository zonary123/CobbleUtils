package com.kingpixel.cobbleutils.features.breeding.models;

import com.kingpixel.cobbleutils.Model.ItemModel;
import lombok.Getter;

/**
 * @author Carlos Varas Alonso - 03/01/2025 21:08
 */
@Getter
public class SelectMenu {
  private String title;
  private ItemModel previous;
  private ItemModel close;
  private ItemModel next;

  public SelectMenu(String title, ItemModel previous, ItemModel close, ItemModel next) {
    this.title = title;
    this.previous = previous;
    this.close = close;
    this.next = next;
  }
}
