package com.kingpixel.cobbleutils.util.economys.v1;

import com.kingpixel.cobbleutils.CobbleUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.sixik.sdmeconomy.economyData.CurrencyPlayerData;
import net.sixik.sdmeconomy.utils.CurrencyHelper;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 16/03/2025 3:56
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SDMEconomy extends EconomyAbstract {
  public static final String IDENTIFY = "SDM_ECONOMY";

  public SDMEconomy() {
  }

  @Override
  public String getIdentify() {
    return IDENTIFY;
  }


  @Override
  public boolean isPresent() {
    CurrencyHelper.getAllCurrency();
    return true;
  }

  public CurrencyPlayerData.PlayerCurrency getPlayerData(UUID uuid, String currency) {
    //return CurrencyPlayerData.SERVER.getPlayerCurrency(uuid, currency).orElse(null);
    CobbleUtils.LOGGER.info("This economy is not supported. it have problems you can try to repair it in the github: https://github.com/zonary123/CobbleUtils/blob/1.21.1/common/src/main/java/com/kingpixel/cobbleutils/util/economys/SDMEconomy.java");
    return null;
  }

  @Override
  public boolean deposit(UUID playerUuid, BigDecimal money, String currency) {
    var playerData = getPlayerData(playerUuid, currency);
    if (playerData == null) {
      CobbleUtils.LOGGER.error("Player data not found for player: " + playerUuid + " and currency: " + currency);
      return false;
    }
    playerData.balance += money.doubleValue();
    return true;
  }

  @Override
  public boolean withdraw(UUID playerUuid, BigDecimal money, String currency) {
    var playerData = getPlayerData(playerUuid, currency);
    if (playerData == null) {
      CobbleUtils.LOGGER.error("Player data not found for player: " + playerUuid + " and currency: " + currency);
      return false;
    }
    if (playerData.balance < money.doubleValue()) return false;
    playerData.balance -= money.doubleValue();
    return true;
  }

  @Override
  public BigDecimal getBalance(UUID playerUuid, String currency) {
    return BigDecimal.valueOf(getPlayerData(playerUuid, currency).balance);
  }

  @Override
  public String format(BigDecimal money, String currency) {
    return CobbleUtils.language.getDefaultSymbol() + " " + CobbleUtils.config.getFormat(money);
  }

  @Override
  public boolean setBalance(UUID playerUuid, BigDecimal money, String currency) {
    var playerData = getPlayerData(playerUuid, currency);
    if (playerData == null) {
      CobbleUtils.LOGGER.error("Player data not found for player: " + playerUuid + " and currency: " + currency);
      return false;
    }
    playerData.balance = money.doubleValue();
    return true;
  }

  @Override
  public int getDecimals(String currency) {
    return CobbleUtils.config.getDecimals();
  }
}
