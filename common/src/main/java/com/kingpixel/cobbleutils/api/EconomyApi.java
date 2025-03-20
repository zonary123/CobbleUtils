package com.kingpixel.cobbleutils.api;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.Priority;
import com.kingpixel.cobbleutils.Model.PriorityEconomy;
import com.kingpixel.cobbleutils.util.economys.*;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author Carlos Varas Alonso - 05/11/2024 23:58
 */
@Data
public class EconomyApi {
  @Getter private static Set<EconomyAbstract> economys = new HashSet<>();

  public static void setEconomyType() {
    economys.add(new ImpactorEconomy());
    economys.add(new CobbleDollarsEconomy());
    economys.add(new BeEconomy());
    economys.add(new PebbleEconomy());
    economys.add(new SDMEconomy());
    economys.add(new VaultEconomy());

    economys.removeIf(economy -> {
      try {
        if (economy.isPresent()) {
          CobbleUtils.LOGGER.info("Economy found: " + economy.getIdentify());
          return false;
        } else {
          CobbleUtils.LOGGER.info("Economy not found: " + economy.getIdentify());
          return true;
        }
      } catch (NoClassDefFoundError | IncompatibleClassChangeError | Exception e) {
        if (CobbleUtils.config.isDebug()) {
          e.printStackTrace();
        }
        CobbleUtils.LOGGER.info("Economy not found: " + economy.getIdentify());
        return true;
      }
    });
  }


  private static EconomyAbstract getEconomy(String EconomyId) {
    if (economys.isEmpty()) {
      setEconomyType();
      if (economys.isEmpty()) {
        throw new RuntimeException("You dont have any economys, Supported: " +
          "BeEconomy, CobbleDollarsEconomy, ImpactorEconomy, PebbleEconomy, SDMEconomy, VaultEconomy");
      }
    }
    if (economys.size() == 1) {
      return economys.iterator().next();
    } else {
      for (EconomyAbstract economy : economys) {
        if (economy.getIdentify().equalsIgnoreCase(EconomyId)) {
          return economy;
        }
      }
    }

    EconomyAbstract economy = getHighestPriorityEconomy();
    if (economy == null) throw new RuntimeException("CobbleUtils could not find any economys with that id");
    return economy;
  }

  private static EconomyAbstract getHighestPriorityEconomy() {
    List<EconomyAbstract> economyList = new ArrayList<>(economys);
    economyList.sort((e1, e2) -> {
      Priority p1 = CobbleUtils.config.getPriorityEconomy().stream()
        .filter(pe -> pe.getEconomyId().equals(e1.getIdentify()))
        .findFirst()
        .map(PriorityEconomy::getPriority)
        .orElse(Priority.LOWEST);
      Priority p2 = CobbleUtils.config.getPriorityEconomy().stream()
        .filter(pe -> pe.getEconomyId().equals(e2.getIdentify()))
        .findFirst()
        .map(PriorityEconomy::getPriority)
        .orElse(Priority.LOWEST);
      return p1.compareTo(p2);
    });
    return economyList.isEmpty() ? null : economyList.getFirst();
  }


  /**
   * Add a new economys
   *
   * @param economy The economys to add
   */
  public static void addEconomy(EconomyAbstract economy) {
    economys.add(economy);
  }

  /**
   * Add money to the player
   *
   * @param playerUuid The player to add the money
   * @param money      The amount of money
   * @param currency   The currency to add
   *
   * @return If the money was added
   */
  public static boolean addMoney(UUID playerUuid, BigDecimal money, String currency,
                                 String economyId) {
    return getEconomy(economyId).deposit(playerUuid, money, currency);
  }

  /**
   * Remove money from the player
   *
   * @param playerUuid The player to remove the money
   * @param money      The amount of money
   * @param currency   The currency to remove
   *
   * @return If the money was removed
   */
  public static boolean removeMoney(UUID playerUuid, BigDecimal money, String currency, String economyId) {

    return getEconomy(economyId).withdraw(playerUuid, money, currency);
  }

  /**
   * Get the money of the player
   *
   * @param playerUuid The player to get the money
   * @param currency   The currency to get
   *
   * @return The amount of money
   */
  public static BigDecimal getBalance(UUID playerUuid, String currency, String economyId) {
    return getEconomy(economyId).getBalance(playerUuid, currency);
  }

  /**
   * Set the money of the player
   *
   * @param playerUuid The player to set the money
   * @param money      The amount of money
   * @param currency   The currency to set
   */
  public static boolean setBalance(UUID playerUuid, BigDecimal money, String currency, String economyId) {
    return getEconomy(economyId).setBalance(playerUuid, money, currency);
  }

  /**
   * Format the money of the player
   *
   * @param money    The amount of money
   * @param currency The currency to format
   *
   * @return The formatted money
   */
  public static String formatMoney(BigDecimal money, String currency, String economyId) {
    return getEconomy(economyId).format(money, currency);
  }

  /**
   * Check if the player has enough money and remove it
   *
   * @param playerUuid The player to check the money
   * @param money      The amount of money
   * @param currency   The currency to check
   * @param economyId  The economys to check
   *
   * @return If the player has enough money
   */
  public static boolean hasEnoughMoney(UUID playerUuid, BigDecimal money, String currency, boolean removeMoney,
                                       String economyId) {
    return getEconomy(economyId).hasEnough(playerUuid, money, currency, removeMoney);
  }

  /**
   * Get the symbol of the currency
   *
   * @param currency The currency to get the symbol
   *
   * @return The symbol of the currency
   */
  public static String getSymbol(String currency, String economyId) {
    return getEconomy(economyId).getSymbol(currency);
  }
}
