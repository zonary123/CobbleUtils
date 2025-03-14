package com.kingpixel.cobbleutils.api;

import com.kingpixel.cobbleutils.util.EconomyUtil;
import net.minecraft.server.network.ServerPlayerEntity;

import javax.annotation.Nonnull;
import java.math.BigDecimal;

/**
 * @author Carlos Varas Alonso - 05/11/2024 23:58
 */
public class EconomyApi {
  /**
   * Add money to the player
   *
   * @param player  The player to add the money
   * @param money   The amount of money
   * @param curreny The currency to add
   *
   * @return
   */
  public static boolean addMoney(ServerPlayerEntity player, BigDecimal money, @Nonnull String curreny) {
    EconomyUtil.setEconomyType();
    return EconomyUtil.addMoney(player, curreny, money);

  }

  /**
   * Remove money from the player
   *
   * @param player  The player to remove the money
   * @param money   The amount of money
   * @param curreny The currency to remove
   *
   * @return
   */
  public static boolean removeMoney(ServerPlayerEntity player, BigDecimal money, @Nonnull String curreny) {
    EconomyUtil.setEconomyType();
    return EconomyUtil.removeMoney(player, curreny, money);
  }

  /**
   * Get the money of the player
   *
   * @param player  The player to get the money
   * @param curreny The currency to get
   *
   * @return The amount of money
   */
  public static BigDecimal getMoney(ServerPlayerEntity player, @Nonnull String curreny) {
    EconomyUtil.setEconomyType();
    return EconomyUtil.getBalance(player, curreny);
  }

  /**
   * Set the money of the player
   *
   * @param player  The player to set the money
   * @param money   The amount of money
   * @param curreny The currency to set
   */
  public static void setMoney(ServerPlayerEntity player, BigDecimal money, @Nonnull String curreny) {
    EconomyUtil.setEconomyType();
    EconomyUtil.setMoney(player, curreny, money);
  }

  /**
   * Format the money of the player
   *
   * @param money    The amount of money
   * @param currency The currency to format
   *
   * @return The formatted money
   */
  public static String formatMoney(BigDecimal money, @Nonnull String currency) {
    EconomyUtil.setEconomyType();
    return EconomyUtil.formatCurrency(money, currency);
  }

  /**
   * Check if the player has enough money
   *
   * @param player   The player to check the money
   * @param money    The amount of money
   * @param currency The currency to check
   *
   * @return If the player has enough money
   */
  @Deprecated(forRemoval = true, since = "1.1.3 - 07/01/2025 23:58")
  public static String formatMoney(ServerPlayerEntity player, BigDecimal money, @Nonnull String currency) {
    EconomyUtil.setEconomyType();
    return EconomyUtil.formatCurrency(money, currency);
  }


  /**
   * Check if the player has enough money and remove it
   *
   * @param player   The player to check the money
   * @param money    The amount of money
   * @param currency The currency to check
   *
   * @return If the player has enough money
   */
  @Deprecated(forRemoval = true, since = "1.1.3 - 05/11/2024 23:58")
  public static boolean hasEnoughMoney(ServerPlayerEntity player, BigDecimal money, @Nonnull String currency) {
    EconomyUtil.setEconomyType();
    return EconomyUtil.hasEnough(player, currency, money);
  }

  /**
   * Check if the player has enough money and remove it
   *
   * @param player   The player to check the money
   * @param money    The amount of money
   * @param currency The currency to check
   * @param notify   If the player should be notified
   *
   * @return If the player has enough money
   */
  public static boolean hasEnoughMoney(ServerPlayerEntity player, BigDecimal money,
                                       @Nonnull String currency, boolean notify) {
    EconomyUtil.setEconomyType();
    return EconomyUtil.hasEnough(player, currency, money, notify);
  }

  /**
   * Get the symbol of the currency
   *
   * @param currency The currency to get the symbol
   *
   * @return The symbol of the currency
   */
  public static String getSymbol(String currency) {
    EconomyUtil.setEconomyType();
    return EconomyUtil.getSymbol(currency);
  }
}
