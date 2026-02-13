package com.kingpixel.cobbleutils.Model;

import com.kingpixel.cobbleutils.api.EconomyApi;
import com.kingpixel.cobbleutils.util.economys.Economy;
import com.kingpixel.cobbleutils.util.economys.EconomyResult;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 16/03/2025 3:20
 */
@Data
public class EconomyUse {
  private String EconomyId;
  private String currency;

  public EconomyUse(String EconomyId, String currency) {
    this.EconomyId = EconomyId;
    this.currency = currency;
  }


  public CompletableFuture<EconomyResult> getBalance(UUID playerUuid) {
    return EconomyApi.getBalanceAsync(playerUuid, this);
  }

  public CompletableFuture<EconomyResult> deposit(UUID playerUuid, BigDecimal amount, String reason) {
    return EconomyApi.deposit(playerUuid, this, amount, reason);
  }

  public CompletableFuture<EconomyResult> withdraw(UUID playerUuid, BigDecimal amount, String reason) {
    return EconomyApi.withdraw(playerUuid, this, amount, reason);
  }

  public CompletableFuture<EconomyResult> transfer(UUID fromPlayer, UUID toPlayer, BigDecimal amount, String reason) {
    return EconomyApi.transfer(fromPlayer, toPlayer, this, amount, reason);
  }

  public String format(BigDecimal amount) {
    Economy eco = EconomyApi.getEconomy(EconomyId);
    if (eco == null) return amount.toString();
    return eco.formatCurrency(currency, amount);
  }
}
