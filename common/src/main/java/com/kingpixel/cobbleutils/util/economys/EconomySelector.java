package com.kingpixel.cobbleutils.util.economys;

import com.kingpixel.cobbleutils.api.EconomyApi;
import com.kingpixel.cobbleutils.util.economys.providers.ImpactorEconomy;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 16/03/2025 3:20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EconomySelector {
  private Economy economy;
  @Builder.Default
  private String economyId = ImpactorEconomy.IDENTIFY;
  @Builder.Default
  private String currency = "impactor:dollars";

  public EconomySelector(String economy, String currency) {
    this.economyId = economy;
    this.currency = currency;
  }

  @NotNull
  private Economy getEconomy() {
    if (economy != null) return economy;
    economy = EconomyApi.getEconomy(economyId);
    if (economy == null) throw new IllegalStateException("Economy not found: " + economyId);
    return economy;
  }

  public CompletableFuture<EconomyResponse> getBalance(UUID playerUuid) {
    return getEconomy().getBalance(playerUuid, currency);
  }

  public CompletableFuture<EconomyResponse> deposit(UUID playerUuid, BigDecimal amount, @NonNull String reason) {
    return getEconomy().deposit(playerUuid, currency, amount, reason);
  }

  public CompletableFuture<EconomyResponse> withdraw(UUID playerUuid, BigDecimal amount, @NonNull String reason) {
    return getEconomy().withdraw(playerUuid, currency, amount, reason);
  }

  public CompletableFuture<EconomyResponse> transfer(UUID fromPlayer, UUID toPlayer, BigDecimal amount, @NonNull String reason) {
    return getEconomy().transfer(fromPlayer, toPlayer, currency, amount, reason);
  }

  public CompletableFuture<EconomyResponse> hasEnoughMoney(UUID playerId, BigDecimal amount) {
    return getEconomy().hasEnoughMoney(playerId, currency, amount);
  }

  public CompletableFuture<EconomyResponse> setBalance(UUID playerId, BigDecimal amount, String reason) {
    return getEconomy().setBalance(playerId, currency, amount, reason);
  }

  public String format(BigDecimal amount) {
    return getEconomy().format(amount, currency);
  }
}
