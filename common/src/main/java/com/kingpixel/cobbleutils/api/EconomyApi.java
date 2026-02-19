package com.kingpixel.cobbleutils.api;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.EconomySelector;
import com.kingpixel.cobbleutils.Model.EconomyUse;
import com.kingpixel.cobbleutils.util.economys.Economy;
import com.kingpixel.cobbleutils.util.economys.EconomyResult;
import com.kingpixel.cobbleutils.util.economys.providers.*;
import lombok.Data;
import org.jetbrains.annotations.UnknownNullability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Carlos Varas Alonso - 05/11/2024 23:58
 */
@Data
public class EconomyApi {
  private static final Map<String, Economy> ECONOMIES = new ConcurrentHashMap<>();
  private static Economy DEFAULT_ECONOMY;

  public synchronized static void loadEconomies() {
    if (!ECONOMIES.isEmpty()) return;
    var economies = List.of(
      new UltraEEconomy(),
      new ImpactorEconomy(),
      new CobbleDollarsEconomy(),
      new BeEconomy(),
      new PebbleEconomy(),
      new SDMEconomy(),
      new VaultEconomy()
    );
    for (Economy economy : economies) {
      try {
        if (!economy.isPresent()) {
          CobbleUtils.LOGGER.info("Economy found: " + economy.getIdentify());
          continue;
        }
        registerEconomy(economy);
      } catch (NoClassDefFoundError | IncompatibleClassChangeError | Exception e) {
        CobbleUtils.LOGGER.info("Economy not found: " + economy.getIdentify());
      }
    }
  }

  /**
   * Add money to the player
   *
   * @param playerUuid The player to add the money
   * @param money      The amount of money
   * @param currency   The currency to add
   * @return If the money was added
   */
  @Deprecated(forRemoval = true)
  public static boolean addMoney(UUID playerUuid, BigDecimal money, String currency,
                                 String economyId) {
    Economy economy = getEconomy(economyId);
    if (economy == null) {
      throw new IllegalArgumentException("Economy not found: " + economyId);
    }
    return economy.deposit(playerUuid, money, currency);
  }

  @Deprecated(forRemoval = true)
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

  @Deprecated(forRemoval = true)
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

  @Deprecated(forRemoval = true)
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

