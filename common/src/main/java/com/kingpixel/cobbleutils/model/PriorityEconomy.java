package com.kingpixel.cobbleutils.model;

import lombok.Data;

/**
 * @author Carlos Varas Alonso - 16/03/2025 4:13
 */
@Data
public class PriorityEconomy {
  private String EconomyId;
  private Priority priority;

  public PriorityEconomy(String EconomyId, Priority priority) {
    this.EconomyId = EconomyId;
    this.priority = priority;
  }

  public int compareTo(PriorityEconomy priorityEconomy) {
    return this.priority.compareTo(priorityEconomy.priority);
  }
}
