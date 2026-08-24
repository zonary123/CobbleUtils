package com.kingpixel.cobbleutils.util.economys.v1;

import com.kingpixel.cobbleutils.CobbleUtils;
import fr.harmex.cobbledollars.common.CobbleDollars;
import fr.harmex.cobbledollars.common.utils.CobbleDollarsPlayer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class CobbleDollarsEconomy extends EconomyAbstract {

  public static final String IDENTIFY = "COBBLE_DOLLARS";

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

  private CobbleDollarsPlayer getCobblePlayer(UUID uuid) {
    Object player = getPlayer(uuid);
    if (player instanceof CobbleDollarsPlayer cdPlayer) {
      return cdPlayer;
    }
    return null;
  }

  private BigInteger toBigInteger(BigDecimal money) {
    if (money == null || money.compareTo(BigDecimal.ZERO) < 0) return null;
    return money.toBigInteger();
  }

  @Override
  public boolean deposit(UUID playerUuid, BigDecimal money, String currency) {
    BigInteger amount = toBigInteger(money);
    if (amount == null) return false;

    CobbleDollarsPlayer player = getCobblePlayer(playerUuid);
    if (player == null) return false;

    BigInteger balance = player.cobbleDollars$getCobbleDollars();
    if (balance == null) balance = BigInteger.ZERO;
    player.cobbleDollars$setCobbleDollars(balance.add(amount));

    return true;
  }

  @Override
  public boolean withdraw(UUID playerUuid, BigDecimal money, String currency) {
    BigInteger amount = toBigInteger(money);
    if (amount == null) return false;

    CobbleDollarsPlayer player = getCobblePlayer(playerUuid);
    if (player == null) return false;

    BigInteger balance = player.cobbleDollars$getCobbleDollars();
    if (balance == null) balance = BigInteger.ZERO;

    if (balance.compareTo(amount) < 0) {
      return false;
    }

    player.cobbleDollars$setCobbleDollars(balance.subtract(amount));
    return true;
  }

  @Override
  public BigDecimal getBalance(UUID playerUuid, String currency) {
    CobbleDollarsPlayer player = getCobblePlayer(playerUuid);
    if (player == null) return BigDecimal.ZERO;

    BigInteger balance = player.cobbleDollars$getCobbleDollars();
    if (balance == null) return BigDecimal.ZERO;

    return new BigDecimal(balance);
  }

  @Override
  public boolean setBalance(UUID playerUuid, BigDecimal money, String currency) {
    BigInteger amount = toBigInteger(money);
    if (amount == null) return false;

    CobbleDollarsPlayer player = getCobblePlayer(playerUuid);
    if (player == null) return false;

    player.cobbleDollars$setCobbleDollars(amount);
    return true;
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
}