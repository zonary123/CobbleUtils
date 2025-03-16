package com.kingpixel.cobbleutils.util.economys;

import com.kingpixel.cobbleutils.CobbleUtils;
import org.beconomy.api.BEconomy;
import org.beconomy.api.EconomyAPI;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 16/03/2025 3:35
 */
public class BeEconomy extends EconomyAbstract {
  public static final String IDENTIFY = "BECONOMY";
  private static EconomyAPI service;


  @Override public String getIdentify() {
    return IDENTIFY;
  }

  @Override public boolean isPresent() {
    try {
      BEconomy.INSTANCE.initialize(CobbleUtils.server);
      service = BEconomy.INSTANCE.getAPI();
      CobbleUtils.LOGGER.info("BlanketEconomy found Identifier: " + getIdentify());
      return true;
    } catch (NoClassDefFoundError | Exception e) {
      CobbleUtils.LOGGER.error("BlanketEconomy not found");
      return false;
    }
  }

  @Override public boolean deposit(UUID playerUuid, BigDecimal money, String currency) {
    service.addBalance(playerUuid, money, currency);
    return true;
  }

  @Override public boolean withdraw(UUID playerUuid, BigDecimal money, String currency) {
    BigDecimal balance = getBalance(playerUuid, currency);
    return setBalance(playerUuid, balance.subtract(money), currency);
  }

  @Override public BigDecimal getBalance(UUID playerUuid, String currency) {
    return service.getBalance(playerUuid, currency);
  }

  @Override public String format(BigDecimal money, String currency) {
    return money + " " + getSymbol(currency);
  }

  @Override public boolean setBalance(UUID playerUuid, BigDecimal money, String currency) {
    service.setBalance(playerUuid, money, currency);
    return true;
  }
}
