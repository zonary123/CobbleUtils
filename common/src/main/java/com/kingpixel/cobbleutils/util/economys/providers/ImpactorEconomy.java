package com.kingpixel.cobbleutils.util.economys.providers;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.economys.Economy;
import com.kingpixel.cobbleutils.util.economys.EconomyResponse;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
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

        BigDecimal before = account.balance();
        var result = account.deposit(amount);

        if (!result.successful()) return EconomyResponse.failure("Deposit failed");

        BigDecimal after = account.balance();
        return EconomyResponse.success(amount, after);
      })
      .exceptionally(ex -> EconomyResponse.failure("Error during deposit: " + ex.getMessage()));
  }

  @Override
  public CompletableFuture<EconomyResponse> withdraw(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return getAccountAsync(playerId, currencyId)
      .thenApply(account -> {
        if (account == null) return EconomyResponse.failure("Account not found");

        BigDecimal before = account.balance();
        if (before.compareTo(amount) < 0) return EconomyResponse.failure("Insufficient funds");

        var result = account.withdraw(amount);
        if (!result.successful()) return EconomyResponse.failure("Withdraw failed");

        BigDecimal after = account.balance();
        return EconomyResponse.success(amount, after);
      })
      .exceptionally(ex -> EconomyResponse.failure("Error during withdrawal: " + ex.getMessage()));
  }

  @Override
  public CompletableFuture<EconomyResponse> setBalance(UUID playerId, String currencyId, BigDecimal amount, String reason) {
    return getBalance(playerId, currencyId).thenCompose(balanceResp -> {
      BigDecimal current = balanceResp.balance();
      BigDecimal diff = amount.subtract(current);

      if (diff.compareTo(BigDecimal.ZERO) > 0) return deposit(playerId, currencyId, diff, reason);
      if (diff.compareTo(BigDecimal.ZERO) < 0) return withdraw(playerId, currencyId, diff.abs(), reason);
      return CompletableFuture.completedFuture(EconomyResponse.success(BigDecimal.ZERO, current));
    });
  }

  @Override
  public CompletableFuture<EconomyResponse> hasEnoughMoney(UUID playerId, String currencyId, BigDecimal amount) {
    return getBalance(playerId, currencyId)
      .thenApply(balanceResp -> balanceResp.balance().compareTo(amount) >= 0
        ? EconomyResponse.success(BigDecimal.ZERO, balanceResp.balance())
        : EconomyResponse.failure("Not enough money"));
  }

  @Override
  public CompletableFuture<EconomyResponse> transfer(@NonNull UUID fromPlayerId, @NonNull UUID toPlayerId,
                                                     @NonNull String currencyId, @NonNull BigDecimal amount,
                                                     @NonNull String reason) {
    if (fromPlayerId.equals(toPlayerId))
      return CompletableFuture.completedFuture(EconomyResponse.failure("Cannot transfer to the same player"));

    return hasEnoughMoney(fromPlayerId, currencyId, amount).thenCompose(hasMoneyResp -> {
      if (!hasMoneyResp.success())
        return CompletableFuture.completedFuture(EconomyResponse.failure("Source player has insufficient funds"));

      return withdraw(fromPlayerId, currencyId, amount, reason).thenCompose(withdrawResp -> {
        if (!withdrawResp.success())
          return CompletableFuture.completedFuture(EconomyResponse.failure("Failed to withdraw from source player"));

        return deposit(toPlayerId, currencyId, amount, reason).thenApply(depositResp -> {
          if (!depositResp.success()) {
            // Rollback
            deposit(fromPlayerId, currencyId, amount, "rollback");
            return EconomyResponse.failure("Failed to deposit to target player, rollback applied");
          }
          return EconomyResponse.success(amount, depositResp.balance());
        });
      });
    });
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
    return service.hasAccount(uuid).thenCompose(hasAccount -> {
      if (!hasAccount) return service.account(uuid);
      return service.account(getCurrency(currency), uuid);
    });
  }

  private Currency getCurrency(String currency) {
    Currency cached = currencies.get(currency);
    if (cached != null) return cached;

    if (!currency.contains(":")) currency = "impactor:" + currency;
    var optional = service.currencies().currency(Key.key(currency));

    if (optional.isPresent()) {
      Currency result = optional.get();
      currencies.put(currency, result);
      return result;
    }

    return service.currencies().primary();
  }
}