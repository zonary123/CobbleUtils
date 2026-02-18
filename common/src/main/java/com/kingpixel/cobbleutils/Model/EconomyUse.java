package com.kingpixel.cobbleutils.Model;

import com.kingpixel.cobbleutils.api.EconomyApi;
import com.kingpixel.cobbleutils.util.economys.Economy;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author Carlos Varas Alonso - 16/03/2025 3:20
 * @deprecated {@link EconomySelector}
 */
@Data
@Deprecated(forRemoval = true)
public class EconomyUse {
  private String EconomyId;
  private String currency;

  public EconomyUse(String EconomyId, String currency) {
    this.EconomyId = EconomyId;
    this.currency = currency;
  }


  public String format(BigDecimal amount) {
    Economy eco = EconomyApi.getEconomy(EconomyId);
    if (eco == null) return amount.toString();
    return eco.formatCurrency(currency, amount);
  }
}
