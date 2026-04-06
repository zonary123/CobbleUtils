package com.kingpixel.cobbleutils.util.economys.providers;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.economys.Economy;
import com.kingpixel.cobbleutils.util.economys.EconomyResponse;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@EqualsAndHashCode(callSuper = true)
public class VaultEconomy extends Economy {

  public static final String IDENTIFY = "VAULT";

  private net.milkbowl.vault.economy.Economy service;

  @Override
  public String getIdentify() {
    return IDENTIFY;
  }

  @Override
  public boolean isPresent() {
    if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
      CobbleUtils.LOGGER_RAW.info("Vault not found");
      return false;
    }

    RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp =
      Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);

    if (rsp == null) {
      CobbleUtils.LOGGER_RAW.info("Vault Economy provider not found");
      return false;
    }

    service = rsp.getProvider();

    if (service == null) return false;

    CobbleUtils.LOGGER_RAW.info("Hooked into Vault economy: " + service.getName());
    return true;
  }

  @Override
  public CompletableFuture<EconomyResponse> getBalance(UUID playerId, String currencyId) {
    return CobbleUtils.ASYNC.supply(() -> {
      OfflinePlayer player = getOffline(playerId);
      double balance = service.getBalance(player);
      BigDecimal bal = BigDecimal.valueOf(balance);
      return EconomyResponse.success(bal, bal);
    });
  }

  @Override
  public CompletableFuture<EconomyResponse> deposit(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CobbleUtils.ASYNC.supply(() -> {
      OfflinePlayer player = getOffline(playerId);
      double amountDouble = amount.doubleValue();
      var resp = service.depositPlayer(player, amountDouble);
      BigDecimal newBalance = BigDecimal.valueOf(service.getBalance(player));
      return resp.transactionSuccess()
        ? EconomyResponse.success(amount, newBalance)
        : EconomyResponse.failure(resp.errorMessage);
    });
  }

  @Override
  public CompletableFuture<EconomyResponse> withdraw(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CobbleUtils.ASYNC.supply(() -> {
      OfflinePlayer player = getOffline(playerId);
      double amountDouble = amount.doubleValue();
      var resp = service.withdrawPlayer(player, amountDouble);
      BigDecimal newBalance = BigDecimal.valueOf(service.getBalance(player));
      return resp.transactionSuccess()
        ? EconomyResponse.success(amount, newBalance)
        : EconomyResponse.failure(resp.errorMessage);
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

    if (fromPlayerId.equals(toPlayerId))
      return CompletableFuture.completedFuture(EconomyResponse.failure("Cannot transfer to the same player"));
    return hasEnoughMoney(fromPlayerId, currencyId, amount).thenCompose(hasMoneyResp -> {
      if (!hasMoneyResp.success())
        return CompletableFuture.completedFuture(EconomyResponse.failure("Source player does not have enough money"));
      return withdraw(fromPlayerId, currencyId, amount, reason).thenCompose(withdrawResp -> {
        if (!withdrawResp.success())
          return CompletableFuture.completedFuture(EconomyResponse.failure("Failed to withdraw from source player"));
        return deposit(toPlayerId, currencyId, amount, reason).thenApply(depositResp -> {
          if (!depositResp.success()) {
            deposit(fromPlayerId, currencyId, amount, "rollback");
            return EconomyResponse.failure("Failed to deposit to target player, rollback applied");
          }
          return EconomyResponse.success(amount, depositResp.balance());
        });
      });
    });
  }

  // =========================================================
  // Helper
  // =========================================================

  @NotNull
  private OfflinePlayer getOffline(UUID uuid) {
    return Bukkit.getOfflinePlayer(uuid);
  }
}