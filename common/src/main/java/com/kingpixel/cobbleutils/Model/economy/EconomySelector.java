package com.kingpixel.cobbleutils.Model.economy;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.api.EconomyApi;
import com.kingpixel.cobbleutils.util.economys.Economy;
import lombok.Data;
import lombok.NonNull;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 16/03/2025 3:20
 */
@Data
public class EconomySelector {
  private String economyId;
  private String currency;

  public EconomySelector(String economy, String currency) {
    this.economyId = economy;
    this.currency = currency;
  }

  private Economy getEconomy() {
    Economy eco = EconomyApi.getEconomy(economyId);
    if (eco == null) CobbleUtils.LOGGER.info("Economy " + economyId + " not found, using default economy");
    return eco;
  }

  public CompletableFuture<EconomyResult> getBalance(UUID playerUuid) {
    return getEconomy().getBalanceAsync(playerUuid, currency);
  }

  public CompletableFuture<EconomyResult> deposit(UUID playerUuid, BigDecimal amount, @NonNull String reason) {
    return getEconomy().deposit(playerUuid, currency, amount, reason);
  }

  public CompletableFuture<EconomyResult> withdraw(UUID playerUuid, BigDecimal amount, @NonNull String reason) {
    return getEconomy().withdraw(playerUuid, currency, amount, reason);
  }

  public CompletableFuture<EconomyTransferResult> transfer(UUID fromPlayer, UUID toPlayer, BigDecimal amount, @NonNull String reason) {
    return getEconomy().transfer(fromPlayer, toPlayer, currency, amount, reason);
  }

  public String format(BigDecimal amount) {
    return getEconomy().format(amount, currency);
  }

  public CompletableFuture<Boolean> hasEnoughMoney(UUID playerId, BigDecimal amount) {
    return getEconomy().hasEnoughMoney(playerId, currency, amount);
  }

  public CompletableFuture<EconomyResult> setBalance(UUID playerId, BigDecimal amount, String reason) {
    return getEconomy().setBalance(playerId, currency, amount, reason);
  }
}
