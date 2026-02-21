package com.kingpixel.cobbleutils.util.economys.providers;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.economy.EconomyResult;
import com.kingpixel.cobbleutils.Model.economy.EconomyStatus;
import com.kingpixel.cobbleutils.util.economys.Economy;
import lombok.EqualsAndHashCode;
import net.sixik.sdmeconomy.economyData.CurrencyPlayerData;
import net.sixik.sdmeconomy.utils.CurrencyHelper;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@EqualsAndHashCode(callSuper = true)
public class SDMEconomy extends Economy {

  public static final String IDENTIFY = "SDM_ECONOMY";

  @Override
  public String getIdentify() {
    return IDENTIFY;
  }

  @Override
  public boolean isPresent() {
    try {
      CurrencyHelper.getAllCurrency();
      return true;
    } catch (Exception e) {
      CobbleUtils.LOGGER.error("SDM Economy not present");
      return false;
    }
  }

  // =========================================================
  // Internal access
  // =========================================================

  public CurrencyPlayerData.PlayerCurrency getPlayerData(UUID uuid, String currency) {
    CobbleUtils.LOGGER.warn(
      "SDM Economy integration may be unstable. Repository: " +
        "https://github.com/zonary123/CobbleUtils"
    );

    return null; // Mantengo tu comportamiento actual
  }

  // =========================================================
  // Deprecated API
  // =========================================================

  @Override
  @Deprecated(forRemoval = true)
  public boolean deposit(UUID playerUuid, BigDecimal money, String currency) {
    var data = getPlayerData(playerUuid, currency);
    if (data == null || money.signum() < 0) return false;

    data.balance += money.doubleValue();
    return true;
  }

  @Override
  @Deprecated(forRemoval = true)
  public boolean withdraw(UUID playerUuid, BigDecimal money, String currency) {
    var data = getPlayerData(playerUuid, currency);
    if (data == null) return false;

    if (data.balance < money.doubleValue()) return false;

    data.balance -= money.doubleValue();
    return true;
  }

  @Override
  @Deprecated(forRemoval = true)
  public BigDecimal getBalance(UUID playerUuid, String currency) {
    var data = getPlayerData(playerUuid, currency);
    if (data == null) return BigDecimal.ZERO;

    return BigDecimal.valueOf(data.balance);
  }

  @Override
  @Deprecated(forRemoval = true)
  public boolean setBalance(UUID playerUuid, BigDecimal money, String currency) {
    var data = getPlayerData(playerUuid, currency);
    if (data == null) return false;

    data.balance = money.doubleValue();
    return true;
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

        if (!deposit(playerId, amount, currencyId)) {
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
          "Error depositing: " + e.getMessage(),
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

        if (!withdraw(playerId, amount, currencyId)) {
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
          "Error withdrawing: " + e.getMessage(),
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

        if (!setBalance(playerId, amount, currencyId)) {
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
        return getBalance(playerId, currencyId)
          .compareTo(amount) >= 0;
      } catch (Exception e) {
        CobbleUtils.LOGGER.error("Error checking balance");
        e.printStackTrace();
        return false;
      }
    });
  }

  // =========================================================
  // Formatting
  // =========================================================

  @Override
  public String format(BigDecimal money, String currency) {
    return CobbleUtils.language.getDefaultSymbol()
      + " "
      + CobbleUtils.config.getFormat(money);
  }

  @Override
  public int getDecimals(String currency) {
    return CobbleUtils.config.getDecimals();
  }
}