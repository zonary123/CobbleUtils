package com.kingpixel.cobbleutils.util.economys;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.economy.EconomyResult;
import com.kingpixel.cobbleutils.Model.economy.EconomyStatus;
import com.kingpixel.cobbleutils.Model.economy.EconomyTransferResult;
import lombok.Data;
import lombok.NonNull;
import net.minecraft.server.network.ServerPlayerEntity;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Data
public abstract class Economy {

  public abstract String getIdentify();

  public abstract boolean isPresent();

  // =======================
  // Deprecated API
  // =======================

  @Deprecated(forRemoval = true)
  public abstract boolean deposit(UUID playerUuid, BigDecimal money, String currency);

  @Deprecated(forRemoval = true)
  public abstract boolean withdraw(UUID playerUuid, BigDecimal money, String currency);

  @Deprecated(forRemoval = true)
  public abstract BigDecimal getBalance(UUID playerUuid, String currency);

  @Deprecated(forRemoval = true)
  public boolean hasEnough(UUID playerUuid, BigDecimal money, String currency, boolean removeMoney) {
    BigDecimal balance = getBalance(playerUuid, currency);
    if (balance.compareTo(money) >= 0) {
      if (removeMoney) withdraw(playerUuid, money, currency);
      return true;
    }
    return false;
  }

  @Deprecated(forRemoval = true)
  public abstract boolean setBalance(UUID playerUuid, BigDecimal money, String currency);

  // =======================
  // Modern API
  // =======================

  public abstract CompletableFuture<EconomyResult> getBalanceAsync(UUID playerId, String currencyId);

  public abstract CompletableFuture<EconomyResult> setBalance(UUID playerId, String currencyId, BigDecimal amount, String reason);

  public abstract CompletableFuture<EconomyResult> deposit(UUID playerId, String currencyId, BigDecimal amount, String reason);

  public abstract CompletableFuture<EconomyResult> withdraw(UUID playerId, String currencyId, BigDecimal amount, String reason);

  public abstract CompletableFuture<Boolean> hasEnoughMoney(UUID playerId, String currencyId, BigDecimal amount);

  public abstract int getDecimals(String currency);

  public abstract String format(BigDecimal money, String currency);

  // =======================
  // Default utilities
  // =======================

  public String getSymbol(String currency) {
    return CobbleUtils.language.getDefaultSymbol();
  }

  public ServerPlayerEntity getPlayer(UUID uuid) {
    return CobbleUtils.server.getPlayerManager().getPlayer(uuid);
  }

  // =======================
  // Transfer logic
  // =======================

  public CompletableFuture<EconomyTransferResult> transfer(
    @NonNull UUID fromPlayerId,
    @NonNull UUID toPlayerId,
    @NonNull String currencyId,
    @NonNull BigDecimal amount,
    @NonNull String reason
  ) {
    if (fromPlayerId.equals(toPlayerId)) {
      return getBalanceAsync(fromPlayerId, currencyId)
        .thenApply(balance ->
          EconomyTransferResult.failure(
            EconomyStatus.INVALID_AMOUNT,
            fromPlayerId,
            toPlayerId,
            "Cannot transfer to yourself",
            balance.getBefore(),
            balance.getBefore()
          )
        );
    }

    if (amount.signum() <= 0) {
      return getBalanceAsync(fromPlayerId, currencyId)
        .thenApply(balance ->
          EconomyTransferResult.failure(
            EconomyStatus.INVALID_AMOUNT,
            fromPlayerId,
            toPlayerId,
            "Invalid transfer amount",
            balance.getBefore(),
            null
          )
        );
    }

    return withdraw(fromPlayerId, currencyId, amount, reason)
      .thenCompose(withdrawResult -> {

        if (!withdrawResult.isSuccess()) {
          return CompletableFuture.completedFuture(
            EconomyTransferResult.failure(
              withdrawResult.getStatus(),
              fromPlayerId,
              toPlayerId,
              "Withdrawal failed: " + withdrawResult.getReason(),
              withdrawResult.getBefore(),
              null
            )
          );
        }

        return deposit(toPlayerId, currencyId, amount, reason)
          .thenCompose(depositResult -> {

            if (!depositResult.isSuccess()) {

              return deposit(fromPlayerId, currencyId, amount, "Transfer rollback")
                .thenApply(rollback ->
                  EconomyTransferResult.failure(
                    depositResult.getStatus(),
                    fromPlayerId,
                    toPlayerId,
                    "Deposit failed, rolled back: " + depositResult.getReason(),
                    withdrawResult.getBefore(),
                    depositResult.getBefore()
                  )
                );
            }

            return CompletableFuture.completedFuture(
              EconomyTransferResult.success(
                fromPlayerId,
                toPlayerId,
                withdrawResult.getBefore(),
                withdrawResult.getAfter(),
                depositResult.getBefore(),
                depositResult.getAfter(),
                amount,
                reason
              )
            );
          });
      })
      .exceptionally(throwable -> {
        CobbleUtils.LOGGER.error("Economy transfer failed");
        throwable.printStackTrace();
        return EconomyTransferResult.failure(
          EconomyStatus.ERROR,
          fromPlayerId,
          toPlayerId,
          "Transfer exception: " + throwable.getMessage(),
          null,
          null
        );
      });
  }
}