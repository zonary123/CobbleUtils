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
      var resp = service.depositPlayer(player, amount.doubleValue());
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
      if (!service.has(player, amount.doubleValue())) return EconomyResponse.failure("Insufficient funds");
      
      var resp = service.withdrawPlayer(player, amount.doubleValue());
      BigDecimal newBalance = BigDecimal.valueOf(service.getBalance(player));
      return resp.transactionSuccess()
        ? EconomyResponse.success(amount, newBalance)
        : EconomyResponse.failure(resp.errorMessage);
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