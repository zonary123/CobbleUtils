package com.kingpixel.cobbleutils.util.economys;

import com.kingpixel.cobbleutils.CobbleUtils;
import lombok.NonNull;
import net.minecraft.server.network.ServerPlayerEntity;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract class for any economy provider.
 * Provides default implementations for common operations like setBalance, hasEnoughMoney, and transfer.
 */
public abstract class Economy {

  // -----------------------------
  // Required methods to implement
  // -----------------------------

  public abstract String getIdentify();

  public abstract boolean isPresent();

  /**
   * Get the balance of a player for a specific currency.
   */
  public abstract CompletableFuture<EconomyResponse> getBalance(UUID playerId, String currencyId);

  /**
   * Deposit money to a player's account.
   */
  public abstract CompletableFuture<EconomyResponse> deposit(UUID playerId, String currencyId, BigDecimal amount, String reason);

  /**
   * Withdraw money from a player's account.
   */
  public abstract CompletableFuture<EconomyResponse> withdraw(UUID playerId, String currencyId, BigDecimal amount, String reason);

  /**
   * Gets the display name of the currency.
   */
  public String getDisplayName(String currencyId) {
    return currencyId;
  }

  /**
   * Gets the symbol of the currency.
   */
  public String getSymbol(String currencyId) {
    return CobbleUtils.language.getDefaultSymbol();
  }

  /**
   * Gets the number of decimals for the currency.
   */
  public int getDecimals(String currencyId) {
    return 2;
  }

  /**
   * Format a monetary amount as a string.
   */
  public String format(BigDecimal money, String currency) {
    return CobbleUtils.config.getFormat(money);
  }

  // -----------------------------
  // Default implementations
  // -----------------------------

  /**
   * Sets a player's balance by calculating the difference between current balance and desired amount.
   */
  public CompletableFuture<EconomyResponse> setBalance(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return getBalance(playerId, currencyId).thenCompose(balanceResp -> {
      BigDecimal currentBalance = balanceResp.balance();
      BigDecimal difference = amount.subtract(currentBalance);
      if (difference.compareTo(BigDecimal.ZERO) > 0) {
        return deposit(playerId, currencyId, difference, reason);
      } else if (difference.compareTo(BigDecimal.ZERO) < 0) {
        return withdraw(playerId, currencyId, difference.abs(), reason);
      } else {
        return CompletableFuture.completedFuture(EconomyResponse.success(BigDecimal.ZERO, currentBalance));
      }
    });
  }

  /**
   * Checks if a player has enough money.
   */
  public CompletableFuture<EconomyResponse> hasEnoughMoney(UUID playerId, String currencyId, BigDecimal amount) {
    return getBalance(playerId, currencyId).thenApply(balanceResp -> {
      BigDecimal currentBalance = balanceResp.balance();
      boolean enough = currentBalance.compareTo(amount) >= 0;
      return enough ? EconomyResponse.success(BigDecimal.ZERO, currentBalance)
        : EconomyResponse.failure("Not enough money");
    });
  }

  /**
   * Transfers money from one player to another safely, with rollback in case of deposit failure.
   */
  public CompletableFuture<EconomyResponse> transfer(@NonNull UUID fromPlayerId,
                                                     @NonNull UUID toPlayerId,
                                                     @NonNull String currencyId,
                                                     @NonNull BigDecimal amount,
                                                     @NonNull String reason) {
    if (fromPlayerId.equals(toPlayerId)) {
      return CompletableFuture.completedFuture(EconomyResponse.failure("Cannot transfer to the same player"));
    }

    return hasEnoughMoney(fromPlayerId, currencyId, amount).thenCompose(hasMoneyResp -> {
      if (!hasMoneyResp.success()) {
        return CompletableFuture.completedFuture(EconomyResponse.failure("Source player does not have enough money"));
      }

      // Withdraw from source
      return withdraw(fromPlayerId, currencyId, amount, reason).thenCompose(withdrawResp -> {
        if (!withdrawResp.success()) {
          return CompletableFuture.completedFuture(EconomyResponse.failure("Failed to withdraw from source player"));
        }

        // Deposit to target
        return deposit(toPlayerId, currencyId, amount, reason).thenApply(depositResp -> {
          if (!depositResp.success()) {
            // Rollback: return money to source
            deposit(fromPlayerId, currencyId, amount, "rollback");
            return EconomyResponse.failure("Failed to deposit to target player, rollback applied");
          }

          // Success: return transfer info
          return EconomyResponse.success(amount, depositResp.balance());
        });
      });
    });
  }


  public CompletableFuture<ServerPlayerEntity> getPlayer(UUID playerId) {
    if (CobbleUtils.server == null) {
      return CompletableFuture.completedFuture(null);
    }
    return CobbleUtils.server.submit(() -> {
      var playerManager = CobbleUtils.server.getPlayerManager();
      return playerManager != null ? playerManager.getPlayer(playerId) : null;
    });
  }
}