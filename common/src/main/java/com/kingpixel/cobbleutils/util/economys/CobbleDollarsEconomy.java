package com.kingpixel.cobbleutils.util.economys;

import com.kingpixel.cobbleutils.CobbleUtils;
import fr.harmex.cobbledollars.common.CobbleDollars;
import fr.harmex.cobbledollars.common.utils.CobbleDollarsPlayer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 16/03/2025 3:41
 */
public class CobbleDollarsEconomy extends EconomyAbstract {
  public static final String IDENTIFY = "COBBLE_DOLLARS";

  @Override public String getIdentify() {
    return IDENTIFY;
  }

  @Override public boolean isPresent() {
    try {
      CobbleDollars.INSTANCE.getImplementation();
      CobbleUtils.LOGGER.info("CobbleDollars found Identifier: " + getIdentify());
      return true;
    } catch (NoClassDefFoundError | Exception e) {
      CobbleUtils.LOGGER.error("CobbleDollars not found");
      return false;
    }
  }

  @Override public boolean deposit(UUID playerUuid, BigDecimal money, String currency) {
    CobbleDollarsPlayer player = (CobbleDollarsPlayer) getPlayer(playerUuid);
    if (player == null) return false;
    player.cobbleDollars$addCobbleDollars(BigInteger.valueOf(money.longValue()));
    return true;
  }

  @Override public boolean withdraw(UUID playerUuid, BigDecimal money, String currency) {
    CobbleDollarsPlayer player = (CobbleDollarsPlayer) getPlayer(playerUuid);
    if (player == null) return false;
    BigInteger balance = player.cobbleDollars$getCobbleDollars();
    if (balance.compareTo(BigInteger.valueOf(money.longValue())) >= 0) {
      player.cobbleDollars$addCobbleDollars(balance.subtract(BigInteger.valueOf(money.longValue())));
      return true;
    }
    return false;
  }

  @Override public BigDecimal getBalance(UUID playerUuid, String currency) {
    CobbleDollarsPlayer player = (CobbleDollarsPlayer) getPlayer(playerUuid);
    if (player == null) return BigDecimal.ZERO;
    return BigDecimal.valueOf(player.cobbleDollars$getCobbleDollars().doubleValue());
  }

  @Override public String format(BigDecimal money, String currency) {
    return CobbleUtils.language.getDefaultSymbol() + money;
  }

  @Override public boolean setBalance(UUID playerUuid, BigDecimal money, String currency) {
    CobbleDollarsPlayer player = (CobbleDollarsPlayer) getPlayer(playerUuid);
    if (player == null) return false;
    player.cobbleDollars$setCobbleDollars(BigInteger.valueOf(money.longValue()));
    return true;
  }
}
