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

  @Override public String format(BigDecimal money, String currency) {
    return AdventureTranslator.legacyComponentSerializer.serialize(
      getCurrency(currency).format(money)
    );
  }

  @Override public boolean setBalance(UUID playerUuid, BigDecimal money, String currency) {
    return getAccount(playerUuid, currency).set(money).successful();
  }

  @Override public String getSymbol(String currency) {
    return GsonComponentSerializer.gson().serialize(getCurrency(currency).symbol());
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

  private Currency getCurrency(@Subst("") String currency) {
    if (!currency.contains(":")) currency = "impactor:" + currency;
    var c = service.currencies().currency(Key.key(currency));
    if (c.isPresent()) return c.get();
    if (CobbleUtils.config.isDebug())
      CobbleUtils.LOGGER.error("Currency not found: " + currency + " using primary currency");
    return service.currencies().primary();
  }

  @Override public int getDecimals(String currency) {
    return getCurrency(currency).decimals();
  }
}
