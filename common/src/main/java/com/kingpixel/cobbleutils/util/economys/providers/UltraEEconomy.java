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
        BigDecimal after = UltraEconomyApi.getBalance(playerId, currencyId);

        return success
          ? EconomyResponse.success(amount, after)
          : EconomyResponse.failure("Deposit failed");
      } catch (Exception e) {
        return EconomyResponse.failure("Error during deposit: " + e.getMessage());
      }
    });
  }

  @Override
  public CompletableFuture<EconomyResponse> withdraw(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CobbleUtils.ASYNC.supply(() -> {
      try {
        BigDecimal before = UltraEconomyApi.getBalance(playerId, currencyId);
        if (before.compareTo(amount) < 0) return EconomyResponse.failure("Insufficient funds");

        boolean success = UltraEconomyApi.withdraw(playerId, currencyId, amount);
        BigDecimal after = UltraEconomyApi.getBalance(playerId, currencyId);

        return success
          ? EconomyResponse.success(amount, after)
          : EconomyResponse.failure("Withdraw failed");
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
      if (!hasMoneyResp.success()) {
        return CompletableFuture.completedFuture(EconomyResponse.failure("Source player does not have enough money"));
      }

      return withdraw(fromPlayerId, currencyId, amount, reason).thenCompose(withdrawResp -> {
        if (!withdrawResp.success()) {
          return CompletableFuture.completedFuture(EconomyResponse.failure("Failed to withdraw from source player"));
        }

        return deposit(toPlayerId, currencyId, amount, reason).thenApply(depositResp -> {
          if (!depositResp.success()) {
            // Rollback
            deposit(fromPlayerId, currencyId, amount, "rollback");
            return EconomyResponse.failure("Failed to deposit to target player, rollback applied");
          }
          return EconomyResponse.success(amount, depositResp.balance());
        });
      });
    });
  }

  @Override
  public String format(BigDecimal money, String currency) {
    Currency curr = Currencies.getCurrency(currency);
    if (curr == null) return money.toString();
    return curr.format(money);
  }
}