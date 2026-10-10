package com.kingpixel.cobbleutils.util.economys.providers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.economys.Economy;
import com.kingpixel.cobbleutils.util.economys.EconomyResponse;

import lombok.EqualsAndHashCode;
import net.impactdev.impactor.api.economy.EconomyService;
import net.impactdev.impactor.api.economy.accounts.Account;
import net.impactdev.impactor.api.economy.currency.Currency;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

@EqualsAndHashCode(callSuper = true)
public class ImpactorEconomy extends Economy {

  public static final String IDENTIFY = "IMPACTOR";
  public static EconomyService service;

  private final Map<String, Currency> currencies = new HashMap<>();

  private static final Cache<String, String> formatCache =
    Caffeine.newBuilder()
      .maximumSize(5000)
      .expireAfterAccess(5, TimeUnit.SECONDS)
      .build();

  @Override
  public String getIdentify() {
    return IDENTIFY;
  }

  @Override
  public boolean isPresent() {
    try {
      service = EconomyService.instance();
      return service != null;
    } catch (NoClassDefFoundError | Exception e) {
      CobbleUtils.LOGGER_RAW.error("Impactor not present");
      return false;
    }
  }

  // =========================================================
  // Async Economy API
  // =========================================================

  @Override
  public CompletableFuture<EconomyResponse> getBalance(UUID playerId, String currencyId) {
    return getAccountAsync(playerId, currencyId)
      .thenApply(account -> {
        if (account == null) return EconomyResponse.failure("Account not found");
        BigDecimal bal = account.balance();
        return EconomyResponse.success(bal, bal);
      })
      .exceptionally(ex -> EconomyResponse.failure("Error retrieving balance: " + ex.getMessage()));
  }

  @Override
  public CompletableFuture<EconomyResponse> deposit(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return getAccountAsync(playerId, currencyId)
      .thenApply(account -> {
        if (account == null) return EconomyResponse.failure("Account not found");

        var result = account.deposit(amount);
        if (!result.successful()) return EconomyResponse.failure("Deposit failed");

        return EconomyResponse.success(amount, account.balance());
      })
      .exceptionally(ex -> EconomyResponse.failure("Error during deposit: " + ex.getMessage()));
  }

  @Override
  public CompletableFuture<EconomyResponse> withdraw(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return getAccountAsync(playerId, currencyId)
      .thenApply(account -> {
        if (account == null) return EconomyResponse.failure("Account not found");

        if (account.balance().compareTo(amount) < 0) return EconomyResponse.failure("Insufficient funds");

        var result = account.withdraw(amount);
        if (!result.successful()) return EconomyResponse.failure("Withdraw failed");

        return EconomyResponse.success(amount, account.balance());
      })
      .exceptionally(ex -> EconomyResponse.failure("Error during withdrawal: " + ex.getMessage()));
  }

  // =========================================================
  // Formatting & Currency
  // =========================================================

  @Override
  public String format(BigDecimal money, String currency) {
    if (money == null) money = BigDecimal.ZERO;

    String key = money.toPlainString() + "|" + currency;
    BigDecimal finalMoney = money;

    return formatCache.get(key, k ->
      AdventureTranslator.legacyComponentSerializer.serialize(
        getCurrency(currency).format(finalMoney)
      )
    );
  }

  @Override
  public String getSymbol(String currency) {
    return GsonComponentSerializer.gson().serialize(getCurrency(currency).symbol());
  }

  @Override
  public int getDecimals(String currency) {
    return getCurrency(currency).decimals();
  }

  // =========================================================
  // Internal helpers
  // =========================================================

  private CompletableFuture<Account> getAccountAsync(UUID uuid, String currency) {
    try {
      Currency curr = getCurrency(currency);
      return service.account(curr, uuid);
    } catch (Throwable t) {
      return CompletableFuture.failedFuture(t);
    }
  }

  private Currency getCurrency(String currency) {
    if (currency == null || currency.isBlank()) {
      return service.currencies().primary();
    }

    Currency cached = currencies.get(currency);
    if (cached != null) return cached;

    String cleanCurrency = currency.trim().toLowerCase(Locale.ROOT);
    cached = currencies.get(cleanCurrency);
    if (cached != null) return cached;

    // 1. Direct key resolution
    try {
      Key directKey = cleanCurrency.contains(":") ? Key.key(cleanCurrency) : Key.key("impactor", cleanCurrency);
      var optional = service.currencies().currency(directKey);
      if (optional.isPresent()) {
        Currency result = optional.get();
        currencies.put(currency, result);
        currencies.put(cleanCurrency, result);
        return result;
      }
    } catch (Throwable ignored) {
      // Fallback to registry search below
    }

    // 2. Search through all registered currencies
    try {
      for (Currency reg : service.currencies().registered()) {
        Key regKey = reg.key();
        if (regKey.asString().equalsIgnoreCase(cleanCurrency)
          || regKey.value().equalsIgnoreCase(cleanCurrency)
          || regKey.asString().equalsIgnoreCase(currency.trim())
          || regKey.value().equalsIgnoreCase(currency.trim())) {
          currencies.put(currency, reg);
          currencies.put(cleanCurrency, reg);
          return reg;
        }
      }
    } catch (Throwable t) {
      CobbleUtils.LOGGER_RAW.error("Failed to query registered currencies from Impactor: {}", t.getMessage());
    }

    // 3. Currency specified but not found
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

}