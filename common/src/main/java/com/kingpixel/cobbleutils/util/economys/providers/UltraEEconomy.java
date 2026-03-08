package com.kingpixel.cobbleutils.util.economys.providers;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.economy.EconomyResult;
import com.kingpixel.cobbleutils.Model.economy.EconomyStatus;
import com.kingpixel.cobbleutils.util.economys.Economy;
import com.kingpixel.ultraeconomy.api.UltraEconomyApi;
import com.kingpixel.ultraeconomy.config.Currencies;
import com.kingpixel.ultraeconomy.models.Currency;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@EqualsAndHashCode(callSuper = true)
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

  // =========================================================
  // Deprecated API
  // =========================================================

  @Override
  @Deprecated(forRemoval = true)
  public boolean deposit(UUID playerUuid, BigDecimal money, String currency) {
    return UltraEconomyApi.deposit(playerUuid, currency, money);
  }

  @Override
  @Deprecated(forRemoval = true)
  public boolean withdraw(UUID playerUuid, BigDecimal money, String currency) {
    return UltraEconomyApi.withdraw(playerUuid, currency, money);
  }

  @Override
  @Deprecated(forRemoval = true)
  public BigDecimal getBalance(UUID playerUuid, String currency) {
    return UltraEconomyApi.getBalance(playerUuid, currency);
  }

  @Override
  @Deprecated(forRemoval = true)
  public boolean setBalance(UUID playerUuid, BigDecimal money, String currency) {
    return UltraEconomyApi.setBalance(playerUuid, currency, money) != null;
  }

  // =========================================================
  // Modern Async API
  // =========================================================

  @Override
  public CompletableFuture<EconomyResult> getBalanceAsync(UUID playerId, String currencyId) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        BigDecimal balance = UltraEconomyApi.getBalance(playerId, currencyId);

        if (balance == null) {
          return EconomyResult.failure(
            EconomyStatus.PLAYER_NOT_FOUND,
            "Balance not found",
            null
          );
        }

        return EconomyResult.success(
          balance,
          balance,
          BigDecimal.ZERO,
          "Balance retrieved"
        );

      } catch (Exception e) {
        return EconomyResult.failure(
          EconomyStatus.ERROR,
          "Error retrieving balance: " + e.getMessage(),
          null
        );
      }
    });
  }

  @Override
  public CompletableFuture<EconomyResult> deposit(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        BigDecimal before = UltraEconomyApi.getBalance(playerId, currencyId);

        boolean success = UltraEconomyApi.deposit(playerId, currencyId, amount);

        if (!success) {
          return EconomyResult.failure(
            EconomyStatus.ERROR,
            "Deposit failed",
            before
          );
        }

        BigDecimal after = UltraEconomyApi.getBalance(playerId, currencyId);

        return EconomyResult.success(before, after, amount, reason);
      } catch (Exception e) {
        return EconomyResult.failure(
          EconomyStatus.ERROR,
          "Error during deposit: " + e.getMessage(),
          null
        );
      }
    });
  }

  @Override
  public CompletableFuture<EconomyResult> withdraw(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        BigDecimal before = UltraEconomyApi.getBalance(playerId, currencyId);

        if (before.compareTo(amount) < 0) {
          return EconomyResult.failure(
            EconomyStatus.INSUFFICIENT_FUNDS,
            "Insufficient funds",
            before
          );
        }

        boolean success = UltraEconomyApi.withdraw(playerId, currencyId, amount);

        if (!success) {
          return EconomyResult.failure(
            EconomyStatus.ERROR,
            "Withdraw failed",
            before
          );
        }

        BigDecimal after = UltraEconomyApi.getBalance(playerId, currencyId);

        return EconomyResult.success(before, after, amount, reason);

      } catch (Exception e) {
        return EconomyResult.failure(
          EconomyStatus.ERROR,
          "Error during withdrawal: " + e.getMessage(),
          null
        );
      }
    });
  }

  @Override
  public CompletableFuture<EconomyResult> setBalance(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        BigDecimal before = UltraEconomyApi.getBalance(playerId, currencyId);

        BigDecimal success = UltraEconomyApi.setBalance(playerId, currencyId, amount);

        if (success == null || success.compareTo(amount) != 0) {
          return EconomyResult.failure(
            EconomyStatus.ERROR,
            "Set balance failed",
            before
          );
        }

        return EconomyResult.success(before, amount, amount, reason);

      } catch (Exception e) {
        return EconomyResult.failure(
          EconomyStatus.ERROR,
          "Error setting balance: " + e.getMessage(),
          null
        );
      }
    });
  }

  @Override
  public CompletableFuture<Boolean> hasEnoughMoney(UUID playerId, String currencyId, BigDecimal amount) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return UltraEconomyApi.hasEnoughBalance(playerId, currencyId, amount);
      } catch (Exception e) {
        return false;
      }
    });
  }

  // =========================================================
  // Formatting
  // =========================================================

  @Override
  public String format(BigDecimal money, String currency) {
    Currency curr = Currencies.getCurrency(currency);
    if (curr == null) return money.toString();
    return curr.format(money);
  }

  @Override
  public int getDecimals(String currency) {
    Currency curr = Currencies.getCurrency(currency);
    if (curr == null) return CobbleUtils.config.getDecimals();
    return curr.getDecimals();
  }
}