  @Deprecated(forRemoval = true)
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
  @Deprecated(forRemoval = true)
  public static boolean transferMoney(UUID fromPlayerUuid, UUID toPlayerUuid, BigDecimal money, EconomyUse economy,
                                      boolean needHasEnough) {
    if (needHasEnough && !hasEnoughMoney(fromPlayerUuid, money, economy, true)) {
      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER.info("Player " + fromPlayerUuid + " does not have enough money to transfer " + money +
          " to " + toPlayerUuid + " using " + economy.getEconomyId());
      }
      return false;
    }
    if (addMoney(toPlayerUuid, money, economy)) {
      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER.info("Transferred " + money + " from " + fromPlayerUuid + " to " + toPlayerUuid +
          " using " + economy.getEconomyId());
      }
      return removeMoney(fromPlayerUuid, money, economy);
    }
    if (CobbleUtils.config.isDebug()) {
      CobbleUtils.LOGGER.info("Failed to transfer " + money + " from " + fromPlayerUuid + " to " + toPlayerUuid +
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
  @Deprecated(forRemoval = true)
  public static String formatMoney(BigDecimal money, String currency, String economyId) {
    return getEconomy(economyId).format(money, currency);
  }

  @Deprecated(forRemoval = true)
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

  @Deprecated(forRemoval = true)
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

  @Deprecated(forRemoval = true)
  public static String getSymbol(EconomyUse economy) {
    return getSymbol(economy.getCurrency(), economy.getEconomyId());
  }

  @Deprecated(forRemoval = true)
  public static int getDecimals(EconomyUse economy) {
    return getEconomy(economy.getEconomyId()).getDecimals(economy.getCurrency());
  }

  /* -------------------------------------------------------------------------- */
  /* New Methods                                                                */
  /* -------------------------------------------------------------------------- */

  public static void registerEconomy(@Nonnull @UnknownNullability Economy economy) {
    String economyId = economy.getIdentify();
    try {
      if (ECONOMIES.putIfAbsent(economyId, economy) != null) {
        CobbleUtils.LOGGER.warn("Economy with ID '%s' is already registered.".formatted(economyId));
        return;
      }
      UUID testPlayer = UUID.randomUUID();
      String currencyId = "";

      economy.getBalanceAsync(testPlayer, currencyId)
        .whenComplete((result, ex) -> {
          if (ex == null) {
            CobbleUtils.LOGGER.warn("Economy '%s' registered successfully.".formatted(economyId));
            if (DEFAULT_ECONOMY == null) {
              DEFAULT_ECONOMY = economy;
              CobbleUtils.LOGGER.warn("Economy '%s' set as default economy.".formatted(economyId));
            }
          } else {
            ECONOMIES.remove(economyId);
          }
        });
    } catch (NoClassDefFoundError | NoSuchMethodError | Exception e) {
      ECONOMIES.remove(economyId);
    }
  }

  @Nullable
  public static Economy getEconomy(@Nonnull String economyId) {
    if (ECONOMIES.size() == 1) return DEFAULT_ECONOMY;
    Economy economy = ECONOMIES.get(economyId);
    if (economy == null) economy = DEFAULT_ECONOMY;
    return economy;
  }

  @Nonnull
  public static Map<String, Economy> getEconomies() {
    return ECONOMIES;
  }


  @Nonnull
  public static CompletableFuture<EconomyResult> getBalanceAsync(@Nonnull UUID playerId, @Nonnull EconomySelector selector) {
    return resolveEconomy(selector).getBalanceAsync(playerId, selector.getCurrency());
  }

  @Nonnull
  public static CompletableFuture<EconomyResult> setBalance(@Nonnull UUID playerId, @Nonnull String economyId, @Nonnull String currencyId, @Nonnull BigDecimal amount, @Nonnull String reason) {
    return setBalance(playerId, new EconomySelector(economyId, currencyId), amount, reason);
  }

  @Nonnull
  public static CompletableFuture<EconomyResult> setBalance(@Nonnull UUID playerId, @Nonnull EconomySelector selector, @Nonnull BigDecimal amount, @Nonnull String reason) {
    return resolveEconomy(selector).setBalance(playerId, selector.getCurrency(), amount, reason);
  }

  @Nonnull
  public static CompletableFuture<EconomyResult> deposit(@Nonnull UUID playerId, @Nonnull String economyId, @Nonnull String currencyId, @Nonnull BigDecimal amount, @Nonnull String reason) {
    return deposit(playerId, new EconomySelector(economyId, currencyId), amount, reason);
  }

  @Nonnull
  public static CompletableFuture<EconomyResult> deposit(@Nonnull UUID playerId, @Nonnull EconomySelector selector, @Nonnull BigDecimal amount, @Nonnull String reason) {
    return resolveEconomy(selector).deposit(playerId, selector.getCurrency(), amount, reason);
  }

  @Nonnull
  public static CompletableFuture<EconomyResult> withdraw(@Nonnull UUID playerId, @Nonnull String economyId, @Nonnull String currencyId, @Nonnull BigDecimal amount, @Nonnull String reason) {
    return withdraw(playerId, new EconomySelector(economyId, currencyId), amount, reason);
  }

  @Nonnull
  public static CompletableFuture<EconomyResult> withdraw(@Nonnull UUID playerId, @Nonnull EconomySelector selector, @Nonnull BigDecimal amount, @Nonnull String reason) {
    return resolveEconomy(selector).withdraw(playerId, selector.getCurrency(), amount, reason);
  }

  public static CompletableFuture<Boolean> hasBalance(@Nonnull UUID playerId, @Nonnull String economyId, @Nonnull String currencyId, @Nonnull BigDecimal amount) {
    return hasBalance(playerId, new EconomySelector(economyId, currencyId), amount);
  }

  public static CompletableFuture<Boolean> hasBalance(@Nonnull UUID playerId, @Nonnull EconomySelector selector, @Nonnull BigDecimal amount) {
    return resolveEconomy(selector).hasBalance(playerId, selector.getCurrency(), amount);
  }

  @Nonnull
  public static CompletableFuture<EconomyResult> transfer(@Nonnull UUID fromPlayerId, @Nonnull UUID toPlayerId, @Nonnull EconomySelector selector, @Nonnull BigDecimal amount, @Nonnull String reason) {
    return resolveEconomy(selector).transfer(fromPlayerId, toPlayerId, selector.getCurrency(), amount, reason);
  }

  /* -------------------------------------------------------------------------- */
  /* Internal                                                                    */
  /* -------------------------------------------------------------------------- */

  @Nonnull
  private static Economy resolveEconomy(@Nonnull EconomySelector selector) {
    Economy economy = getEconomy(selector.getEconomy());
    if (economy == null) throw new IllegalArgumentException("Economy not found: " + selector.getEconomy());
    return economy;
  }

  public static Map<String, Economy> getEconomys() {
    return ECONOMIES;
  }
}
