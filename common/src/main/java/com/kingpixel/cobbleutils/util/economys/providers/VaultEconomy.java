package com.kingpixel.cobbleutils.util.economys.providers;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.economys.Economy;
import com.kingpixel.cobbleutils.util.economys.EconomyResult;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 16/03/2025 3:30
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class VaultEconomy extends Economy {
  public static final String IDENTIFY = "VAULT";
  private net.milkbowl.vault.economy.Economy service;

  public VaultEconomy() {

  }

  @Override
  public String getIdentify() {
    return IDENTIFY;
  }

  @Override
  public boolean isPresent() {
    if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
      CobbleUtils.LOGGER.info("Cannot find Vault!");
      List<String> plugins = new ArrayList<>();
      for (Plugin plugin : Bukkit.getServer().getPluginManager().getPlugins()) {
        plugins.add(plugin.getName());
      }
      CobbleUtils.LOGGER.info("Report this to zonary123 Plugins to Vault -> " + plugins);
    } else {
      RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
      if (rsp == null) {
        CobbleUtils.LOGGER.info("Registered Service Provider for Economy.class not found");
      } else {
        service = rsp.getProvider();
        CobbleUtils.LOGGER.info("Economy successfully hooked up");
        CobbleUtils.LOGGER.info("Economy: " + service.getName());
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean deposit(UUID playerUuid, BigDecimal money, String currency) {
    return service.depositPlayer(Bukkit.getOfflinePlayer(playerUuid), money.doubleValue()).transactionSuccess();
  }

  @Override
  public boolean withdraw(UUID playerUuid, BigDecimal money, String currency) {
    return service.withdrawPlayer(Bukkit.getOfflinePlayer(playerUuid), money.doubleValue()).transactionSuccess();
  }

  @Override
  public BigDecimal getBalance(UUID playerUuid, String currency) {
    return BigDecimal.valueOf(service.getBalance(Bukkit.getOfflinePlayer(playerUuid)));
  }

  @Override
  public CompletableFuture<EconomyResult> getBalanceAsync(UUID playerUuid, String currency) {
    return CompletableFuture.supplyAsync(() -> {
      BigDecimal balance = getBalance(playerUuid, currency);
      return EconomyResult.success(balance, balance, "");
    });
  }

  @Override
  public CompletableFuture<EconomyResult> setBalance(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CompletableFuture.supplyAsync(() -> {
      boolean success = setBalance(playerId, amount, currencyId);
      return success ? EconomyResult.success(amount, amount, "") : EconomyResult.fail("Failed to set balance");
    });
  }

  @Override
  public CompletableFuture<EconomyResult> deposit(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CompletableFuture.supplyAsync(() -> {
      boolean success = deposit(playerId, amount, currencyId);
      return success ? EconomyResult.success(amount, amount, "") : EconomyResult.fail("Failed to deposit");
    });
  }

  @Override
  public CompletableFuture<EconomyResult> withdraw(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CompletableFuture.supplyAsync(() -> {
      boolean success = withdraw(playerId, amount, currencyId);
      return success ? EconomyResult.success(amount, amount, "") : EconomyResult.fail("Failed to withdraw");
    });
  }

  @Override
  public CompletableFuture<Boolean> hasBalance(UUID playerId, String currencyId, BigDecimal amount) {
    return CompletableFuture.supplyAsync(() -> {
      BigDecimal balance = getBalance(playerId, currencyId);
      return balance.compareTo(amount) >= 0;
    });
  }
  
  @Override
  public String format(BigDecimal money, String currency) {
    return service.format(money.doubleValue());
  }

  @Override
  public boolean setBalance(UUID playerUuid, BigDecimal money, String currency) {
    var offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
    if (service.hasAccount(offlinePlayer)) {
      service.withdrawPlayer(offlinePlayer, this.getBalance(playerUuid, currency).doubleValue());
      return service.depositPlayer(offlinePlayer, money.doubleValue()).transactionSuccess();
    }
    return false;
  }

  @Override
  public int getDecimals(String currency) {
    return CobbleUtils.config.getDecimals();
  }
}
