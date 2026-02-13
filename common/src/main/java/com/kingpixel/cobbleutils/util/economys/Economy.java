package com.kingpixel.cobbleutils.util.economys;

import com.kingpixel.cobbleutils.CobbleUtils;
import lombok.Data;
import net.minecraft.server.network.ServerPlayerEntity;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 29/01/2025 4:06
 */
@Data
public abstract class Economy {

  public abstract String getIdentify();

  public abstract boolean isPresent();


  /**
   * deposit money to the player
   *
   * @param playerUuid The player to deposit money to
   * @param money      The amount of money to deposit
   * @param currency   The currency to depositç
   */
  @Deprecated(forRemoval = true)
  public abstract boolean deposit(UUID playerUuid, BigDecimal money, String currency);

  /**
   * withdraw money from the player
   *
   * @param playerUuid The player to withdraw money from
   * @param money      The amount of money to withdraw
   * @param currency   The currency to withdraw
   * @return true if the player has enough money
   */
  @Deprecated(forRemoval = true)
  public abstract boolean withdraw(UUID playerUuid, BigDecimal money, String currency);

  /**
   * get the balance of the player
   *
   * @param playerUuid The player to deposit money to
   * @param currency   The currency to deposit
   * @return The balance of the player
   */
  @Deprecated(forRemoval = true)
  public abstract BigDecimal getBalance(UUID playerUuid, String currency);


  /**
   * check if the player has enough money
   *
   * @param playerUuid  The player to withdraw money from
   * @param money       The amount of money to withdraw
   * @param currency    The currency to withdraw
   * @param removeMoney Remove the money if the player has enough
   * @return true if the player has enough money
   */
  @Deprecated(forRemoval = true)
  public boolean hasEnough(UUID playerUuid, BigDecimal money, String currency, boolean removeMoney) {
    if (this.getBalance(playerUuid, currency).compareTo(money) >= 0) {
      if (removeMoney) withdraw(playerUuid, money, currency);
      return true;
    }
    return false;
  }

  /**
   * get the symbol of the currency
   *
   * @param currency The currency to withdraw
   * @return The symbol of the currency
   */
  public String getSymbol(String currency) {
    return CobbleUtils.language.getDefaultSymbol();
  }

  /**
   * Format the money
   *
   * @param money    The amount of money to format
   * @param currency The currency to format
   * @return The formatted money
   */
  public abstract String format(BigDecimal money, String currency);

  /**
   * set the balance of the player
   *
   * @param playerUuid The player to set the balance
   * @param money      The amount of money to set
   * @param currency   The currency to set
   * @return true if the balance was set
   */
  @Deprecated(forRemoval = true)
  public abstract boolean setBalance(UUID playerUuid, BigDecimal money, String currency);

  /**
   * get the decimals of the currency
   *
   * @param currency The currency to get the decimals
   * @return The decimals of the currency
   */
  public abstract int getDecimals(String currency);


  public ServerPlayerEntity getPlayer(UUID uuid) {
    return CobbleUtils.server.getPlayerManager().getPlayer(uuid);
  }


  // =======================
  // New methods
  // =======================

  public abstract CompletableFuture<EconomyResult> getBalanceAsync(UUID playerId, String currencyId);

  public abstract CompletableFuture<EconomyResult> setBalance(UUID playerId, String currencyId, BigDecimal amount, String reason);

  public abstract CompletableFuture<EconomyResult> deposit(UUID playerId, String currencyId, BigDecimal amount, String reason);

  public abstract CompletableFuture<EconomyResult> withdraw(UUID playerId, String currencyId, BigDecimal amount, String reason);

  public abstract CompletableFuture<Boolean> hasBalance(UUID playerId, String currencyId, BigDecimal amount);

  public abstract String formatCurrency(String currencyId, BigDecimal amount);

  // =======================
  // Default business logic
  // =======================

  /**
   * Transfer an amount from one player to another safely.
   * <p>
   * - Rolls back if deposit fails.
   * - Handles exceptions internally and returns a failed EconomyResult on error.
   * - Validates amount > 0 and different players.
   * </p>
   */
  public CompletableFuture<EconomyResult> transfer(
    UUID fromPlayerId,
    UUID toPlayerId,
    String currencyId,
    BigDecimal amount,
    String reason
  ) {
    if (fromPlayerId.equals(toPlayerId)) return this.getBalanceAsync(fromPlayerId, currencyId)
      .thenApply(balance -> EconomyResult.success(balance.getBefore(), balance.getBefore(), "Self-transfer ignored"));

    if (amount == null || amount.signum() <= 0) return this.getBalanceAsync(fromPlayerId, currencyId)
      .thenApply(balance -> EconomyResult.fail("Invalid transfer amount", balance.getBefore()));


    Objects.requireNonNull(currencyId, "currencyId cannot be null");
    Objects.requireNonNull(reason, "reason cannot be null");

    return withdraw(fromPlayerId, currencyId, amount, reason)
      .thenCompose(withdrawResult -> {
        if (!withdrawResult.isSuccess()) return CompletableFuture.completedFuture(EconomyResult.fail(
          "Withdrawal failed: " + withdrawResult.getReason(),
          withdrawResult.getBefore()
        ));

        return deposit(toPlayerId, currencyId, amount, reason)
          .thenCompose(depositResult -> {
            if (!depositResult.isSuccess()) {
              return deposit(fromPlayerId, currencyId, amount, "Transfer rollback")
                .thenApply(r -> EconomyResult.fail(
                  "Deposit failed, rolled back: " + depositResult.getReason(),
                  withdrawResult.getBefore()
                ));
            }

            return CompletableFuture.completedFuture(EconomyResult.success(
              withdrawResult.getBefore(),
              withdrawResult.getBefore().subtract(amount),
              reason
            ));
          });
      })
      .handle((result, throwable) -> {
        if (throwable != null) {
          CobbleUtils.LOGGER.warn("Economy transfer failed: " + throwable);
          return EconomyResult.fail("Transfer exception occurred", BigDecimal.ZERO);
        }
        return result;
      });
  }
}
