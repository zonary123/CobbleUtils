package com.kingpixel.cobbleutils.util.economys.v1;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.impactdev.impactor.api.economy.EconomyService;
import net.impactdev.impactor.api.economy.accounts.Account;
import net.impactdev.impactor.api.economy.currency.Currency;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of the Economy system using the Impactor Economy Service.
 * Provides methods for handling deposits, withdrawals, and balance management for players,
 * formatted currency strings, and caching for performance optimization.
 * <p>
 * This class communicates with the Impact API to manage player accounts and currency balances.
 * It uses Caffeine caching to optimize repeated formatting of currency values.
 * </p>
 *
 * @author Carlos Varas Alonso
 * @version 1.0
 * @since 2025-01-29
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ImpactorEconomy extends EconomyAbstract {

  // The unique identifier for the Impactor economy implementation.
  public static final String IDENTIFY = "IMPACTOR";

  // The economy service instance used to interact with the Impact economy API.
  public static EconomyService service;

  /**
   * Default constructor for the ImpactorEconomy class.
   * Initializes the service field when needed.
   */
  public ImpactorEconomy() {
  }

  /**
   * Returns the identifier for this economy implementation.
   *
   * @return The string "IMPACTOR".
   */
  @Override
  public String getIdentify() {
    return IDENTIFY;
  }

  /**
   * Checks if the Impactor economy service is present and initializes it if necessary.
   *
   * @return Always returns true, indicating that the service is available.
   */
  @Override
  public boolean isPresent() {
    service = EconomyService.instance();
    return true;
  }

  /**
   * Deposits the specified amount of money into the player's account.
   *
   * @param playerUuid The UUID of the player.
   * @param money      The amount of money to deposit.
   * @param currency   The currency type in which to deposit the money.
   * @return True if the deposit is successful, false otherwise.
   */
  @Override
  public boolean deposit(UUID playerUuid, BigDecimal money, String currency) {
    Account account = getAccount(playerUuid, currency);
    return account.deposit(money).successful();
  }

  /**
   * Withdraws the specified amount of money from the player's account.
   *
   * @param playerUuid The UUID of the player.
   * @param money      The amount of money to withdraw.
   * @param currency   The currency type from which to withdraw the money.
   * @return True if the withdrawal is successful, false otherwise.
   */
  @Override
  public boolean withdraw(UUID playerUuid, BigDecimal money, String currency) {
    Account account = getAccount(playerUuid, currency);
    return account.withdraw(money).successful();
  }

  /**
   * Gets the current balance of the player's account in the specified currency.
   *
   * @param playerUuid The UUID of the player.
   * @param currency   The currency type for which to fetch the balance.
   * @return The current balance of the account.
   */
  @Override
  public BigDecimal getBalance(UUID playerUuid, String currency) {
    return getAccount(playerUuid, currency).balance();
  }

  /**
   * Cache size for formatted currency strings. This cache stores previously formatted values
   * to avoid redundant formatting operations.
   * <p>
   * The cache automatically removes the least recently used entry when it exceeds the specified size.
   */
  private static final int CACHE_SIZE = 5000;

  // Cache for storing formatted currency strings.
  private static final Cache<String, String> formatCache = Caffeine.newBuilder()
    .maximumSize(CACHE_SIZE)
    .expireAfterAccess(5, TimeUnit.SECONDS)
    .build();

  /**
   * Formats the specified amount of money into a string representation using the specified currency.
   * The formatted string is cached for performance optimization.
   *
   * @param money    The amount of money to format.
   * @param currency The currency in which to format the money.
   * @return The formatted string representation of the money.
   */
  @Override
  public String format(BigDecimal money, String currency) {
    String key = money.toPlainString() + "|" + currency;
    return formatCache.get(key, k -> AdventureTranslator.legacyComponentSerializer.serialize(
      getCurrency(currency).format(money)
    ));
  }

  /**
   * Sets the balance of the player's account to the specified amount.
   *
   * @param playerUuid The UUID of the player.
   * @param money      The new balance to set.
   * @param currency   The currency type in which to set the balance.
   * @return True if the balance is successfully set, false otherwise.
   */
  @Override
  public boolean setBalance(UUID playerUuid, BigDecimal money, String currency) {
    Account account = getAccount(playerUuid, currency);
    return account.set(money).successful();
  }

  /**
   * Cache for storing currency symbols. This cache is used to prevent repeated lookups
   * of the currency symbols for formatting purposes.
   */
  private static final Map<String, String> SYMBOLS_CACHE = new HashMap<>();

  /**
   * Gets the symbol for the specified currency.
   *
   * @param currency The currency type for which to fetch the symbol.
   * @return The symbol of the currency, serialized as a string.
   */
  @Override
  public String getSymbol(String currency) {
    return SYMBOLS_CACHE.computeIfAbsent(currency, k -> GsonComponentSerializer.gson().serialize(getCurrency(currency).symbol()));
  }

  private Map<String, Cache<UUID, Account>> accountCacheBuilder = new HashMap<>();
  private Cache<UUID, Boolean> hasAccountCache = Caffeine.newBuilder()
    .expireAfterAccess(1, java.util.concurrent.TimeUnit.MINUTES)
    .maximumSize(10_000)
    .build();

  /**
   * Retrieves an account from the Impactor API, creating a new account if necessary.
   *
   * @param uuid     The UUID of the account.
   * @param currency The currency type of the account.
   * @return The account associated with the UUID and currency.
   */
  private Account getAccount(UUID uuid, String currency) {
    // Use a nested cache to store accounts by currency and UUID. ! Problems: Inconsistence money if changed outside.
    /*var currencyCache = accountCacheBuilder.computeIfAbsent(currency, k -> Caffeine.newBuilder()
      .expireAfterAccess(1, TimeUnit.MINUTES)
      .maximumSize(1_000)
      .build());
    return currencyCache.get(uuid, k -> {
      if (!service.hasAccount(uuid).join()) {
        return service.account(uuid).join();
      }
      return service.account(getCurrency(currency), uuid).join();
    });*/
    if (hasAccountCache.getIfPresent(uuid) == null) hasAccountCache.put(uuid, service.hasAccount(uuid).join());
    return service.account(getCurrency(currency), uuid).join();
  }

  // Cache for storing currencies to avoid redundant lookups.
  private final Map<String, Currency> currencies = new HashMap<>();

  /**
   * Retrieves the Currency object corresponding to the specified currency string.
   * If the currency is not already cached, it will be fetched from the Impact API.
   *
   * @param currency The string representation of the currency (e.g., "impactor:currencyName").
   * @return The Currency object corresponding to the specified currency.
   */
  private Currency getCurrency(String currency) {
    if (currency == null || currency.isBlank()) {
      return service.currencies().primary();
    }

    Currency result = currencies.get(currency);
    if (result != null) return result;

    String cleanCurrency = currency.trim().toLowerCase(Locale.ROOT);
    result = currencies.get(cleanCurrency);
    if (result != null) return result;

    // 1. Try direct Key lookup if valid
    try {
      Key directKey = cleanCurrency.contains(":") ? Key.key(cleanCurrency) : Key.key("impactor", cleanCurrency);
      var opt = service.currencies().currency(directKey);
      if (opt.isPresent()) {
        result = opt.get();
        currencies.put(currency, result);
        currencies.put(cleanCurrency, result);
        return result;
      }
    } catch (Throwable ignored) {
      // Key format might be unusual, fallback to searching registered currencies below
    }

    // 2. Search through all registered currencies in Impactor
    try {
      for (Currency reg : service.currencies().registered()) {
        Key regKey = reg.key();
        if (regKey.asString().equalsIgnoreCase(cleanCurrency)
          || regKey.value().equalsIgnoreCase(cleanCurrency)
          || regKey.asString().equalsIgnoreCase(currency.trim())
          || regKey.value().equalsIgnoreCase(currency.trim())) {
          result = reg;
          currencies.put(currency, result);
          currencies.put(cleanCurrency, result);
          return result;
        }
      }
    } catch (Throwable t) {
      CobbleUtils.LOGGER_RAW.error("Failed to query registered currencies from Impactor: {}", t.getMessage());
    }

    // 3. Currency was specified but not found: DO NOT silently use primary currency!
    List<String> registeredKeys = new ArrayList<>();
    try {
      for (Currency reg : service.currencies().registered()) {
        registeredKeys.add(reg.key().asString());
      }
    } catch (Throwable ignored) {
      // Ignored
    }

    CobbleUtils.LOGGER_RAW.error(
      "Impactor currency '{}' was not found! Registered currencies: {}. Cannot process operation with invalid currency.",
      currency, registeredKeys
    );

    throw new IllegalArgumentException(
      "Impactor currency '" + currency + "' not found! Registered currencies: " + registeredKeys
    );
  }

  /**
   * Retrieves the number of decimal places for the specified currency.
   *
   * @param currency The currency for which to retrieve the number of decimals.
   * @return The number of decimal places for the specified currency.
   */
  @Override
  public int getDecimals(String currency) {
    return getCurrency(currency).decimals();
  }
}
