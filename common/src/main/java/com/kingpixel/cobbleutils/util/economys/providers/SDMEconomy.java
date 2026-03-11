package com.kingpixel.cobbleutils.util.economys.providers;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.economys.Economy;
import com.kingpixel.cobbleutils.util.economys.EconomyResponse;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import net.sixik.sdmeconomy.economyData.CurrencyPlayerData;
import net.sixik.sdmeconomy.utils.CurrencyHelper;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@EqualsAndHashCode(callSuper = true)
public class SDMEconomy extends Economy {

  public static final String IDENTIFY = "SDM_ECONOMY";

  @Override
  public String getIdentify() {
    return IDENTIFY;
  }

  @Override
  public boolean isPresent() {
    try {
      CurrencyHelper.getAllCurrency();
      return true;
    } catch (Exception e) {
      CobbleUtils.LOGGER.error("SDM Economy not present");
      return false;
    }
  }

  // =========================================================
  // Internal Access
  // =========================================================

  private CurrencyPlayerData.PlayerCurrency getPlayerData(UUID uuid, String currency) {
    CobbleUtils.LOGGER.warn(
      "SDM Economy integration may be unstable. Repository: https://github.com/zonary123/CobbleUtils"
    );
    return null; // Mantengo comportamiento actual
  }

  // =========================================================
  // Modern Async API
  // =========================================================

  @Override
  public CompletableFuture<EconomyResponse> getBalance(UUID playerId, String currencyId) {
    return CobbleUtils.ASYNC.supply(() -> {
      try {
        CurrencyPlayerData.PlayerCurrency data = getPlayerData(playerId, currencyId);
        if (data == null) return EconomyResponse.failure("Player data not found");

        BigDecimal balance = BigDecimal.valueOf(data.balance);
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
        CurrencyPlayerData.PlayerCurrency data = getPlayerData(playerId, currencyId);
        if (data == null || amount.signum() <= 0)
          return EconomyResponse.failure("Deposit failed: player not found or invalid amount");

        BigDecimal before = BigDecimal.valueOf(data.balance);
        data.balance += amount.doubleValue();
        BigDecimal after = BigDecimal.valueOf(data.balance);

        return EconomyResponse.success(amount, after);
      } catch (Exception e) {
        return EconomyResponse.failure("Error during deposit: " + e.getMessage());
      }
    });
  }

  @Override
  public CompletableFuture<EconomyResponse> withdraw(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CobbleUtils.ASYNC.supply(() -> {
      try {
        CurrencyPlayerData.PlayerCurrency data = getPlayerData(playerId, currencyId);
        if (data == null) return EconomyResponse.failure("Player not found");

        BigDecimal before = BigDecimal.valueOf(data.balance);
        if (before.compareTo(amount) < 0) return EconomyResponse.failure("Insufficient funds");

        data.balance -= amount.doubleValue();
        BigDecimal after = BigDecimal.valueOf(data.balance);

        return EconomyResponse.success(amount, after);
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
            // Rollback
            deposit(fromPlayerId, currencyId, amount, "rollback");
            return EconomyResponse.failure("Failed to deposit to target player, rollback applied");
          }
          return EconomyResponse.success(amount, depositResp.balance());
        });
      });
    });
  }
}