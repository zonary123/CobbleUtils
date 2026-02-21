package com.kingpixel.cobbleutils.util.economys.providers;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.economy.EconomyResult;
import com.kingpixel.cobbleutils.Model.economy.EconomyStatus;
import com.kingpixel.cobbleutils.util.economys.Economy;
import fr.harmex.cobbledollars.common.CobbleDollars;
import fr.harmex.cobbledollars.common.utils.CobbleDollarsPlayer;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@EqualsAndHashCode(callSuper = true)
@Getter
public class CobbleDollarsEconomy extends Economy {

  public static final String IDENTIFY = "COBBLE_DOLLARS";

  @Override
  public String getIdentify() {
    return IDENTIFY;
  }

  @Override
  public boolean isPresent() {
    try {
      return CobbleDollars.INSTANCE.getImplementation() != null;
    } catch (Exception e) {
      CobbleUtils.LOGGER.error("CobbleDollars not present");
      return false;
    }
  }

  // =========================================================
  // Deprecated API
  // =========================================================

  @Override
  @Deprecated(forRemoval = true)
  public boolean deposit(UUID playerUuid, BigDecimal money, String currency) {
    CobbleDollarsPlayer player = getCobblePlayer(playerUuid);
    if (player == null || money.compareTo(BigDecimal.ZERO) < 0) return false;

    BigInteger amount = toBigInteger(money);
    player.cobbleDollars$setCobbleDollars(
      player.cobbleDollars$getCobbleDollars().add(amount)
    );
    return true;
  }

  @Override
  @Deprecated(forRemoval = true)
  public boolean withdraw(UUID playerUuid, BigDecimal money, String currency) {
    CobbleDollarsPlayer player = getCobblePlayer(playerUuid);
    if (player == null || money.compareTo(BigDecimal.ZERO) < 0) return false;

    BigInteger amount = toBigInteger(money);
    BigInteger balance = player.cobbleDollars$getCobbleDollars();

    if (balance.compareTo(amount) < 0) return false;

    player.cobbleDollars$setCobbleDollars(balance.subtract(amount));
    return true;
  }

  @Override
  @Deprecated(forRemoval = true)
  public BigDecimal getBalance(UUID playerUuid, String currency) {
    CobbleDollarsPlayer player = getCobblePlayer(playerUuid);
    if (player == null) return BigDecimal.ZERO;

    return new BigDecimal(player.cobbleDollars$getCobbleDollars());
  }

  @Override
  @Deprecated(forRemoval = true)
  public boolean setBalance(UUID playerUuid, BigDecimal money, String currency) {
    CobbleDollarsPlayer player = getCobblePlayer(playerUuid);
    if (player == null || money.compareTo(BigDecimal.ZERO) < 0) return false;

    player.cobbleDollars$setCobbleDollars(toBigInteger(money));
    return true;
  }

  // =========================================================
  // Modern API
  // =========================================================

  @Override
  public CompletableFuture<EconomyResult> getBalanceAsync(UUID playerUuid, String currency) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        BigDecimal balance = getBalance(playerUuid, currency);

        return EconomyResult.success(
          balance,
          balance,
          BigDecimal.ZERO,
          "Balance fetched"
        );

      } catch (Exception e) {
        return EconomyResult.failure(
          EconomyStatus.ERROR,
          "Error getting balance: " + e.getMessage(),
          null
        );
      }
    });
  }

  @Override
  public CompletableFuture<EconomyResult> setBalance(UUID playerUuid, String currency, BigDecimal amount, String reason) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        BigDecimal before = getBalance(playerUuid, currency);

        if (!setBalance(playerUuid, amount, currency)) {
          return EconomyResult.failure(
            EconomyStatus.ERROR,
            "Failed to set balance",
            before
          );
        }

        BigDecimal after = getBalance(playerUuid, currency);

        return EconomyResult.success(before, after, amount, reason);

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
  public CompletableFuture<EconomyResult> deposit(UUID playerUuid, String currency, BigDecimal amount, String reason) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        BigDecimal before = getBalance(playerUuid, currency);

        if (!deposit(playerUuid, amount, currency)) {
          return EconomyResult.failure(
            EconomyStatus.ERROR,
            "Deposit failed",
            before
          );
        }

        BigDecimal after = getBalance(playerUuid, currency);

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
  public CompletableFuture<EconomyResult> withdraw(UUID playerUuid, String currency, BigDecimal amount, String reason) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        BigDecimal before = getBalance(playerUuid, currency);

        if (before.compareTo(amount) < 0) {
          return EconomyResult.failure(
            EconomyStatus.INSUFFICIENT_FUNDS,
            "Insufficient funds",
            before
          );
        }

        if (!withdraw(playerUuid, amount, currency)) {
          return EconomyResult.failure(
            EconomyStatus.ERROR,
            "Withdraw failed",
            before
          );
        }

        BigDecimal after = getBalance(playerUuid, currency);

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
  public CompletableFuture<Boolean> hasEnoughMoney(UUID playerId, String currencyId, BigDecimal amount) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return getBalance(playerId, currencyId).compareTo(amount) >= 0;
      } catch (Exception e) {
        CobbleUtils.LOGGER.error("Error checking balance");
        e.printStackTrace();
        return false;
      }
    });
  }

  @Override
  public String format(BigDecimal money, String currency) {
    return CobbleUtils.config.getFormat(money);
  }

  @Override
  public int getDecimals(String currency) {
    return CobbleUtils.config.getDecimals();
  }

  // =========================================================
  // Helpers
  // =========================================================

  private CobbleDollarsPlayer getCobblePlayer(UUID uuid) {
    Object player = getPlayer(uuid);
    if (player instanceof CobbleDollarsPlayer cdPlayer) {
      return cdPlayer;
    }
    return null;
  }

  private BigInteger toBigInteger(BigDecimal value) {
    return value.toBigInteger();
  }
}