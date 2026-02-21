package com.kingpixel.cobbleutils.Model.economy;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.api.EconomyApi;
import com.kingpixel.cobbleutils.util.economys.Economy;
import com.kingpixel.cobbleutils.util.economys.EconomyResult;
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
  private String economy;
  private String currency;

  public EconomySelector(String economy, String currency) {
    this.economy = economy;
    this.currency = currency;
  }

  public CompletableFuture<EconomyResult> getBalance(UUID playerUuid) {
    return EconomyApi.getBalanceAsync(playerUuid, this);
  }

  public CompletableFuture<EconomyResult> deposit(UUID playerUuid, BigDecimal amount, @NonNull String reason) {
    return EconomyApi.deposit(playerUuid, this, amount, reason);
  }

  public CompletableFuture<EconomyResult> withdraw(UUID playerUuid, BigDecimal amount, @NonNull String reason) {
    return EconomyApi.withdraw(playerUuid, this, amount, reason);
  }

  public CompletableFuture<EconomyResult> transfer(UUID fromPlayer, UUID toPlayer, BigDecimal amount, @NonNull String reason) {
    return EconomyApi.transfer(fromPlayer, toPlayer, this, amount, reason);
  }

  public String format(BigDecimal amount) {
    Economy eco = EconomyApi.getEconomy(economy);
    if (eco == null) {
      CobbleUtils.LOGGER.info("Economy " + economy + " not found, using default formatting for amount: " + amount);
      return amount.toString();
    }
    return eco.format(amount, currency);
  }
}
