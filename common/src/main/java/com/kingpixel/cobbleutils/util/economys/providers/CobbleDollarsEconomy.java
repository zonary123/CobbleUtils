package com.kingpixel.cobbleutils.util.economys.providers;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.economys.Economy;
import com.kingpixel.cobbleutils.util.economys.EconomyResult;
import fr.harmex.cobbledollars.common.CobbleDollars;
import fr.harmex.cobbledollars.common.utils.CobbleDollarsPlayer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 16/03/2025 3:41
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CobbleDollarsEconomy extends Economy {
  public static final String IDENTIFY = "COBBLE_DOLLARS";

  public CobbleDollarsEconomy() {
  }

  @Override
  public String getIdentify() {
    return IDENTIFY;
  }

  @Override
  public boolean isPresent() {
    CobbleDollars.INSTANCE.getImplementation();
    return true;
  }

  @Override
  public boolean deposit(UUID playerUuid, BigDecimal money, String currency) {
    CobbleDollarsPlayer player = (CobbleDollarsPlayer) getPlayer(playerUuid);
    if (player == null) return false;
    BigInteger depositAmount = BigInteger.valueOf(money.longValue());
    if (depositAmount.compareTo(BigInteger.ZERO) < 0) return false;
    BigInteger balance = player.cobbleDollars$getCobbleDollars();
    player.cobbleDollars$setCobbleDollars(balance.add(depositAmount));
    return true;
  }

  @Override
  public boolean withdraw(UUID playerUuid, BigDecimal money, String currency) {
    CobbleDollarsPlayer player = (CobbleDollarsPlayer) getPlayer(playerUuid);
    if (player == null) return false;
    BigInteger balance = player.cobbleDollars$getCobbleDollars();
    BigInteger integerMoney = BigInteger.valueOf(money.longValue());
    if (balance.compareTo(integerMoney) >= 0) {
      player.cobbleDollars$setCobbleDollars(balance.subtract(integerMoney));
      return true;
    }
    return false;
  }

  @Override
  public BigDecimal getBalance(UUID playerUuid, String currency) {
    CobbleDollarsPlayer player = (CobbleDollarsPlayer) getPlayer(playerUuid);
    if (player == null) return BigDecimal.ZERO;
    return BigDecimal.valueOf(player.cobbleDollars$getCobbleDollars().doubleValue());
  }

  @Override
  public CompletableFuture<EconomyResult> getBalanceAsync(UUID playerUuid, String currency) {
    return CompletableFuture.supplyAsync(() -> {
      CobbleDollarsPlayer player = (CobbleDollarsPlayer) getPlayer(playerUuid);
      if (player == null) return EconomyResult.fail("Player not found");
      BigDecimal balance = BigDecimal.valueOf(player.cobbleDollars$getCobbleDollars().doubleValue());
      return EconomyResult.success(balance, balance, "Balance retrieved successfully");
    });
  }

  @Override
  public CompletableFuture<EconomyResult> setBalance(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CompletableFuture.supplyAsync(() -> {
      boolean success = setBalance(playerId, amount, currencyId);
      return success ? EconomyResult.success(amount, amount, "Balance set successfully") : EconomyResult.fail("Failed to set balance");
    });
  }

  @Override
  public CompletableFuture<EconomyResult> deposit(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CompletableFuture.supplyAsync(() -> {
      boolean success = deposit(playerId, amount, currencyId);
      return success ? EconomyResult.success(amount, amount, "Deposit successful") : EconomyResult.fail("Failed to deposit");
    });
  }

  @Override
  public CompletableFuture<EconomyResult> withdraw(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return CompletableFuture.supplyAsync(() -> {
      boolean success = withdraw(playerId, amount, currencyId);
      return success ? EconomyResult.success(amount, amount, "Withdraw successful") : EconomyResult.fail("Failed to withdraw");
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
  public String formatCurrency(String currencyId, BigDecimal amount) {
    return "";
  }

  @Override
  public String format(BigDecimal money, String currency) {
    return CobbleUtils.config.getFormat(money);
  }

  @Override
  public boolean setBalance(UUID playerUuid, BigDecimal money, String currency) {
    CobbleDollarsPlayer player = (CobbleDollarsPlayer) getPlayer(playerUuid);
    if (player == null) return false;
    player.cobbleDollars$setCobbleDollars(BigInteger.valueOf(money.longValue()));
    return true;
  }

  @Override
  public int getDecimals(String currency) {
    return CobbleUtils.config.getDecimals();
  }
}
