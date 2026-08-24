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
  private static final String PLAYER_NOT_FOUND = "Player not found or offline";

  @Override
  public String getIdentify() {
    return IDENTIFY;
  }

  @Override
  public boolean isPresent() {
    try {
      return CobbleDollars.INSTANCE != null && CobbleDollars.INSTANCE.getImplementation() != null;
    } catch (NoClassDefFoundError | Exception ignored) {
      return false;
    }
  }

  @Override
  public CompletableFuture<EconomyResponse> getBalance(UUID playerUuid, String currency) {
    return getCobblePlayer(playerUuid)
      .thenApply(cdPlayer -> {
        if (cdPlayer == null) return EconomyResponse.failure(PLAYER_NOT_FOUND);
        BigInteger rawBalance = cdPlayer.cobbleDollars$getCobbleDollars();
        BigDecimal balance = rawBalance != null ? new BigDecimal(rawBalance) : BigDecimal.ZERO;
        return EconomyResponse.success(balance, balance);
      });
  }

  @Override
  public CompletableFuture<EconomyResponse> deposit(UUID playerUuid, String currency, BigDecimal amount, String reason) {
    if (amount == null || amount.signum() < 0) {
      return CompletableFuture.completedFuture(EconomyResponse.failure("Cannot deposit negative or null amount"));
    }

    return getCobblePlayer(playerUuid)
      .thenApply(cdPlayer -> {
        if (cdPlayer == null) return EconomyResponse.failure(PLAYER_NOT_FOUND);
        BigInteger current = cdPlayer.cobbleDollars$getCobbleDollars();
        if (current == null) current = BigInteger.ZERO;

        BigInteger addAmount = amount.toBigInteger();
        BigInteger newBalance = current.add(addAmount);
        cdPlayer.cobbleDollars$setCobbleDollars(newBalance);
        return EconomyResponse.success(amount, new BigDecimal(newBalance));
      });
  }

  @Override
  public CompletableFuture<EconomyResponse> withdraw(UUID playerUuid, String currency, BigDecimal amount, String reason) {
    if (amount == null || amount.signum() < 0) {
      return CompletableFuture.completedFuture(EconomyResponse.failure("Cannot withdraw negative or null amount"));
    }

    return getCobblePlayer(playerUuid)
      .thenApply(cdPlayer -> {
        if (cdPlayer == null) return EconomyResponse.failure(PLAYER_NOT_FOUND);
        BigInteger current = cdPlayer.cobbleDollars$getCobbleDollars();
        if (current == null) current = BigInteger.ZERO;

        BigInteger subAmount = amount.toBigInteger();
        if (current.compareTo(subAmount) < 0) return EconomyResponse.failure("Insufficient funds");

        BigInteger newBalance = current.subtract(subAmount);
        cdPlayer.cobbleDollars$setCobbleDollars(newBalance);
        return EconomyResponse.success(amount, new BigDecimal(newBalance));
      });
  }

  @Override
  public CompletableFuture<EconomyResponse> setBalance(UUID playerUuid, String currency, BigDecimal amount, String reason) {
    if (amount == null || amount.signum() < 0) {
      return CompletableFuture.completedFuture(EconomyResponse.failure("Cannot set negative or null balance"));
    }

    return getCobblePlayer(playerUuid)
      .thenApply(cdPlayer -> {
        if (cdPlayer == null) return EconomyResponse.failure(PLAYER_NOT_FOUND);
        BigInteger newBalance = amount.toBigInteger();
        cdPlayer.cobbleDollars$setCobbleDollars(newBalance);
        return EconomyResponse.success(amount, new BigDecimal(newBalance));
      });
  }

  @Override
  public String format(BigDecimal money, String currency) {
    if (money == null) money = BigDecimal.ZERO;
    return CobbleUtils.config.getFormat(money);
  }

  @Override
  public int getDecimals(String currency) {
    return 0;
  }

  // =========================================================
  // Helpers
  // =========================================================

  private CompletableFuture<@Nullable CobbleDollarsPlayer> getCobblePlayer(UUID uuid) {
    return getPlayer(uuid)
      .thenApply(player -> (player instanceof CobbleDollarsPlayer cdPlayer) ? cdPlayer : null);
  }
}