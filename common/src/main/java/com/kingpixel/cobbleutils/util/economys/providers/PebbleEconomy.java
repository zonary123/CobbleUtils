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
        if (service.getEconomy().getBalance(playerId) < amount.doubleValue()) 
          return EconomyResponse.failure("Insufficient funds");

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
}