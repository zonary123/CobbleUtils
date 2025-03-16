package com.kingpixel.cobbleutils.Model;

import lombok.Data;

/**
 * @author Carlos Varas Alonso - 16/03/2025 3:20
 */
@Data
public class EconomyUse {
  private String EconomyId;
  private String currency;

  public EconomyUse(String EconomyId, String currency) {
    this.EconomyId = EconomyId;
    this.currency = currency;
  }
}
