package com.kingpixel.cobbleutils.util.economys.providers;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.economys.Economy;
import com.kingpixel.cobbleutils.util.economys.EconomyResponse;
import fr.harmex.cobbledollars.common.CobbleDollars;
import fr.harmex.cobbledollars.common.utils.CobbleDollarsPlayer;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@EqualsAndHashCode(callSuper = true)
public class CobbleDollarsEconomy extends Economy {

  public static final String IDENTIFY = "COBBLE_DOLLARS";

  @Override
  public String getIdentify() {
    return IDENTIFY;
  }

  @Override
  public boolean isPresent() {
    try {
      CobbleDollars.INSTANCE.getImplementation();
      return true;
    } catch (NoClassDefFoundError | Exception e) {
      CobbleUtils.LOGGER_RAW.error("CobbleDollars not present");
      return false;
    }
  }

  private CompletableFuture<EconomyResponse> runAsyncEconomyOp(CompletableFuture<EconomyResponse> future) {
    return CobbleUtils.ASYNC.supply(() ->
      future.exceptionally(ex -> {
        CobbleUtils.LOGGER_RAW.error("Economy operation failed");
        ex.printStackTrace();
        return EconomyResponse.failure("Error: " + ex.getMessage());
      }).join()
    );
  }

  @Override
  public CompletableFuture<EconomyResponse> getBalance(UUID playerUuid, String currency) {
    return getCobblePlayer(playerUuid)
      .thenApply(cdPlayer -> {
        if (cdPlayer == null) return EconomyResponse.failure("Player not found");
        BigDecimal balance = new BigDecimal(cdPlayer.cobbleDollars$getCobbleDollars());
        return EconomyResponse.success(balance, balance);
      });
  }

  @Override
  public CompletableFuture<EconomyResponse> setBalance(UUID playerUuid, String currency, BigDecimal amount, String reason) {
    return getCobblePlayer(playerUuid)
      .thenApply(cdPlayer -> {
        if (cdPlayer == null) return EconomyResponse.failure("Player not found");
        BigInteger value = amount.toBigInteger();
        cdPlayer.cobbleDollars$setCobbleDollars(value);
        BigDecimal after = new BigDecimal(cdPlayer.cobbleDollars$getCobbleDollars());
        return EconomyResponse.success(amount, after);
      });
  }

  @Override
  public CompletableFuture<EconomyResponse> deposit(UUID playerUuid, String currency, BigDecimal amount, String reason) {
    if (amount.signum() < 0)
      return CompletableFuture.completedFuture(EconomyResponse.failure("Cannot deposit negative amount"));

    return getCobblePlayer(playerUuid)
      .thenApply(cdPlayer -> {
        if (cdPlayer == null) return EconomyResponse.failure("Player not found");
        cdPlayer.cobbleDollars$setCobbleDollars(cdPlayer.cobbleDollars$getCobbleDollars().add(amount.toBigInteger()));
        BigDecimal after = new BigDecimal(cdPlayer.cobbleDollars$getCobbleDollars());
        return EconomyResponse.success(amount, after);
      });
  }

  @Override
  public CompletableFuture<EconomyResponse> withdraw(UUID playerUuid, String currency, BigDecimal amount, String reason) {
    if (amount.signum() < 0)
      return CompletableFuture.completedFuture(EconomyResponse.failure("Cannot withdraw negative amount"));

    return getCobblePlayer(playerUuid)
      .thenApply(cdPlayer -> {
        if (cdPlayer == null) return EconomyResponse.failure("Player not found");
        BigDecimal before = new BigDecimal(cdPlayer.cobbleDollars$getCobbleDollars());
        if (before.compareTo(amount) < 0) return EconomyResponse.failure("Insufficient funds");
        cdPlayer.cobbleDollars$setCobbleDollars(cdPlayer.cobbleDollars$getCobbleDollars().subtract(amount.toBigInteger()));
        BigDecimal after = new BigDecimal(cdPlayer.cobbleDollars$getCobbleDollars());
        return EconomyResponse.success(amount, after);
      });
  }

  @Override
  public CompletableFuture<EconomyResponse> hasEnoughMoney(UUID playerId, String currencyId, BigDecimal amount) {
    return getBalance(playerId, currencyId)
      .thenApply(response -> {
        if (!response.success()) return response;
        return response.balance().compareTo(amount) >= 0
          ? EconomyResponse.success(BigDecimal.ZERO, response.balance())
          : EconomyResponse.failure("Insufficient funds");
      });
  }

  @Override
  public String format(BigDecimal money, String currency) {
    if (money == null) money = BigDecimal.ZERO;
    return CobbleUtils.config.getFormat(money);
  }

  @Override
  public int getDecimals(String currency) {
    return CobbleUtils.config.getDecimals();
  }

  // =========================================================
  // Helpers
  // =========================================================

  private CompletableFuture<@Nullable CobbleDollarsPlayer> getCobblePlayer(UUID uuid) {
    return getPlayer(uuid)
      .thenApply(player -> (player instanceof CobbleDollarsPlayer cdPlayer) ? cdPlayer : null);
  }
}