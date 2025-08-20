package com.kingpixel.cobbleutils.util.economys;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.impactdev.impactor.api.economy.EconomyService;
import net.impactdev.impactor.api.economy.accounts.Account;
import net.impactdev.impactor.api.economy.currency.Currency;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.intellij.lang.annotations.Subst;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 29/01/2025 4:13
 */
@EqualsAndHashCode(callSuper = true) @Data
public class ImpactorEconomy extends EconomyAbstract {
  public static final String IDENTIFY = "IMPACTOR";
  public static EconomyService service;

  public ImpactorEconomy() {
  }

  @Override public String getIdentify() {
    return IDENTIFY;
  }

  @Override public boolean isPresent() {
    service = EconomyService.instance();
    return true;
  }

  @Override public boolean deposit(UUID playerUuid, BigDecimal money, String currency) {
    Account account = getAccount(playerUuid, currency);
    return account.deposit(money).successful();
  }

  @Override public boolean withdraw(UUID playerUuid, BigDecimal money, String currency) {
    Account account = getAccount(playerUuid, currency);
    return account.withdraw(money).successful();
  }

  @Override public BigDecimal getBalance(UUID playerUuid, String currency) {
    return getAccount(playerUuid, currency).balance();
  }

  /**
   * Cache size for the format method.
   * This is used to cache formatted money strings to avoid reformatting the same amount multiple times.
   * The cache will remove the least recently used entry when it exceeds the specified size.
   */
  private static final int CACHE_SIZE = 5000;
  private static final Map<String, String> formatCache = new LinkedHashMap<>(CACHE_SIZE, 0.75f, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
      return size() > CACHE_SIZE;
    }
  };

  @Override
  public String format(BigDecimal money, String currency) {
    String key = (money.toPlainString().intern() + "|" + currency.intern()).intern();
    String cached = formatCache.get(key);
    if (cached != null) return cached;
    String formatted = AdventureTranslator.legacyComponentSerializer.serialize(
      getCurrency(currency).format(money)
    );
    formatCache.put(key, formatted);
    return formatted;
  }

  @Override public boolean setBalance(UUID playerUuid, BigDecimal money, String currency) {
    return getAccount(playerUuid, currency).set(money).successful();
  }

  /**
   * Cache for currency symbols to avoid repeated lookups.
   * This cache stores the serialized symbol of each currency.
   */
  private static final Map<String, String> SYMBOLS_CACHE = new HashMap<>();

  @Override public String getSymbol(String currency) {
    String result = SYMBOLS_CACHE.get(currency);
    if (result == null) {
      result = GsonComponentSerializer.gson().serialize(getCurrency(currency).symbol());
      SYMBOLS_CACHE.put(currency, result);
      return result;
    }
    return result;
  }

  /**
   * Method to get an account from the impactor api.
   *
   * @param uuid     The uuid of the account.
   * @param currency The currency of the account.
   *
   * @return The account.
   */
  private Account getAccount(UUID uuid, String currency) {
    if (!service.hasAccount(uuid).join()) return service.account(uuid).join();
    return service.account(getCurrency(currency), uuid).join();
  }

  private final Map<String, Currency> currencies = new HashMap<>();

  private Currency getCurrency(@Subst("") String currency) {
    Currency result = currencies.get(currency);
    if (result != null) return result;
    if (!currency.contains(":")) currency = "impactor:" + currency;
    var c = service.currencies().currency(Key.key(currency));
    if (c.isPresent()) {
      result = c.get();
      currencies.put(currency, result);
      return result;
    }
    if (CobbleUtils.config.isDebug())
      CobbleUtils.LOGGER.error("Currency not found: " + currency + " using primary currency");
    result = service.currencies().primary();
    return result;
  }

  @Override public int getDecimals(String currency) {
    return getCurrency(currency).decimals();
  }
}
