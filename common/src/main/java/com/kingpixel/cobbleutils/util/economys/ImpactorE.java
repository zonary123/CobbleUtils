package com.kingpixel.cobbleutils.util.economys;

import com.kingpixel.cobbleutils.CobbleUtils;
import net.impactdev.impactor.api.economy.EconomyService;
import net.impactdev.impactor.api.economy.accounts.Account;
import net.impactdev.impactor.api.economy.currency.Currency;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;

/**
 * @author Carlos Varas Alonso - 29/01/2025 4:13
 */
public class ImpactorE extends Economy {
  private EconomyService economy;

  @Override public boolean init() {
    try {
      economy = EconomyService.instance();
      CobbleUtils.LOGGER.info("Impactor Economy found");
      return true;
    } catch (IllegalStateException | NullPointerException | NoClassDefFoundError e) {
      CobbleUtils.LOGGER.error("Impactor Economy not found");
      return false;
    }
  }

  private Account getAccount(ServerPlayerEntity player, String currency) {
    if (!economy.hasAccount(player.getUuid()).join()) {
      return economy.account(player.getUuid()).join();
    }
    return economy.account(getCurrency(currency), player.getUuid()).join();
  }

  private Currency getCurrency(@Subst("") String currency) {
    try {
      Key key = Key.key(currency);
      return economy.currencies().currency(key).get();
    } catch (NullPointerException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override public void deposit(ServerPlayerEntity player, BigDecimal money, String currency, @Nullable String message,
                                @Nullable String prefix) {
    if (getAccount(player, currency).deposit(money).successful()) {
      sendMessage(player, money, currency, message, prefix);
    }
  }

  @Override
  public boolean withdraw(ServerPlayerEntity player, BigDecimal money, String currency, @Nullable String message,
                          @Nullable String prefix) {
    if (getAccount(player, currency).withdraw(money).successful()) {
      sendMessage(player, money, currency, message, prefix);
      return true;
    }
    return false;
  }

  @Override BigDecimal getBalance(ServerPlayerEntity player, String currency) {
    return getAccount(player, currency).balance();
  }

  @Override
  public boolean hasEnough(ServerPlayerEntity player, BigDecimal money, String currency, @Nullable String message,
                           @Nullable String prefix) {
    Account account = getAccount(player, currency);
    if (account.balance().compareTo(money) >= 0) {
      withdraw(player, money, currency, message, prefix);
      return true;
    }
    return false;
  }

  @Override public String getSymbol(String currency) {
    try {
      Currency c = getCurrency(currency);
      return GsonComponentSerializer.gson().serialize(c.symbol());
    } catch (NullPointerException e) {
      e.printStackTrace();
      return CobbleUtils.language.getDefaultSymbol();
    }
  }
}
