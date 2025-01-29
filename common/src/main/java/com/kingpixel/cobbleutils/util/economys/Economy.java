package com.kingpixel.cobbleutils.util.economys;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;

/**
 * @author Carlos Varas Alonso - 29/01/2025 4:06
 */
public abstract class Economy {

  public boolean init() {
    CobbleUtils.LOGGER.info("Economy not implemented");
    return false;
  }


  /**
   * deposit money to the player
   *
   * @param player   The player to deposit money to
   * @param money    The amount of money to deposit
   * @param currency The currency to deposit
   * @param message  The message to send to the player
   * @param prefix   The prefix of the message
   */
  public void deposit(ServerPlayerEntity player, BigDecimal money, String currency, @Nullable String message,
                      @Nullable String prefix) {
  }

  /**
   * withdraw money from the player
   *
   * @param player   The player to withdraw money from
   * @param money    The amount of money to withdraw
   * @param currency The currency to withdraw
   * @param message  The message to send to the player
   * @param prefix   The prefix of the message
   *
   * @return true if the player has enough money
   */
  public boolean withdraw(ServerPlayerEntity player, BigDecimal money, String currency, @Nullable String message,
                          @Nullable String prefix) {
    return false;
  }

  /**
   * get the balance of the player
   *
   * @param player   The player to deposit money to
   * @param currency The currency to deposit
   *
   * @return The balance of the player
   */
  BigDecimal getBalance(ServerPlayerEntity player, String currency) {
    CobbleUtils.LOGGER.info("getBalance not implemented");
    return BigDecimal.ZERO;
  }

  /**
   * check if the player has enough money
   *
   * @param player   The player to withdraw money from
   * @param money    The amount of money to withdraw
   * @param currency The currency to withdraw
   *
   * @return true if the player has enough money
   */
  public boolean hasEnough(ServerPlayerEntity player, BigDecimal money, String currency) {
    if (getBalance(player, currency).compareTo(money) >= 0) {
      withdraw(player, money, currency, null, null);
      return true;
    }
    return false;
  }

  public abstract boolean hasEnough(ServerPlayerEntity player, BigDecimal money, String currency, @Nullable String message,
                                    @Nullable String prefix);

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
  public String format(BigDecimal money, String currency) {
    return getSymbol(currency) + money;
  }

  public void sendMessage(ServerPlayerEntity player, BigDecimal amount, String currency, String message,
                          String prefix) {
    if (message == null) return;
    PlayerUtils.sendMessage(
      player,
      message
        .replace("%price%", format(amount, currency))
        .replace("%amount%", format(amount, currency))
        .replace("%balance%", format(getBalance(player, currency), currency))
        .replace("%symbol%", getSymbol(currency))
        .replace("%currency%", currency),
      prefix == null ? CobbleUtils.language.getPrefixShop() : prefix,
      TypeMessage.CHAT
    );
  }


}
