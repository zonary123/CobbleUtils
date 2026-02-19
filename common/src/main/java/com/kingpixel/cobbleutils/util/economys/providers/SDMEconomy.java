package com.kingpixel.cobbleutils.util.economys.providers;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.economys.Economy;
import com.kingpixel.cobbleutils.util.economys.EconomyResult;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.sixik.sdmeconomy.economyData.CurrencyPlayerData;
import net.sixik.sdmeconomy.utils.CurrencyHelper;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 16/03/2025 3:56
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SDMEconomy extends Economy {
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
    var playerData = getPlayerData(playerUuid, currency);
    if (playerData == null) {
      CobbleUtils.LOGGER.error("Player data not found for player: " + playerUuid + " and currency: " + currency);
      return BigDecimal.ZERO;
    }
    return BigDecimal.valueOf(playerData.balance);
  }

  @Override
  public CompletableFuture<EconomyResult> getBalanceAsync(UUID playerUuid, String currency) {
    return CompletableFuture.supplyAsync(() -> {
      BigDecimal balance = getBalance(playerUuid, currency);
      return EconomyResult.success(balance, balance, "");
    });
  }

  @Override
  public CompletableFuture<EconomyResult> setBalance(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CompletableFuture.supplyAsync(() -> {
      boolean success = setBalance(playerId, amount, currencyId);
      return success ? EconomyResult.success(amount, amount, "Balance set successfully") : EconomyResult.fail("Failed to set balance");
    });
  }

  @Override
  public CompletableFuture<EconomyResult> deposit(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CompletableFuture.supplyAsync(() -> {
      boolean success = deposit(playerId, amount, currencyId);
      return success ? EconomyResult.success(amount, amount, "Deposit successful") : EconomyResult.fail("Failed to deposit");
    });
  }

  @Override
  public CompletableFuture<EconomyResult> withdraw(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CompletableFuture.supplyAsync(() -> {
      boolean success = withdraw(playerId, amount, currencyId);
      return success ? EconomyResult.success(amount, amount, "Withdraw successful") : EconomyResult.fail("Failed to withdraw");
    });
  }

  @Override
  public CompletableFuture<Boolean> hasBalance(UUID playerId, String currencyId, BigDecimal amount) {
    return CompletableFuture.supplyAsync(() -> {
      BigDecimal balance = getBalance(playerId, currencyId);
      return balance.compareTo(amount) >= 0;
    });
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
