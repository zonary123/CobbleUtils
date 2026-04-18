package com.kingpixel.cobbleutils.util.economys.providers;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.economys.Economy;
import com.kingpixel.cobbleutils.util.economys.EconomyResponse;
import com.kingpixel.ultraeconomy.api.UltraEconomyApi;
import com.kingpixel.ultraeconomy.config.Currencies;
import com.kingpixel.ultraeconomy.models.Currency;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

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

  @Override
  public CompletableFuture<EconomyResponse> getBalance(UUID playerId, String currencyId) {
    return CobbleUtils.ASYNC.supply(() -> {
      try {
        BigDecimal balance = UltraEconomyApi.getBalance(playerId, currencyId);
        if (balance == null) {
          return EconomyResponse.failure("Player balance not found");
        }
        return EconomyResponse.success(balance, balance);
      } catch (Exception e) {
        return EconomyResponse.failure("Error retrieving balance: " + e.getMessage());
      }
    });
  }

  @Override
  public CompletableFuture<EconomyResponse> deposit(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CobbleUtils.ASYNC.supply(() -> {
      try {
        boolean success = UltraEconomyApi.deposit(playerId, currencyId, amount);
        if (!success) return EconomyResponse.failure("Deposit failed");
        
        BigDecimal balance = UltraEconomyApi.getBalance(playerId, currencyId);
        return EconomyResponse.success(amount, balance != null ? balance : BigDecimal.ZERO);
      } catch (Exception e) {
        return EconomyResponse.failure("Error during deposit: " + e.getMessage());
      }
    });
  }

  @Override
  public CompletableFuture<EconomyResponse> withdraw(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CobbleUtils.ASYNC.supply(() -> {
      try {
        BigDecimal balance = UltraEconomyApi.getBalance(playerId, currencyId);
        if (balance == null || balance.compareTo(amount) < 0) return EconomyResponse.failure("Insufficient funds");

        boolean success = UltraEconomyApi.withdraw(playerId, currencyId, amount);
        if (!success) return EconomyResponse.failure("Withdraw failed");

        BigDecimal after = UltraEconomyApi.getBalance(playerId, currencyId);
        return EconomyResponse.success(amount, after != null ? after : BigDecimal.ZERO);
      } catch (Exception e) {
        return EconomyResponse.failure("Error during withdrawal: " + e.getMessage());
      }
    });
  }

  @Override
  public String format(BigDecimal money, String currency) {
    Currency curr = Currencies.getCurrency(currency);
    if (curr == null) return money.toString();
    return curr.format(money);
  }
}