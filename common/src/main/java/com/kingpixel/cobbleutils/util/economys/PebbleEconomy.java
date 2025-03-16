package com.kingpixel.cobbleutils.util.economys;

import com.kingpixel.cobbleutils.CobbleUtils;
import tech.sethi.pebbleseconomy.PebblesEconomyInitializer;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 16/03/2025 3:51
 */
public class PebbleEconomy extends EconomyAbstract {
  public static final String IDENTIFY = "PEBBLE_ECONOMY";
  private PebblesEconomyInitializer service;

  @Override public String getIdentify() {
    return IDENTIFY;
  }

  @Override public boolean isPresent() {
    try {
      service = PebblesEconomyInitializer.INSTANCE;
      CobbleUtils.LOGGER.info("PebbleEconomy isPresent Identifier: " + getIdentify());
      return true;
    } catch (NoClassDefFoundError | Exception e) {
      CobbleUtils.LOGGER.error("Pebble Economy not found");
      return false;
    }
  }

  @Override public boolean deposit(UUID playerUuid, BigDecimal money, String currency) {
    service.getEconomy().deposit(playerUuid, money.doubleValue());
    return true;
  }

  @Override public boolean withdraw(UUID playerUuid, BigDecimal money, String currency) {
    return service.getEconomy().withdraw(playerUuid, money.doubleValue());
  }

  @Override public BigDecimal getBalance(UUID playerUuid, String currency) {
    return BigDecimal.valueOf(service.getEconomy().getBalance(playerUuid));
  }

  @Override public String format(BigDecimal money, String currency) {
    return CobbleUtils.language.getDefaultSymbol() + " " + money;
  }

  @Override public boolean setBalance(UUID playerUuid, BigDecimal money, String currency) {
    service.getEconomy().setBalance(playerUuid, money.doubleValue());
    return true;
  }
}
