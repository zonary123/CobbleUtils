package com.kingpixel.cobbleutils.Model;

import lombok.Getter;

/**
 * @author Carlos Varas Alonso - 16/03/2025 4:11
 */
@Getter
public enum Priority {
  HIGHEST(5),
  HIGH(4),
  MEDIUM(3),
  LOW(2),
  LOWEST(1);

  private final int value;

  Priority(int value) {
    this.value = value;
  }

}
