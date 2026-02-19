package com.kingpixel.cobbleutils.util.economys.providers;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.economys.Economy;
import com.kingpixel.cobbleutils.util.economys.EconomyResult;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.krripe.beconomy.api.BEconomy;
import org.krripe.beconomy.api.EconomyAPI;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 16/03/2025 3:35
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BeEconomy extends Economy {
  public static final String IDENTIFY = "BECONOMY";
  private static EconomyAPI service;

  public BeEconomy() {
  }

  @Override
  public String getIdentify() {
    return IDENTIFY;
  }

  @Override
  public boolean isPresent() {
    BEconomy.INSTANCE.initialize(CobbleUtils.server);
    service = BEconomy.INSTANCE.getAPI();
    return true;
  }

  @Override
  public boolean deposit(UUID playerUuid, BigDecimal money, String currency) {
    service.addBalance(playerUuid, money, currency);
    return true;
  }

  @Override
  public boolean withdraw(UUID playerUuid, BigDecimal money, String currency) {
    BigDecimal balance = this.getBalance(playerUuid, currency);
    return setBalance(playerUuid, balance.subtract(money), currency);
  }

  @Override
  public BigDecimal getBalance(UUID playerUuid, String currency) {
    return service.getBalance(playerUuid, currency);
  }

  @Override
  public String format(BigDecimal money, String currency) {
    return CobbleUtils.config.getFormat(money) + " " + service.getCurrencySymbol(currency);
  }

  @Override
  public boolean setBalance(UUID playerUuid, BigDecimal money, String currency) {
    service.setBalance(playerUuid, money, currency);
    return true;
  }

  @Override
  public int getDecimals(String currency) {
    return CobbleUtils.config.getDecimals();
  }

  @Override
  public CompletableFuture<EconomyResult> getBalanceAsync(UUID playerId, String currencyId) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        BigDecimal balance = getBalance(playerId, currencyId);
        return EconomyResult.success(balance, balance, "Balance retrieved successfully");
      } catch (Exception e) {
        return EconomyResult.fail("Error retrieving balance: " + e.getMessage());
      }
    });
  }

  @Override
  public CompletableFuture<EconomyResult> setBalance(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        boolean success = setBalance(playerId, amount, currencyId);
        if (success) {
          return EconomyResult.success(amount, amount, "Balance set successfully");
        } else {
          return EconomyResult.fail("Failed to set balance");
        }
      } catch (Exception e) {
        return EconomyResult.fail("Error setting balance: " + e.getMessage());
      }
    });
  }

  @Override
  public CompletableFuture<EconomyResult> deposit(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        boolean success = deposit(playerId, amount, currencyId);
        if (success) {
          return EconomyResult.success(amount, amount, "Deposit successful");
        } else {
          return EconomyResult.fail("Failed to deposit");
        }
      } catch (Exception e) {
        return EconomyResult.fail("Error depositing: " + e.getMessage());
      }
    });
  }

  @Override
  public CompletableFuture<EconomyResult> withdraw(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        boolean success = withdraw(playerId, amount, currencyId);
        if (success) {
          return EconomyResult.success(amount, amount, "Withdrawal successful");
        } else {
          return EconomyResult.fail("Failed to withdraw");
        }
      } catch (Exception e) {
        return EconomyResult.fail("Error withdrawing: " + e.getMessage());
      }
    });
  }

  @Override
  public CompletableFuture<Boolean> hasBalance(UUID playerId, String currencyId, BigDecimal amount) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        BigDecimal balance = getBalance(playerId, currencyId);
        return balance.compareTo(amount) >= 0;
      } catch (Exception e) {
        CobbleUtils.LOGGER.error("Error checking balance: " + e.getMessage());
        return false;
      }
    });
  }

}
