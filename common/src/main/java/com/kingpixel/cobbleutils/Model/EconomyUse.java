package com.kingpixel.cobbleutils.Model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Carlos Varas Alonso - 16/03/2025 3:20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EconomyUse {
  @SerializedName(value = "EconomyId", alternate = {"economyId", "economy"})
  private String EconomyId = "IMPACTOR";

  @SerializedName(value = "currency", alternate = {"Currency"})
  private String currency = "";

  public String getEconomyId() {
    return EconomyId != null && !EconomyId.isBlank() ? EconomyId : "IMPACTOR";
  }

  public String getCurrency() {
    return currency != null ? currency : "";
  }
}
