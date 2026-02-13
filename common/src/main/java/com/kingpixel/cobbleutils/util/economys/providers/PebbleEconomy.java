package com.kingpixel.cobbleutils.util.economys.providers;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.economys.Economy;
import com.kingpixel.cobbleutils.util.economys.EconomyResult;
import lombok.Data;
import lombok.EqualsAndHashCode;
import tech.sethi.pebbleseconomy.PebblesEconomyInitializer;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 16/03/2025 3:51
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PebbleEconomy extends Economy {
  public static final String IDENTIFY = "PEBBLE_ECONOMY";
  private PebblesEconomyInitializer service;

  public PebbleEconomy() {
  }

  @Override
  public String getIdentify() {
    return IDENTIFY;
  }

  @Override
  public boolean isPresent() {
    service = PebblesEconomyInitializer.INSTANCE;
    return true;
  }

  @Override
  public boolean deposit(UUID playerUuid, BigDecimal money, String currency) {
    service.getEconomy().deposit(playerUuid, money.doubleValue());
    return true;
  }

  @Override
  public boolean withdraw(UUID playerUuid, BigDecimal money, String currency) {
    return service.getEconomy().withdraw(playerUuid, money.doubleValue());
  }

  @Override
  public BigDecimal getBalance(UUID playerUuid, String currency) {
    return BigDecimal.valueOf(service.getEconomy().getBalance(playerUuid));
  }

  @Override
  public CompletableFuture<EconomyResult> getBalanceAsync(UUID playerUuid, String currency) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        BigDecimal balance = getBalance(playerUuid, currency);
        return EconomyResult.success(balance, balance, "Balance retrieved successfully");
      } catch (Exception e) {
        return EconomyResult.fail("Error retrieving balance: " + e.getMessage());
      }
    });
  }

  @Override
  public CompletableFuture<EconomyResult> setBalance(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        boolean success = setBalance(playerId, amount, currencyId);
        if (success) {
          return EconomyResult.success(amount, amount, "Balance set successfully");
        } else {
          return EconomyResult.fail("Failed to set balance");
        }
      } catch (Exception e) {
        return EconomyResult.fail("Error setting balance: " + e.getMessage());
      }
    });
  }

  @Override
  public CompletableFuture<EconomyResult> deposit(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        boolean success = deposit(playerId, amount, currencyId);
        if (success) {
          return EconomyResult.success(amount, amount, "Deposit successful");
        } else {
          return EconomyResult.fail("Failed to deposit");
        }
      } catch (Exception e) {
        return EconomyResult.fail("Error depositing: " + e.getMessage());
      }
    });
  }

  @Override
  public CompletableFuture<EconomyResult> withdraw(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        boolean success = withdraw(playerId, amount, currencyId);
        if (success) {
          return EconomyResult.success(amount, amount, "Withdrawal successful");
        } else {
          return EconomyResult.fail("Failed to withdraw");
        }
      } catch (Exception e) {
        return EconomyResult.fail("Error withdrawing: " + e.getMessage());
      }
    });
  }

  @Override
  public CompletableFuture<Boolean> hasBalance(UUID playerId, String currencyId, BigDecimal amount) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        BigDecimal balance = getBalance(playerId, currencyId);
        return balance.compareTo(amount) >= 0;
      } catch (Exception e) {
        CobbleUtils.LOGGER.error("Error checking balance: " + e.getMessage());
        return false;
      }
    });
  }

  @Override
  public String formatCurrency(String currencyId, BigDecimal amount) {
    return "";
  }

  @Override
  public String format(BigDecimal money, String currency) {
    return CobbleUtils.language.getDefaultSymbol() + " " + CobbleUtils.config.getFormat(money);
  }

  @Override
  public boolean setBalance(UUID playerUuid, BigDecimal money, String currency) {
    service.getEconomy().setBalance(playerUuid, money.doubleValue());
    return true;
  }

  @Override
  public int getDecimals(String currency) {
    return CobbleUtils.config.getDecimals();
  }
}
