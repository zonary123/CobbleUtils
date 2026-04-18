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
    } catch (NoClassDefFoundError | Exception e) {
      CobbleUtils.LOGGER_RAW.error("Failed to initialize BEconomy");
      return false;
    }
  }

  // Wrapper async seguro
  private CompletableFuture<EconomyResponse> runAsync(Supplier<EconomyResponse> supplier) {
    return CobbleUtils.ASYNC.supply(() -> {
      try {
        return supplier.get();
      } catch (Exception e) {
        CobbleUtils.LOGGER_RAW.error("Economy operation failed");
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
  public CompletableFuture<EconomyResponse> deposit(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return runAsync(() -> {
      service.addBalance(playerId, amount, currencyId);
      return EconomyResponse.success(amount, service.getBalance(playerId, currencyId));
    });
  }

  @Override
  public CompletableFuture<EconomyResponse> withdraw(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return runAsync(() -> {
      BigDecimal before = service.getBalance(playerId, currencyId);
      if (before.compareTo(amount) < 0) return EconomyResponse.failure("Insufficient funds");
      service.setBalance(playerId, before.subtract(amount), currencyId);
      return EconomyResponse.success(amount, service.getBalance(playerId, currencyId));
    });
  }

  @Override
  public String getSymbol(String currency) {
    return service.getCurrencySymbol(currency);
  }
}