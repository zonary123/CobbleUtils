package com.kingpixel.cobbleutils.util.economys;

import com.kingpixel.cobbleutils.CobbleUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.beconomy.api.BEconomy;
import org.beconomy.api.EconomyAPI;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 16/03/2025 3:35
 */
@EqualsAndHashCode(callSuper = true) @Data
public class BeEconomy extends EconomyAbstract {
  public static final String IDENTIFY = "BECONOMY";
  private static EconomyAPI service;

  public BeEconomy() {
  }

  @Override public String getIdentify() {
    return IDENTIFY;
  }

  @Override public boolean isPresent() {
    BEconomy.INSTANCE.initialize(CobbleUtils.server);
    service = BEconomy.INSTANCE.getAPI();
    return true;
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
    return money.setScale(getDecimals(currency), RoundingMode.UNNECESSARY) + " " + service.getCurrencySymbol(currency);
  }

  @Override public boolean setBalance(UUID playerUuid, BigDecimal money, String currency) {
    service.setBalance(playerUuid, money, currency);
    return true;
  }

  @Override public int getDecimals(String currency) {
    return CobbleUtils.config.getDecimals();
  }
}
