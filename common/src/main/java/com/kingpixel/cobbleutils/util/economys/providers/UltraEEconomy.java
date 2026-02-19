package com.kingpixel.cobbleutils.util.economys.providers;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.economys.Economy;
import com.kingpixel.cobbleutils.util.economys.EconomyResult;
import com.kingpixel.ultraeconomy.api.UltraEconomyApi;
import com.kingpixel.ultraeconomy.config.Currencies;
import com.kingpixel.ultraeconomy.models.Currency;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 28/09/2025 1:12
 */
public class UltraEEconomy extends Economy {
  public static final String IDENTIFY = "ULTRA_ECONOMY";

  @Override
  public String getIdentify() {
    return IDENTIFY;
  }

  @Override
  public boolean isPresent() {
    try {
      Class.forName("com.kingpixel.ultraeconomy.UltraEconomy");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  @Override
  public boolean deposit(UUID playerUuid, BigDecimal money, String currency) {
    return UltraEconomyApi.deposit(playerUuid, currency, money);
  }

  @Override
  public boolean withdraw(UUID playerUuid, BigDecimal money, String currency) {
    return UltraEconomyApi.withdraw(playerUuid, currency, money);
  }

  @Override
  public BigDecimal getBalance(UUID playerUuid, String currency) {
    return UltraEconomyApi.getBalance(playerUuid, currency);
  }

  @Override
  public CompletableFuture<EconomyResult> getBalanceAsync(UUID playerUuid, String currency) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        BigDecimal balance = UltraEconomyApi.getBalance(playerUuid, currency);
        if (balance == null) {
          return EconomyResult.fail("Failed to retrieve balance");
        }
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
        return EconomyResult.fail("Error during deposit: " + e.getMessage());
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
        return EconomyResult.fail("Error during withdrawal: " + e.getMessage());
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
        return false;
      }
    });
  }

  @Override
  public String format(BigDecimal money, String currency) {
    Currency curr = Currencies.getCurrency(currency);
    if (curr == null) return money.toString();
    return curr.format(money);
  }

  @Override
  public boolean setBalance(UUID playerUuid, BigDecimal money, String currency) {
    return UltraEconomyApi.setBalance(playerUuid, currency, money) != null;
  }

  @Override
  public int getDecimals(String currency) {
    Currency curr = Currencies.getCurrency(currency);
    if (curr == null) return CobbleUtils.config.getDecimals();
    return curr.getDecimals();
  }
}
