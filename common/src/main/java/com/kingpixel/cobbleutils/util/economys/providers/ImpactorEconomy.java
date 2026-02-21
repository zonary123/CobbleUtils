package com.kingpixel.cobbleutils.util.economys.providers;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.economy.EconomyResult;
import com.kingpixel.cobbleutils.Model.economy.EconomyStatus;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.economys.Economy;
import lombok.EqualsAndHashCode;
import net.impactdev.impactor.api.economy.EconomyService;
import net.impactdev.impactor.api.economy.accounts.Account;
import net.impactdev.impactor.api.economy.currency.Currency;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@EqualsAndHashCode(callSuper = true)
public class ImpactorEconomy extends Economy {

  public static final String IDENTIFY = "IMPACTOR";
  public static EconomyService service;

  private final Map<String, Currency> currencies = new java.util.HashMap<>();

  @Override
  public String getIdentify() {
    return IDENTIFY;
  }

  @Override
  public boolean isPresent() {
    try {
      service = EconomyService.instance();
      return service != null;
    } catch (Exception e) {
      CobbleUtils.LOGGER.error("Impactor not present");
      return false;
    }
  }

  // =========================================================
  // Deprecated (kept but internally safe)
  // =========================================================

  @Override
  @Deprecated(forRemoval = true)
  public boolean deposit(UUID playerUuid, BigDecimal money, String currency) {
    return getAccountAsync(playerUuid, currency)
      .thenApply(account -> account.deposit(money).successful())
      .join();
  }

  @Override
  @Deprecated(forRemoval = true)
  public boolean withdraw(UUID playerUuid, BigDecimal money, String currency) {
    return getAccountAsync(playerUuid, currency)
      .thenApply(account -> account.withdraw(money).successful())
      .join();
  }

  @Override
  @Deprecated(forRemoval = true)
  public BigDecimal getBalance(UUID playerUuid, String currency) {
    return getAccountAsync(playerUuid, currency)
      .thenApply(Account::balance)
      .join();
  }

  @Override
  @Deprecated(forRemoval = true)
  public boolean setBalance(UUID playerUuid, BigDecimal money, String currency) {
    return getAccountAsync(playerUuid, currency)
      .thenApply(account -> account.set(money).successful())
      .join();
  }

  // =========================================================
  // Modern Async API (NO join, NO blocking)
  // =========================================================

  @Override
  public CompletableFuture<EconomyResult> getBalanceAsync(UUID playerId, String currencyId) {
    return getAccountAsync(playerId, currencyId)
      .thenApply(account -> {
        if (account == null) {
          return EconomyResult.failure(
            EconomyStatus.PLAYER_NOT_FOUND,
            "Account not found",
            null
          );
        }

        BigDecimal balance = account.balance();

        return EconomyResult.success(
          balance,
          balance,
          BigDecimal.ZERO,
          "Balance retrieved"
        );
      })
      .exceptionally(ex ->
        EconomyResult.failure(
          EconomyStatus.ERROR,
          ex.getMessage(),
          null
        )
      );
  }

  @Override
  public CompletableFuture<EconomyResult> deposit(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return getAccountAsync(playerId, currencyId)
      .thenApply(account -> {
        if (account == null) {
          return EconomyResult.failure(
            EconomyStatus.PLAYER_NOT_FOUND,
            "Account not found",
            null
          );
        }

        BigDecimal before = account.balance();

        var result = account.deposit(amount);

        if (!result.successful()) {
          return EconomyResult.failure(
            EconomyStatus.ERROR,
            "Deposit failed",
            before
          );
        }

        BigDecimal after = account.balance();

        return EconomyResult.success(before, after, amount, reason);
      })
      .exceptionally(ex ->
        EconomyResult.failure(
          EconomyStatus.ERROR,
          ex.getMessage(),
          null
        )
      );
  }

  @Override
  public CompletableFuture<EconomyResult> withdraw(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return getAccountAsync(playerId, currencyId)
      .thenApply(account -> {
        if (account == null) {
          return EconomyResult.failure(
            EconomyStatus.PLAYER_NOT_FOUND,
            "Account not found",
            null
          );
        }

        BigDecimal before = account.balance();

        if (before.compareTo(amount) < 0) {
          return EconomyResult.failure(
            EconomyStatus.INSUFFICIENT_FUNDS,
            "Insufficient funds",
            before
          );
        }

        var result = account.withdraw(amount);

        if (!result.successful()) {
          return EconomyResult.failure(
            EconomyStatus.ERROR,
            "Withdraw failed",
            before
          );
        }

        BigDecimal after = account.balance();

        return EconomyResult.success(before, after, amount, reason);
      })
      .exceptionally(ex ->
        EconomyResult.failure(
          EconomyStatus.ERROR,
          ex.getMessage(),
          null
        )
      );
  }

  @Override
  public CompletableFuture<EconomyResult> setBalance(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return getAccountAsync(playerId, currencyId)
      .thenApply(account -> {
        if (account == null) {
          return EconomyResult.failure(
            EconomyStatus.PLAYER_NOT_FOUND,
            "Account not found",
            null
          );
        }

        BigDecimal before = account.balance();

        var result = account.set(amount);

        if (!result.successful()) {
          return EconomyResult.failure(
            EconomyStatus.ERROR,
            "Set balance failed",
            before
          );
        }

        return EconomyResult.success(before, amount, amount, reason);
      })
      .exceptionally(ex ->
        EconomyResult.failure(
          EconomyStatus.ERROR,
          ex.getMessage(),
          null
        )
      );
  }

  @Override
  public CompletableFuture<Boolean> hasEnoughMoney(UUID playerId, String currencyId, BigDecimal amount) {
    return getAccountAsync(playerId, currencyId)
      .thenApply(account -> account != null && account.balance().compareTo(amount) >= 0)
      .exceptionally(ex -> false);
  }

  // =========================================================
  // Formatting & Currency
  // =========================================================

  private static final Cache<String, String> formatCache =
    Caffeine.newBuilder()
      .maximumSize(5000)
      .expireAfterAccess(5, TimeUnit.SECONDS)
      .build();

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
    return GsonComponentSerializer.gson().serialize(
      getCurrency(currency).symbol()
    );
  }

  @Override
  public int getDecimals(String currency) {
    return getCurrency(currency).decimals();
  }

  // =========================================================
  // Internal helpers
  // =========================================================

  private CompletableFuture<Account> getAccountAsync(UUID uuid, String currency) {
    return service.hasAccount(uuid)
      .thenCompose(hasAccount -> {
        if (!hasAccount) {
          return service.account(uuid);
        }
        return service.account(getCurrency(currency), uuid);
      });
  }

  private Currency getCurrency(String currency) {
    Currency cached = currencies.get(currency);
    if (cached != null) return cached;

    if (!currency.contains(":")) {
      currency = "impactor:" + currency;
    }

    var optional = service.currencies().currency(Key.key(currency));

    if (optional.isPresent()) {
      Currency result = optional.get();
      currencies.put(currency, result);
      return result;
    }

    return service.currencies().primary();
  }
}