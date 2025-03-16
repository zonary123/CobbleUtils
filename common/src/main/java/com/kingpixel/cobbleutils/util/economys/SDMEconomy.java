package com.kingpixel.cobbleutils.util.economys;

import com.kingpixel.cobbleutils.CobbleUtils;
import net.sixik.sdm_economy.api.CurrencyHelper;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 16/03/2025 3:56
 */
public class SDMEconomy extends EconomyAbstract {
  public static final String IDENTIFY = "PEBBLE_ECONOMY";

  @Override public String getIdentify() {
    return IDENTIFY;
  }


  @Override public boolean isPresent() {
    try {
      CurrencyHelper.getAllCurrencyKeys();
      CobbleUtils.LOGGER.info("SDM Economy isPresent Identifier: " + getIdentify());
      return true;
    } catch (NoClassDefFoundError | Exception e) {
      CobbleUtils.LOGGER.error("SDM Economy not found");
      return false;
    }
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
