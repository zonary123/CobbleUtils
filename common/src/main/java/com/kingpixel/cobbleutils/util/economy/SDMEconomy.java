package com.kingpixel.cobbleutils.util.economy;

import com.kingpixel.cobbleutils.CobbleUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.sixik.sdm_economy.api.CurrencyHelper;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 16/03/2025 3:56
 */
@EqualsAndHashCode(callSuper = true) @Data
public class SDMEconomy extends EconomyAbstract {
  public static final String IDENTIFY = "SDME_ECONOMY";

  public SDMEconomy() {
  }

  @Override public String getIdentify() {
    return IDENTIFY;
  }


  @Override public boolean isPresent() {
    CurrencyHelper.getAllCurrencyKeys();
    return true;
  }

  @Override public boolean deposit(UUID playerUuid, BigDecimal money, String currency) {
    CurrencyHelper.addMoney(getPlayer(playerUuid), currency, money.longValue());
    return true;
  }

  @Override public boolean withdraw(UUID playerUuid, BigDecimal money, String currency) {
    CurrencyHelper.setMoney(getPlayer(playerUuid), currency,
      getBalance(playerUuid, currency).longValue() - money.longValue());
    return true;
  }

  @Override public BigDecimal getBalance(UUID playerUuid, String currency) {
    return BigDecimal.valueOf(CurrencyHelper.getMoney(getPlayer(playerUuid), currency));
  }

  @Override public String format(BigDecimal money, String currency) {
    return CobbleUtils.language.getDefaultSymbol() + " " + money.doubleValue();
  }

  @Override public boolean setBalance(UUID playerUuid, BigDecimal money, String currency) {
    CurrencyHelper.setMoney(getPlayer(playerUuid), currency, money.longValue());
    return true;
  }
}
