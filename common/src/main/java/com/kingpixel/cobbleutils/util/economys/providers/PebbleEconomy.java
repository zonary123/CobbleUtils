package com.kingpixel.cobbleutils.util.economys.providers;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.economy.EconomyResult;
import com.kingpixel.cobbleutils.Model.economy.EconomyStatus;
import com.kingpixel.cobbleutils.util.economys.Economy;
import lombok.EqualsAndHashCode;
import tech.sethi.pebbleseconomy.PebblesEconomyInitializer;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@EqualsAndHashCode(callSuper = true)
public class PebbleEconomy extends Economy {

  public static final String IDENTIFY = "PEBBLE_ECONOMY";

  private PebblesEconomyInitializer service;

  @Override
  public String getIdentify() {
    return IDENTIFY;
  }

  @Override
  public boolean isPresent() {
    try {
      service = PebblesEconomyInitializer.INSTANCE;
      service.getEconomy();
      return true;
    } catch (Exception e) {
      CobbleUtils.LOGGER.error("PebbleEconomy not present");
      return false;
    }
  }

  // =========================================================
  // Deprecated API
  // =========================================================

  @Override
  @Deprecated(forRemoval = true)
  public boolean deposit(UUID playerUuid, BigDecimal money, String currency) {
    service.getEconomy().deposit(playerUuid, money.doubleValue());
    return true;
  }

  @Override
  @Deprecated(forRemoval = true)
  public boolean withdraw(UUID playerUuid, BigDecimal money, String currency) {
    return service.getEconomy().withdraw(playerUuid, money.doubleValue());
  }

  @Override
  @Deprecated(forRemoval = true)
  public BigDecimal getBalance(UUID playerUuid, String currency) {
    return BigDecimal.valueOf(
      service.getEconomy().getBalance(playerUuid)
    );
  }

  @Override
  @Deprecated(forRemoval = true)
  public boolean setBalance(UUID playerUuid, BigDecimal money, String currency) {
    service.getEconomy().setBalance(playerUuid, money.doubleValue());
    return true;
  }

  // =========================================================
  // Modern Async API
  // =========================================================

  @Override
  public CompletableFuture<EconomyResult> getBalanceAsync(UUID playerId, String currencyId) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        BigDecimal balance = getBalance(playerId, currencyId);

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
        BigDecimal before = getBalance(playerId, currencyId);

        service.getEconomy().deposit(playerId, amount.doubleValue());

        BigDecimal after = getBalance(playerId, currencyId);

        return EconomyResult.success(
          before,
          after,
          amount,
          reason
        );

      } catch (Exception e) {
        return EconomyResult.failure(
          EconomyStatus.ERROR,
          "Error depositing: " + e.getMessage(),
          null
        );
      }
    });
  }

  @Override
  public CompletableFuture<EconomyResult> withdraw(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        BigDecimal before = getBalance(playerId, currencyId);

        if (before.compareTo(amount) < 0) {
          return EconomyResult.failure(
            EconomyStatus.INSUFFICIENT_FUNDS,
            "Insufficient funds",
            before
          );
        }

        boolean success = service.getEconomy()
          .withdraw(playerId, amount.doubleValue());

        if (!success) {
          return EconomyResult.failure(
            EconomyStatus.ERROR,
            "Withdrawal failed",
            before
          );
        }

        BigDecimal after = getBalance(playerId, currencyId);

        return EconomyResult.success(
          before,
          after,
          amount,
          reason
        );

      } catch (Exception e) {
        return EconomyResult.failure(
          EconomyStatus.ERROR,
          "Error withdrawing: " + e.getMessage(),
          null
        );
      }
    });
  }

  @Override
  public CompletableFuture<EconomyResult> setBalance(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        BigDecimal before = getBalance(playerId, currencyId);

        service.getEconomy()
          .setBalance(playerId, amount.doubleValue());

        BigDecimal after = getBalance(playerId, currencyId);

        return EconomyResult.success(
          before,
          after,
          amount,
          reason
        );

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
        return getBalance(playerId, currencyId)
          .compareTo(amount) >= 0;
      } catch (Exception e) {
        CobbleUtils.LOGGER.error("Error checking balance");
        return false;
      }
    });
  }

  // =========================================================
  // Formatting
  // =========================================================

  @Override
  public String format(BigDecimal money, String currency) {
    if (money == null) money = BigDecimal.ZERO;

    return CobbleUtils.language.getDefaultSymbol()
      + " "
      + CobbleUtils.config.getFormat(money);
  }

  @Override
  public int getDecimals(String currency) {
    return CobbleUtils.config.getDecimals();
  }
}