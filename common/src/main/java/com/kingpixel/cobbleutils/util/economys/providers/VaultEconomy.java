package com.kingpixel.cobbleutils.util.economys.providers;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.economy.EconomyResult;
import com.kingpixel.cobbleutils.Model.economy.EconomyStatus;
import com.kingpixel.cobbleutils.util.economys.Economy;
import lombok.EqualsAndHashCode;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

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
      CobbleUtils.LOGGER.info("Vault not found");
      return false;
    }

    RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp =
      Bukkit.getServicesManager()
        .getRegistration(net.milkbowl.vault.economy.Economy.class);

    if (rsp == null) {
      CobbleUtils.LOGGER.info("Vault Economy provider not found");
      return false;
    }

    service = rsp.getProvider();

    if (service == null) return false;

    CobbleUtils.LOGGER.info("Hooked into Vault economy: " + service.getName());
    return true;
  }

  // =========================================================
  // Deprecated API
  // =========================================================

  @Override
  @Deprecated(forRemoval = true)
  public boolean deposit(UUID playerUuid, BigDecimal money, String currency) {
    return service.depositPlayer(getOffline(playerUuid), money.doubleValue())
      .transactionSuccess();
  }

  @Override
  @Deprecated(forRemoval = true)
  public boolean withdraw(UUID playerUuid, BigDecimal money, String currency) {
    return service.withdrawPlayer(getOffline(playerUuid), money.doubleValue())
      .transactionSuccess();
  }

  @Override
  @Deprecated(forRemoval = true)
  public BigDecimal getBalance(UUID playerUuid, String currency) {
    return BigDecimal.valueOf(
      service.getBalance(getOffline(playerUuid))
    );
  }

  @Override
  @Deprecated(forRemoval = true)
  public boolean setBalance(UUID playerUuid, BigDecimal money, String currency) {
    OfflinePlayer player = getOffline(playerUuid);

    if (!service.hasAccount(player)) {
      service.createPlayerAccount(player);
    }

    service.withdrawPlayer(player, service.getBalance(player));
    return service.depositPlayer(player, money.doubleValue())
      .transactionSuccess();
  }

  // =========================================================
  // Modern Async API
  // =========================================================

  @Override
  public CompletableFuture<EconomyResult> getBalanceAsync(UUID playerId, String currencyId) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        BigDecimal balance = getBalance(playerId, currencyId);

        return EconomyResult.success(
          balance,
          balance,
          BigDecimal.ZERO,
          "Balance retrieved"
        );

      } catch (Exception e) {
        return EconomyResult.failure(
          EconomyStatus.ERROR,
          "Error retrieving balance: " + e.getMessage(),
          null
        );
      }
    });
  }

  @Override
  public CompletableFuture<EconomyResult> deposit(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        BigDecimal before = getBalance(playerId, currencyId);

        OfflinePlayer player = getOffline(playerId);

        var result = service.depositPlayer(player, amount.doubleValue());

        if (!result.transactionSuccess()) {
          return EconomyResult.failure(
            EconomyStatus.ERROR,
            "Deposit failed",
            before
          );
        }

        BigDecimal after = getBalance(playerId, currencyId);

        return EconomyResult.success(before, after, amount, reason);

      } catch (Exception e) {
        return EconomyResult.failure(
          EconomyStatus.ERROR,
          "Error during deposit: " + e.getMessage(),
          null
        );
      }
    });
  }

  @Override
  public CompletableFuture<EconomyResult> withdraw(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        BigDecimal before = getBalance(playerId, currencyId);

        if (before.compareTo(amount) < 0) {
          return EconomyResult.failure(
            EconomyStatus.INSUFFICIENT_FUNDS,
            "Insufficient funds",
            before
          );
        }

        OfflinePlayer player = getOffline(playerId);

        var result = service.withdrawPlayer(player, amount.doubleValue());

        if (!result.transactionSuccess()) {
          return EconomyResult.failure(
            EconomyStatus.ERROR,
            "Withdraw failed",
            before
          );
        }

        BigDecimal after = getBalance(playerId, currencyId);

        return EconomyResult.success(before, after, amount, reason);

      } catch (Exception e) {
        return EconomyResult.failure(
          EconomyStatus.ERROR,
          "Error during withdrawal: " + e.getMessage(),
          null
        );
      }
    });
  }

  @Override
  public CompletableFuture<EconomyResult> setBalance(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        BigDecimal before = getBalance(playerId, currencyId);

        OfflinePlayer player = getOffline(playerId);

        service.withdrawPlayer(player, before.doubleValue());
        var result = service.depositPlayer(player, amount.doubleValue());

        if (!result.transactionSuccess()) {
          return EconomyResult.failure(
            EconomyStatus.ERROR,
            "Set balance failed",
            before
          );
        }

        return EconomyResult.success(before, amount, amount, reason);

      } catch (Exception e) {
        return EconomyResult.failure(
          EconomyStatus.ERROR,
          "Error setting balance: " + e.getMessage(),
          null
        );
      }
    });
  }

  @Override
  public CompletableFuture<Boolean> hasEnoughMoney(UUID playerId, String currencyId, BigDecimal amount) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return service.has(getOffline(playerId), amount.doubleValue());
      } catch (Exception e) {
        return false;
      }
    });
  }

  // =========================================================
  // Formatting
  // =========================================================

  @Override
  public String format(BigDecimal money, String currency) {
    return service.format(money.doubleValue());
  }

  @Override
  public int getDecimals(String currency) {
    return CobbleUtils.config.getDecimals();
  }

  // =========================================================
  // Helper
  // =========================================================

  private OfflinePlayer getOffline(UUID uuid) {
    return Bukkit.getOfflinePlayer(uuid);
  }
}