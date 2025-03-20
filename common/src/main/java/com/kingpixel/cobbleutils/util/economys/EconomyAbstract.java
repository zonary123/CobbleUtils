package com.kingpixel.cobbleutils.util.economys;

import com.kingpixel.cobbleutils.CobbleUtils;
import lombok.Data;
import net.minecraft.server.network.ServerPlayerEntity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 29/01/2025 4:06
 */
@Data
public abstract class EconomyAbstract {

  public abstract String getIdentify();

  public abstract boolean isPresent();


  /**
   * deposit money to the player
   *
   * @param playerUuid The player to deposit money to
   * @param money      The amount of money to deposit
   * @param currency   The currency to depositç
   */
  public abstract boolean deposit(UUID playerUuid, BigDecimal money, String currency);

  /**
   * withdraw money from the player
   *
   * @param playerUuid The player to withdraw money from
   * @param money      The amount of money to withdraw
   * @param currency   The currency to withdraw
   *
   * @return true if the player has enough money
   */
  public abstract boolean withdraw(UUID playerUuid, BigDecimal money, String currency);

  /**
   * get the balance of the player
   *
   * @param playerUuid The player to deposit money to
   * @param currency   The currency to deposit
   *
   * @return The balance of the player
   */
  public abstract BigDecimal getBalance(UUID playerUuid, String currency);


  /**
   * check if the player has enough money
   *
   * @param playerUuid  The player to withdraw money from
   * @param money       The amount of money to withdraw
   * @param currency    The currency to withdraw
   * @param removeMoney Remove the money if the player has enough
   *
   * @return true if the player has enough money
   */
  public boolean hasEnough(UUID playerUuid, BigDecimal money, String currency, boolean removeMoney) {
    if (getBalance(playerUuid, currency).compareTo(money) >= 0) {
      if (removeMoney) withdraw(playerUuid, money, currency);
      return true;
    }
    return false;
  }

  /**
   * get the symbol of the currency
   *
   * @param currency The currency to withdraw
   *
   * @return The symbol of the currency
   */
  public String getSymbol(String currency) {
    return CobbleUtils.language.getDefaultSymbol();
  }

  /**
   * Format the money
   *
   * @param money    The amount of money to format
   * @param currency The currency to format
   *
   * @return The formatted money
   */
  public abstract String format(BigDecimal money, String currency);

  /**
   * set the balance of the player
   *
   * @param playerUuid The player to set the balance
   * @param money      The amount of money to set
   * @param currency   The currency to set
   *
   * @return true if the balance was set
   */
  public abstract boolean setBalance(UUID playerUuid, BigDecimal money, String currency);

  /**
   * get the decimals of the currency
   *
   * @param currency The currency to get the decimals
   *
   * @return The decimals of the currency
   */
  public abstract int getDecimals(String currency);


  public ServerPlayerEntity getPlayer(UUID uuid) {
    return CobbleUtils.server.getPlayerManager().getPlayer(uuid);
  }

}
