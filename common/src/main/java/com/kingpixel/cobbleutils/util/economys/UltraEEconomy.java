package com.kingpixel.cobbleutils.util.economys;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.ultraeconomy.api.UltraEconomyApi;
import com.kingpixel.ultraeconomy.config.Currencies;
import com.kingpixel.ultraeconomy.models.Currency;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 28/09/2025 1:12
 */
public class UltraEEconomy extends EconomyAbstract {
  public static final String IDENTIFY = "ULTRA_ECONOMY";

  @Override public String getIdentify() {
    return IDENTIFY;
  }

  @Override public boolean isPresent() {
    try {
      Class.forName("com.kingpixel.ultraeconomy.UltraEconomy");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  @Override public boolean deposit(UUID playerUuid, BigDecimal money, String currency) {
    return UltraEconomyApi.deposit(playerUuid, currency, money);
  }

  @Override public boolean withdraw(UUID playerUuid, BigDecimal money, String currency) {
    return UltraEconomyApi.withdraw(playerUuid, currency, money);
  }

  @Override public BigDecimal getBalance(UUID playerUuid, String currency) {
    return UltraEconomyApi.getBalance(playerUuid, currency);
  }

  @Override public String format(BigDecimal money, String currency) {
    Currency curr = Currencies.getCurrency(currency);
    if (curr == null) return money.toString();
    return curr.format(money);
  }

  @Override public boolean setBalance(UUID playerUuid, BigDecimal money, String currency) {
    return UltraEconomyApi.setBalance(playerUuid, currency, money) != null;
  }

  @Override public int getDecimals(String currency) {
    Currency curr = Currencies.getCurrency(currency);
    if (curr == null) return CobbleUtils.config.getDecimals();
    return curr.getDecimals();
  }
}
