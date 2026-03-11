package com.kingpixel.cobbleutils.util.economys.providers;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.economys.Economy;
import com.kingpixel.cobbleutils.util.economys.EconomyResponse;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.krripe.beconomy.api.BEconomy;
import org.krripe.beconomy.api.EconomyAPI;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

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

  // Wrapper async seguro
  private CompletableFuture<EconomyResponse> runAsync(Supplier<EconomyResponse> supplier) {
    return CobbleUtils.ASYNC.supply(() -> {
      try {
        return supplier.get();
      } catch (Exception e) {
        CobbleUtils.LOGGER.error("Economy operation failed");
        return EconomyResponse.failure("Error: " + e.getMessage());
      }
    });
  }

  @Override
  public CompletableFuture<EconomyResponse> getBalance(UUID playerId, String currencyId) {
    return runAsync(() -> {
      BigDecimal balance = service.getBalance(playerId, currencyId);
      return EconomyResponse.success(balance, balance);
    });
  }

  @Override
  public CompletableFuture<EconomyResponse> setBalance(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return runAsync(() -> {
      BigDecimal before = service.getBalance(playerId, currencyId);
      BigDecimal diff = amount.subtract(before);

      if (diff.compareTo(BigDecimal.ZERO) > 0) {
        service.addBalance(playerId, diff, currencyId);
      } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
        service.setBalance(playerId, amount, currencyId);
      }

      BigDecimal after = service.getBalance(playerId, currencyId);
      return EconomyResponse.success(diff, after);
    });
  }

  @Override
  public CompletableFuture<EconomyResponse> deposit(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return runAsync(() -> {
      service.addBalance(playerId, amount, currencyId);
      BigDecimal after = service.getBalance(playerId, currencyId);
      return EconomyResponse.success(amount, after);
    });
  }

  @Override
  public CompletableFuture<EconomyResponse> withdraw(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return runAsync(() -> {
      BigDecimal before = service.getBalance(playerId, currencyId);
      if (before.compareTo(amount) < 0) return EconomyResponse.failure("Insufficient funds");
      service.setBalance(playerId, before.subtract(amount), currencyId);
      BigDecimal after = service.getBalance(playerId, currencyId);
      return EconomyResponse.success(amount, after);
    });
  }

  @Override
  public CompletableFuture<EconomyResponse> hasEnoughMoney(UUID playerId, String currencyId, BigDecimal amount) {
    return getBalance(playerId, currencyId).thenApply(resp ->
      resp.balance().compareTo(amount) >= 0
        ? EconomyResponse.success(BigDecimal.ZERO, resp.balance())
        : EconomyResponse.failure("Not enough money")
    );
  }

  @Override
  public CompletableFuture<EconomyResponse> transfer(@NonNull UUID fromPlayerId, @NonNull UUID toPlayerId,
                                                     @NonNull String currencyId, @NonNull BigDecimal amount,
                                                     @NonNull String reason) {
    if (fromPlayerId.equals(toPlayerId))
      return CompletableFuture.completedFuture(EconomyResponse.failure("Cannot transfer to the same player"));

    // Check funds y luego ejecutar todo de manera segura
    return hasEnoughMoney(fromPlayerId, currencyId, amount).thenCompose(hasMoneyResp -> {
      if (!hasMoneyResp.success())
        return CompletableFuture.completedFuture(EconomyResponse.failure("Source player lacks funds"));

      return withdraw(fromPlayerId, currencyId, amount, reason).thenCompose(withdrawResp -> {
        if (!withdrawResp.success())
          return CompletableFuture.completedFuture(EconomyResponse.failure("Failed to withdraw from source"));

        return deposit(toPlayerId, currencyId, amount, reason).thenCompose(depositResp -> {
          if (!depositResp.success()) {
            // rollback asincrónico
            return deposit(fromPlayerId, currencyId, amount, "rollback")
              .thenApply(rollbackResp -> EconomyResponse.failure("Failed to deposit to target, rollback applied"));
          }
          return CompletableFuture.completedFuture(EconomyResponse.success(amount, depositResp.balance()));
        });
      });
    });
  }

  @Override
  public String getSymbol(String currency) {
    return service.getCurrencySymbol(currency);
  }
}