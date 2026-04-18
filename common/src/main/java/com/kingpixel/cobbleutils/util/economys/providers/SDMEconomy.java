package com.kingpixel.cobbleutils.util.economys.providers;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.economys.Economy;
import com.kingpixel.cobbleutils.util.economys.EconomyResponse;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
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
      CobbleUtils.LOGGER_RAW.error("SDM Economy not present");
      return false;
    }
  }

  // =========================================================
  // Internal Access
  // =========================================================

  private CurrencyPlayerData.PlayerCurrency getPlayerData(UUID uuid, String currency) {
    CobbleUtils.LOGGER_RAW.warn(
      "SDM Economy integration may be unstable. Repository: https://github.com/zonary123/CobbleUtils"
    );
    return null; // Mantengo comportamiento actual
  }

  // =========================================================
  // Modern Async API
  // =========================================================

  @Override
  public CompletableFuture<EconomyResponse> getBalance(UUID playerId, String currencyId) {
    return CobbleUtils.ASYNC.supply(() -> {
      try {
        CurrencyPlayerData.PlayerCurrency data = getPlayerData(playerId, currencyId);
        if (data == null) return EconomyResponse.failure("Player data not found");

        BigDecimal balance = BigDecimal.valueOf(data.balance);
        return EconomyResponse.success(balance, balance);

      } catch (Exception e) {
        return EconomyResponse.failure("Error retrieving balance: " + e.getMessage());
      }
    });
  }

  @Override
  public CompletableFuture<EconomyResponse> deposit(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CobbleUtils.ASYNC.supply(() -> {
      try {
        CurrencyPlayerData.PlayerCurrency data = getPlayerData(playerId, currencyId);
        if (data == null || amount.signum() <= 0)
          return EconomyResponse.failure("Deposit failed: player not found or invalid amount");

        data.balance += amount.doubleValue();
        return EconomyResponse.success(amount, BigDecimal.valueOf(data.balance));
      } catch (Exception e) {
        return EconomyResponse.failure("Error during deposit: " + e.getMessage());
      }
    });
  }

  @Override
  public CompletableFuture<EconomyResponse> withdraw(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CobbleUtils.ASYNC.supply(() -> {
      try {
        CurrencyPlayerData.PlayerCurrency data = getPlayerData(playerId, currencyId);
        if (data == null) return EconomyResponse.failure("Player not found");

        if (data.balance < amount.doubleValue()) return EconomyResponse.failure("Insufficient funds");

        data.balance -= amount.doubleValue();
        return EconomyResponse.success(amount, BigDecimal.valueOf(data.balance));
      } catch (Exception e) {
        return EconomyResponse.failure("Error during withdrawal: " + e.getMessage());
      }
    });
  }
}