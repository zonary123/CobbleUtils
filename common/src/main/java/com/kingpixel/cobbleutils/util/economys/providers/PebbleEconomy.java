package com.kingpixel.cobbleutils.util.economys.providers;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.economys.Economy;
import com.kingpixel.cobbleutils.util.economys.EconomyResponse;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
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
      CobbleUtils.LOGGER_RAW.error("PebbleEconomy not present");
      return false;
    }
  }

  // =========================================================
  // Modern Async API
  // =========================================================

  @Override
  public CompletableFuture<EconomyResponse> getBalance(UUID playerId, String currencyId) {
    return CobbleUtils.ASYNC.supply(() -> {
      try {
        double balance = service.getEconomy().getBalance(playerId);
        BigDecimal bal = BigDecimal.valueOf(balance);
        return EconomyResponse.success(bal, bal);
      } catch (Exception e) {
        return EconomyResponse.failure("Error retrieving balance: " + e.getMessage());
      }
    });
  }

  @Override
  public CompletableFuture<EconomyResponse> deposit(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CobbleUtils.ASYNC.supply(() -> {
      try {
        double before = service.getEconomy().getBalance(playerId);
        service.getEconomy().deposit(playerId, amount.doubleValue());
        double after = service.getEconomy().getBalance(playerId);

        return EconomyResponse.success(amount, BigDecimal.valueOf(after));
      } catch (Exception e) {
        return EconomyResponse.failure("Error during deposit: " + e.getMessage());
      }
    });
  }

  @Override
  public CompletableFuture<EconomyResponse> withdraw(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CobbleUtils.ASYNC.supply(() -> {
      try {
        double before = service.getEconomy().getBalance(playerId);
        if (before < amount.doubleValue()) return EconomyResponse.failure("Insufficient funds");

        boolean success = service.getEconomy().withdraw(playerId, amount.doubleValue());
        double after = service.getEconomy().getBalance(playerId);

        return success
          ? EconomyResponse.success(amount, BigDecimal.valueOf(after))
          : EconomyResponse.failure("Withdrawal failed");
      } catch (Exception e) {
        return EconomyResponse.failure("Error during withdrawal: " + e.getMessage());
      }
    });
  }

  @Override
  public CompletableFuture<EconomyResponse> setBalance(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return getBalance(playerId, currencyId).thenCompose(balanceResp -> {
      BigDecimal current = balanceResp.balance();
      BigDecimal diff = amount.subtract(current);

      if (diff.compareTo(BigDecimal.ZERO) > 0) {
        return deposit(playerId, currencyId, diff, reason);
      } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
        return withdraw(playerId, currencyId, diff.abs(), reason);
      } else {
        return CompletableFuture.completedFuture(EconomyResponse.success(BigDecimal.ZERO, current));
      }
    });
  }

  @Override
  public CompletableFuture<EconomyResponse> hasEnoughMoney(UUID playerId, String currencyId, BigDecimal amount) {
    return getBalance(playerId, currencyId).thenApply(balanceResp -> {
      BigDecimal current = balanceResp.balance();
      return current.compareTo(amount) >= 0
        ? EconomyResponse.success(BigDecimal.ZERO, current)
        : EconomyResponse.failure("Not enough money");
    });
  }

  @Override
  public CompletableFuture<EconomyResponse> transfer(@NonNull UUID fromPlayerId, @NonNull UUID toPlayerId,
                                                     @NonNull String currencyId, @NonNull BigDecimal amount,
                                                     @NonNull String reason) {
    if (fromPlayerId.equals(toPlayerId)) {
      return CompletableFuture.completedFuture(EconomyResponse.failure("Cannot transfer to the same player"));
    }

    return hasEnoughMoney(fromPlayerId, currencyId, amount).thenCompose(hasMoneyResp -> {
      if (!hasMoneyResp.success())
        return CompletableFuture.completedFuture(EconomyResponse.failure("Source player does not have enough money"));

      return withdraw(fromPlayerId, currencyId, amount, reason).thenCompose(withdrawResp -> {
        if (!withdrawResp.success())
          return CompletableFuture.completedFuture(EconomyResponse.failure("Failed to withdraw from source player"));

        return deposit(toPlayerId, currencyId, amount, reason).thenApply(depositResp -> {
          if (!depositResp.success()) {
            deposit(fromPlayerId, currencyId, amount, "rollback");
            return EconomyResponse.failure("Failed to deposit to target player, rollback applied");
          }
          return EconomyResponse.success(amount, depositResp.balance());
        });
      });
    });
  }
}