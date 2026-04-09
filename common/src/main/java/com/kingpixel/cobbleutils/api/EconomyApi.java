package com.kingpixel.cobbleutils.api;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.EconomyUse;
import com.kingpixel.cobbleutils.Model.Priority;
import com.kingpixel.cobbleutils.Model.PriorityEconomy;
import com.kingpixel.cobbleutils.util.economys.v1.*;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Carlos Varas Alonso - 05/11/2024 23:58
 */
@Data
public class EconomyApi {
  @Getter
  private static final Set<EconomyAbstract> economys = ConcurrentHashMap.newKeySet();

  /**
   * Initialize and refresh available economy types.
   */
  public static void setEconomyType() {
    Set<EconomyAbstract> candidates = new HashSet<>();
    candidates.add(new UltraEEconomy());
    candidates.add(new ImpactorEconomy());
    candidates.add(new CobbleDollarsEconomy());
    candidates.add(new BeEconomy());
    candidates.add(new PebbleEconomy());
    candidates.add(new SDMEconomy());
    candidates.add(new VaultEconomy());

    candidates.removeIf(economy -> {
      try {
        if (economy.isPresent()) {
          CobbleUtils.LOGGER_RAW.info("Economy found: {}", economy.getIdentify());
          return false;
        } else {
          CobbleUtils.LOGGER_RAW.info("Economy not found: {}", economy.getIdentify());
          return true;
        }
      } catch (NoClassDefFoundError | IncompatibleClassChangeError | Exception e) {
        CobbleUtils.LOGGER_RAW.info("Economy not found: {}", economy.getIdentify());
        return true;
      }
    });

    economys.clear();
    economys.addAll(candidates);
  }


  private static EconomyAbstract getEconomy(String economyId) {
    if (economys.isEmpty()) {
      setEconomyType();
    }

    List<EconomyAbstract> snapshot = economys.stream()
      .filter(Objects::nonNull)
      .toList();

    if (snapshot.isEmpty()) {
      throw new RuntimeException("You dont have any economys, Supported: " +
        "BeEconomy, CobbleDollarsEconomy, ImpactorEconomy, PebbleEconomy, SDMEconomy, VaultEconomy");
    }

    if (snapshot.size() == 1) {
      return snapshot.getFirst();
    }

    for (EconomyAbstract economy : snapshot) {
      if (economy.getIdentify().equalsIgnoreCase(economyId)) {
        return economy;
      }
    }

    EconomyAbstract economy = getHighestPriorityEconomy(snapshot);
    if (economy == null) throw new RuntimeException("CobbleUtils could not find any economys with id: " + economyId);
    return economy;
  }

