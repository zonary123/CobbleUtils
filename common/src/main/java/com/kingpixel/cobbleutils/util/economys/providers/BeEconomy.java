package com.kingpixel.cobbleutils.util.economys.providers;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.economy.EconomyResult;
import com.kingpixel.cobbleutils.Model.economy.EconomyStatus;
import com.kingpixel.cobbleutils.util.economys.Economy;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.krripe.beconomy.api.BEconomy;
import org.krripe.beconomy.api.EconomyAPI;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@EqualsAndHashCode(callSuper = true)
@Getter
public class BeEconomy extends Economy {

  public static final String IDENTIFY = "BECONOMY";
  private static EconomyAPI service;

  @Override
  public String getIdentify() {
    return IDENTIFY;
  }

  @Override
  public boolean isPresent() {
    try {
      BEconomy.INSTANCE.initialize(CobbleUtils.server);
      service = BEconomy.INSTANCE.getAPI();
      return true;
    } catch (Exception e) {
      CobbleUtils.LOGGER.error("Failed to initialize BEconomy");
      return false;
    }
  }

  // =======================
  // Deprecated API
  // =======================

  @Override
  @Deprecated(forRemoval = true)
  public boolean deposit(UUID playerUuid, BigDecimal money, String currency) {
    service.addBalance(playerUuid, money, currency);
    return true;
  }

  @Override
  @Deprecated(forRemoval = true)
  public boolean withdraw(UUID playerUuid, BigDecimal money, String currency) {
    BigDecimal balance = getBalance(playerUuid, currency);
    if (balance.compareTo(money) < 0) return false;

    service.setBalance(playerUuid, balance.subtract(money), currency);
    return true;
  }

  @Override
  @Deprecated(forRemoval = true)
  public BigDecimal getBalance(UUID playerUuid, String currency) {
    return service.getBalance(playerUuid, currency);
  }

  @Override
  @Deprecated(forRemoval = true)
  public boolean setBalance(UUID playerUuid, BigDecimal money, String currency) {
    service.setBalance(playerUuid, money, currency);
    return true;
  }

  // =======================
  // Modern API
  // =======================

  @Override
  public CompletableFuture<EconomyResult> getBalanceAsync(UUID playerId, String currencyId) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        BigDecimal balance = getBalance(playerId, currencyId);

        return EconomyResult.success(
          balance,
          balance,
          BigDecimal.ZERO,
          "Balance fetched"
        );

      } catch (Exception e) {
        return EconomyResult.failure(
          EconomyStatus.ERROR,
          "Error getting balance: " + e.getMessage(),
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

        service.setBalance(playerId, amount, currencyId);

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
  public CompletableFuture<EconomyResult> deposit(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        BigDecimal before = getBalance(playerId, currencyId);

        service.addBalance(playerId, amount, currencyId);

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

        service.setBalance(playerId, before.subtract(amount), currencyId);

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
  public CompletableFuture<Boolean> hasEnoughMoney(UUID playerId, String currencyId, BigDecimal amount) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return service.hasEnoughFunds(playerId, amount, currencyId);
      } catch (Exception e) {
        CobbleUtils.LOGGER.error("Error checking balance: " + e.getMessage());
        e.printStackTrace();
        return false;
      }
    });
  }


  @Override
  public int getDecimals(String currency) {
    return CobbleUtils.config.getDecimals();
  }

  @Override
  public String format(BigDecimal money, String currency) {
    return CobbleUtils.config.getFormat(money) + " " + service.getCurrencySymbol(currency);
  }
}