  private static EconomyAbstract getHighestPriorityEconomy(List<EconomyAbstract> economies) {
    if (economies.isEmpty()) return null;

    List<EconomyAbstract> sorted = new ArrayList<>(economies);
    sorted.sort((e1, e2) -> {
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
    return sorted.getFirst();
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
   * @return If the money was added
   */
  @Deprecated
  public static boolean addMoney(UUID playerUuid, BigDecimal money, String currency,
                                 String economyId) {
    return getEconomy(economyId).deposit(playerUuid, money, currency);
  }

  public static boolean addMoney(UUID playerUuid, BigDecimal money, EconomyUse economy) {
    return addMoney(playerUuid, money, economy.getCurrency(), economy.getEconomyId());
  }

  /**
   * Remove money from the player
   *
   * @param playerUuid The player to remove the money
   * @param money      The amount of money
   * @param currency   The currency to remove
   * @return If the money was removed
   */
  @Deprecated(forRemoval = true)
  public static boolean removeMoney(UUID playerUuid, BigDecimal money, String currency, String economyId) {

    return getEconomy(economyId).withdraw(playerUuid, money, currency);
  }

  public static boolean removeMoney(UUID playerUuid, BigDecimal money, EconomyUse economy) {
    return removeMoney(playerUuid, money, economy.getCurrency(), economy.getEconomyId());
  }

  /**
   * Get the money of the player
   *
   * @param playerUuid The player to get the money
   * @param currency   The currency to get
   * @return The amount of money
   */
  @Deprecated(forRemoval = true)
  public static BigDecimal getBalance(UUID playerUuid, String currency, String economyId) {
    return getEconomy(economyId).getBalance(playerUuid, currency);
  }

  public static BigDecimal getBalance(UUID playerUuid, EconomyUse economy) {
    return getBalance(playerUuid, economy.getCurrency(), economy.getEconomyId());
  }

  /**
   * Set the money of the player
   *
   * @param playerUuid The player to set the money
   * @param money      The amount of money
   * @param currency   The currency to set
   */
  @Deprecated(forRemoval = true)
  public static boolean setBalance(UUID playerUuid, BigDecimal money, String currency, String economyId) {
    return getEconomy(economyId).setBalance(playerUuid, money, currency);
  }

  public static boolean setBalance(UUID playerUuid, BigDecimal money, EconomyUse economy) {
    return setBalance(playerUuid, money, economy.getCurrency(), economy.getEconomyId());
  }

  /**
   * Transfer money from one player to another
   *
   * @param fromPlayerUuid The player to transfer money from
   * @param toPlayerUuid   The player to transfer money to
   * @param money          The amount of money to transfer
   * @param economy        The economy to use for the transfer
   */
  public static boolean transferMoney(UUID fromPlayerUuid, UUID toPlayerUuid, BigDecimal money, EconomyUse economy,
                                      boolean needHasEnough) {
    if (needHasEnough && !hasEnoughMoney(fromPlayerUuid, money, economy, true)) {
      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER_RAW.info("Player " + fromPlayerUuid + " does not have enough money to transfer " + money +
          " to " + toPlayerUuid + " using " + economy.getEconomyId());
      }
      return false;
    }
    if (addMoney(toPlayerUuid, money, economy)) {
      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER_RAW.info("Transferred " + money + " from " + fromPlayerUuid + " to " + toPlayerUuid +
          " using " + economy.getEconomyId());
      }
      return removeMoney(fromPlayerUuid, money, economy);
    }
    if (CobbleUtils.config.isDebug()) {
      CobbleUtils.LOGGER_RAW.info("Failed to transfer " + money + " from " + fromPlayerUuid + " to " + toPlayerUuid +
        " using " + economy.getEconomyId());
    }
    return false;
  }

  /**
   * Format the money of the player
   *
   * @param money    The amount of money
   * @param currency The currency to format
   * @return The formatted money
   */
  @Deprecated(forRemoval = false)
  public static String formatMoney(BigDecimal money, String currency, String economyId) {
    return getEconomy(economyId).format(money, currency);
  }

  public static String formatMoney(BigDecimal money, EconomyUse economy) {
    return formatMoney(money, economy.getCurrency(), economy.getEconomyId());
  }

  /**
   * Check if the player has enough money and remove it
   *
   * @param playerUuid The player to check the money
   * @param money      The amount of money
   * @param currency   The currency to check
   * @param economyId  The economys to check
   * @return If the player has enough money
   */
  @Deprecated(forRemoval = true)
  public static boolean hasEnoughMoney(UUID playerUuid, BigDecimal money, String currency, boolean removeMoney,
                                       String economyId) {
    return getEconomy(economyId).hasEnough(playerUuid, money, currency, removeMoney);
  }

  public static boolean hasEnoughMoney(UUID playerUuid, BigDecimal money, EconomyUse economy, boolean removeMoney) {
    return hasEnoughMoney(playerUuid, money, economy.getCurrency(), removeMoney, economy.getEconomyId());
  }

  /**
   * Get the symbol of the currency
   *
   * @param currency The currency to get the symbol
   * @return The symbol of the currency
   */
  @Deprecated(forRemoval = true)
  public static String getSymbol(String currency, String economyId) {
    return getEconomy(economyId).getSymbol(currency);
  }

  public static String getSymbol(EconomyUse economy) {
    return getSymbol(economy.getCurrency(), economy.getEconomyId());
  }

  public static int getDecimals(EconomyUse economy) {
    return getEconomy(economy.getEconomyId()).getDecimals(economy.getCurrency());
  }
}
