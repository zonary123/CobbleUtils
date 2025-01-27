package com.kingpixel.cobbleutils.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kingpixel.cobbleutils.CobbleUtils;
import fr.harmex.cobbledollars.common.CobbleDollars;
import fr.harmex.cobbledollars.common.utils.CobbleDollarsPlayer;
import net.impactdev.impactor.api.economy.EconomyService;
import net.impactdev.impactor.api.economy.accounts.Account;
import net.impactdev.impactor.api.economy.currency.Currency;
import net.impactdev.impactor.api.economy.transactions.EconomyTransaction;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sixik.sdm_economy.api.CurrencyHelper;
import org.blanketeconomy.api.BlanketEconomy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.intellij.lang.annotations.Subst;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.NoSuchElementException;
import java.util.UUID;


/**
 * @author Carlos Varas Alonso - 06/08/2024 11:50
 */
public abstract class EconomyUtil {

  // The impactor service
  public static EconomyService impactorService;

  // The economy type
  public static EconomyType economyType;

  private static Economy vaultEconomy;


  public enum EconomyType {
    IMPACTOR,
    VAULT,
    BLANKECONOMY,
    COBBLEDOLLARS,
    SDM_ECONOMY
  }

  public static String getBalance(ServerPlayerEntity player, String currency, int digits) {
    // Supongamos que obtienes el balance como un BigDecimal desde algún método
    BigDecimal balance = getBalance(player, currency);

    if (balance != null) {
      return formatCurrency(balance, currency);
    }

    // En caso de que el balance sea null, retornas una cadena vacía o algún valor por defecto.
    return "0.00";
  }

  public static int getDecimals(String currency) {
    return switch (economyType) {
      case IMPACTOR -> getCurrency(currency).decimals();
      case VAULT -> 2;
      case BLANKECONOMY -> 2;
      case COBBLEDOLLARS -> 2;
      case SDM_ECONOMY -> 2;
      default -> 2;
    };
  }

  public static void setEconomyType() {
    if (economyType != null) return;
    if (isVaultPresent()) {
      economyType = EconomyType.VAULT;
      CobbleUtils.LOGGER.info("Vault economy found");
    } else if (isImpactorPresent()) {
      economyType = EconomyType.IMPACTOR;
      impactorService = EconomyService.instance();
      CobbleUtils.LOGGER.info("Impactor economy found");
    } else if (isBlankEconomyPresent()) {
      economyType = EconomyType.BLANKECONOMY;
      CobbleUtils.LOGGER.info("BlanketEconomy found");
    } else if (isCobbleDollars()) {
      economyType = EconomyType.COBBLEDOLLARS;
      CobbleUtils.LOGGER.info("CobbleDollars found");
    } else if (isSDMEconomy()) {
      economyType = EconomyType.SDM_ECONOMY;
      CobbleUtils.LOGGER.info("SDM Economy found");
    } else {
      economyType = null;
      CobbleUtils.LOGGER.error("No economy api found");
    }
  }

  private static boolean isSDMEconomy() {
    try {
      CurrencyHelper.getAllCurrencyKeys();
      return true;
    } catch (NoClassDefFoundError | Exception e) {
      CobbleUtils.LOGGER.error("SDM Economy not found");
      return false;
    }
  }

  private static boolean isVaultPresent() {
    try {
      if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
        CobbleUtils.LOGGER.info("Cannot find Vault!");
      } else {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
          CobbleUtils.LOGGER.info("Registered Service Provider for Economy.class not found");
        } else {
          vaultEconomy = rsp.getProvider();
          CobbleUtils.LOGGER.info("Economy successfully hooked up");
          CobbleUtils.LOGGER.info("Economy: " + vaultEconomy.getName());
          return true;
        }
      }
    } catch (IllegalStateException | NullPointerException | NoClassDefFoundError e) {
      CobbleUtils.LOGGER.error("Vault not found");
      return false;
    }
    return false;
  }

  private static boolean isCobbleDollars() {
    try {
      CobbleDollars.INSTANCE.getConfig();
      return true;
    } catch (IllegalStateException | NullPointerException | NoClassDefFoundError e) {
      CobbleUtils.LOGGER.error("CobbleDollars not found");
      return false;
    }
  }

  private static boolean isBlankEconomyPresent() {
    try {
      BlanketEconomy.INSTANCE.initialize(CobbleUtils.server);
      BlanketEconomy.INSTANCE.getAPI();
      return true;
    } catch (IllegalStateException | NullPointerException | NoClassDefFoundError e) {
      CobbleUtils.LOGGER.error("BlanketEconomy not found");
      return false;
    }
  }


  /**
   * Method to check if the impactor api is present.
   *
   * @return true if the api is present.
   */
  public static boolean isImpactorPresent() {
    try {
      EconomyService service = EconomyService.instance();
      return service != null;
    } catch (IllegalStateException | NullPointerException | NoClassDefFoundError e) {
      CobbleUtils.LOGGER.error("Impactor not found");
      return false;
    }

  }


  /**
   * Method to get an account from the impactor api.
   *
   * @param uuid     The uuid of the account.
   * @param currency The currency of the account.
   *
   * @return The account.
   */
  public static Account getAccount(UUID uuid, String currency) {
    if (!impactorService.hasAccount(uuid).join()) {
      return impactorService.account(uuid).join();
    }
    return impactorService.account(getCurrency(currency), uuid).join();
  }

  /**
   * Method to get an account from the impactor api.
   *
   * @param uuid The uuid of the account.
   *
   * @return The account.
   */
  public static Account getAccount(UUID uuid) {
    if (!impactorService.hasAccount(uuid).join()) {
      return impactorService.account(uuid).join();
    }
    return impactorService.account(uuid).join();
  }

  /**
   * Method to add to the balance of an account.
   *
   * @param player   The account to add the balance to.
   * @param currency The currency to add the balance to.
   * @param amount   The amount to add.
   *
   * @return true if the transaction was successful.
   */
  public static boolean addMoney(ServerPlayerEntity player, String currency, BigDecimal amount) {
    return addMoney(player, currency, amount, true);
  }

  public static boolean addMoney(ServerPlayerEntity player, String currency, BigDecimal amount, boolean notify) {
    switch (economyType) {
      case IMPACTOR: {
        Account account = getAccount(player.getUuid(), currency);
        EconomyTransaction transaction = account.deposit(amount);
        if (transaction.successful() && notify) {
          PlayerUtils.sendMessage(
            player,
            CobbleUtils.shopLang.getMessageAddMoney()
              .replace("%price%", formatCurrency(amount, account.currency()))
              .replace("%amount%", formatCurrency(amount, account.currency()))
              .replace("%balance%", formatCurrency(account.balance(), account.currency()))
              .replace("%symbol%", getSymbol(account.currency()))
              .replace("%currency%", getCurrencyName(account.currency())),
            CobbleUtils.shopLang.getPrefix()
          );
          return true;
        }
        return false;
      }
      case VAULT: {
        return vaultEconomy.depositPlayer(player.getGameProfile().getName(), amount.doubleValue()).transactionSuccess();
      }
      case BLANKECONOMY: {
        BlanketEconomy.INSTANCE.getAPI().addBalance(player.getUuid(), amount, currency);
        return true;
      }
      case COBBLEDOLLARS: {
        ((CobbleDollarsPlayer) player).cobbleDollars$addCobbleDollars(BigInteger.valueOf(amount.longValue()));
        return true;
      }
      case SDM_ECONOMY: {
        CurrencyHelper.addMoney(player, currency, amount.longValue());
        return true;
      }
      default:
        return false;
    }
  }

  /**
   * Method to remove a balance from an account.
   *
   * @param player   The player to remove the balance from.
   * @param amount   The amount to remove from the account.
   * @param currency The currency to remove the balance from.
   *
   * @return true if the transaction was successful.
   */
  public static boolean removeMoney(ServerPlayerEntity player, String currency, BigDecimal amount) {

    switch (economyType) {
      case IMPACTOR:
        Account account;
        if (currency == null || currency.isEmpty()) {
          account = getAccount(player.getUuid());
        } else {
          account = getAccount(player.getUuid(), currency);
        }
        EconomyTransaction transaction = account.withdraw(amount);
        return transaction.successful();
      case VAULT:
        return vaultEconomy.bankWithdraw(player.getGameProfile().getName(), amount.doubleValue()).transactionSuccess();
      case BLANKECONOMY:
        BigDecimal bal = BlanketEconomy.INSTANCE.getAPI().getBalance(player.getUuid(), currency);
        BlanketEconomy.INSTANCE.getAPI().setBalance(player.getUuid(), bal.subtract(amount), currency);
        return true;
      case COBBLEDOLLARS:
        BigInteger balance = ((CobbleDollarsPlayer) player).cobbleDollars$getCobbleDollars();
        if (balance.compareTo(BigInteger.valueOf(amount.longValue())) >= 0) {
          ((CobbleDollarsPlayer) player).cobbleDollars$setCobbleDollars(balance.subtract(BigInteger.valueOf(amount.longValue())));
          return true;
        }
        return false;
      case SDM_ECONOMY:
        CurrencyHelper.setMoney(player, currency, getBalance(player, currency).longValue() - amount.longValue());
        return true;
      default:
        return false;
    }
  }

  /**
   * Method to add to the balance of an account.
   *
   * @param account The account to add the balance to.
   * @param amount  The amount to add.
   *
   * @return true if the transaction was successful.
   */
  public static boolean removeMoney(Account account, BigDecimal amount) {
    EconomyTransaction transaction = account.withdraw(amount);
    return transaction.successful();
  }

  /**
   * Method to check if an account has enough balance and optionally remove the
   * amount.
   *
   * @param account The account to check.
   * @param amount  The amount to check for.
   *
   * @return true if the account has enough balance.
   */
  public static boolean hasEnoughImpactor(Account account, BigDecimal amount) {
    if (account.balance().compareTo(amount) >= 0) {
      removeMoney(account, amount);
      sendMessage(account, amount, CobbleUtils.shopLang.getMessageBought());
      return true;
    } else {
      sendMessage(account, amount, CobbleUtils.shopLang.getMessageNotHaveMoney());
      return false;
    }
  }

  private static void sendMessage(Account account, BigDecimal amount, String messageNotHaveMoney) {
    try {
      UUID player = account.owner();
      Currency currency = account.currency();
      PlayerUtils.sendMessage(CobbleUtils.server.getPlayerManager().getPlayer(player),
        messageNotHaveMoney
          .replace("%price%", formatCurrency(amount, currency))
          .replace("%balance%", formatCurrency(account.balance(), currency))
          .replace("%symbol%", getSymbol(currency))
          .replace("%currency%", getCurrencyName(currency)),
        CobbleUtils.shopLang.getPrefix());
    } catch (NoSuchMethodError | Exception e) {
      e.printStackTrace();
    }
  }

  private static void sendMessage(ServerPlayerEntity player, BigDecimal amount, String messageNotHaveMoney) {
    try {
      PlayerUtils.sendMessage(player,
        messageNotHaveMoney
          .replace("%price%", formatCurrency(amount, ""))
          .replace("%balance%", formatCurrency(getBalance(player, ""), ""))
          .replace("%symbol%", getSymbol(""))
          .replace("%currency%", ""),
        CobbleUtils.shopLang.getPrefix());
    } catch (NoSuchMethodError | Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Method to check if an account has enough balance and optionally remove the
   * amount.
   *
   * @param player   The player to check.
   * @param currency The currency to check for.
   * @param amount   The amount to check for.
   *
   * @return true if the account has enough balance.
   */
  public static boolean hasEnough(ServerPlayerEntity player, String currency, BigDecimal amount) {
    return hasEnough(player, currency, amount, true);
  }

  public static boolean hasEnough(ServerPlayerEntity player, String currency, BigDecimal amount, boolean notify) {
    switch (economyType) {
      case IMPACTOR:
        return hasEnoughImpactor(getAccount(player.getUuid(), currency), amount);
      case VAULT:
        if (vaultEconomy.has(player.getGameProfile().getName(), amount.doubleValue())) {
          vaultEconomy.withdrawPlayer(player.getGameProfile().getName(), amount.doubleValue());
          if (notify) sendMessage(player, amount, CobbleUtils.shopLang.getMessageBought());
          return true;
        }
        if (notify) sendMessage(player, amount, CobbleUtils.shopLang.getMessageNotHaveMoney());
        return false;
      case BLANKECONOMY:
        BigDecimal bal = getBalance(player, currency);
        if (bal.compareTo(amount) >= 0) {
          BlanketEconomy.INSTANCE.getAPI().setBalance(player.getUuid(), bal.subtract(amount), currency);
          if (notify) sendMessage(player, amount, CobbleUtils.shopLang.getMessageBought());
          return true;
        }
        if (notify) sendMessage(player, amount, CobbleUtils.shopLang.getMessageNotHaveMoney());
        return false;
      case COBBLEDOLLARS:
        BigInteger balance = ((CobbleDollarsPlayer) player).cobbleDollars$getCobbleDollars();
        if (balance.compareTo(BigInteger.valueOf(amount.longValue())) >= 0) {
          ((CobbleDollarsPlayer) player).cobbleDollars$setCobbleDollars(balance.subtract(BigInteger.valueOf(amount.longValue())));
          if (notify) sendMessage(player, amount, CobbleUtils.shopLang.getMessageBought());
          return true;
        }
        if (notify) sendMessage(player, amount, CobbleUtils.shopLang.getMessageNotHaveMoney());
        return false;
      case SDM_ECONOMY:
        long balance1 = CurrencyHelper.getMoney(player, currency);
        if (balance1 >= amount.longValue()) {
          CurrencyHelper.setMoney(player, currency, balance1 - amount.longValue());
          if (notify) sendMessage(player, amount, CobbleUtils.shopLang.getMessageBought());
          return true;
        }
        if (notify) sendMessage(player, amount, CobbleUtils.shopLang.getMessageNotHaveMoney());
        return false;
      default:
        return false;
    }
  }


  /**
   * Method to format a BigDecimal to a currency string.
   *
   * @param amount   The balance to format.
   * @param currency The currency to format the balance to.
   *
   * @return The formatted balance with the format of Country player.
   */
  public static String formatCurrency(BigDecimal amount, Currency currency) {
    return formatCurrency(amount, getCurrencyName(currency));
  }


  @Deprecated(forRemoval = true, since = "1.1.3 - 07/01/2025")
  public static String formatCurrency(BigDecimal amount, Currency currency, UUID player) {
    return formatCurrency(amount, currency.key().asString(), player);
  }

  @Deprecated(forRemoval = true, since = "1.1.3 - 07/01/2025")
  public static String formatCurrency(BigDecimal amount, String currency, UUID player) {
    return formatCurrency(amount, currency);
  }

  /**
   * Method to format a BigDecimal to a currency string.
   *
   * @param amount   The balance to format.
   * @param currency The currency to format the balance to.
   *
   * @return The formatted balance with the format of Country player.
   */
  public static String formatCurrency(BigDecimal amount, String currency) {
    switch (economyType) {
      case IMPACTOR -> {
        if (!currency.contains(":")) currency = "impactor:" + currency;
        String json = "";
        try {
          json =
            GsonComponentSerializer.gson().serialize(impactorService.currencies().currency(Key.key(currency)).get().format(amount));
          if (json.contains("text")) {
            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
            return jsonObject.get("text").getAsString();
          } else {
            return json.replace("\"", "");
          }
        } catch (IllegalStateException e) {
          return json;
        } catch (NoSuchElementException e) {
          e.printStackTrace();
          CobbleUtils.LOGGER.error("Not found Currency -> " + currency + "| Amount ->" +
            " " + amount);
          return CobbleUtils.language.getDefaultSymbol() + amount;
        } catch (Exception e) {
          CobbleUtils.LOGGER.error("Error formatting currency -> " + currency + "| Amount ->" +
            " " + amount);
          e.printStackTrace();
          return CobbleUtils.language.getDefaultSymbol() + amount;
        }
      }
      case BLANKECONOMY -> {
        return amount + " " + BlanketEconomy.INSTANCE.getAPI().getCurrencySymbol(currency);
      }
      case VAULT -> {
        return vaultEconomy.format(amount.doubleValue());
      }
      default -> {
        return CobbleUtils.language.getDefaultSymbol() + amount;
      }
    }
  }


  /**
   * Method to get the currency from the impactor api.
   *
   * @param currency The currency to get.
   *
   * @return The currency.
   */
  public static Currency getCurrency(@Subst("") String currency) {
    try {
      if (currency == null || currency.isEmpty()) return impactorService.currencies().primary();
      if (!currency.contains(":")) currency = "impactor:" + currency;
      return impactorService.currencies().currency(Key.key(currency)).get();
    } catch (NoSuchMethodError e) {
      CobbleUtils.LOGGER.error("Currency -> " + currency + "| Key -> " + Key.key(currency).asString());
      e.printStackTrace();
      return impactorService.currencies().primary();
    } catch (InvalidKeyException e) {
      e.printStackTrace();
      CobbleUtils.LOGGER.error("Currency -> " + currency + "| Key -> " + Key.key(currency).asString());
      return impactorService.currencies().primary();
    }
  }

  /**
   * Method to get the currency symbol.
   *
   * @param currency The currency to get the symbol for.
   *
   * @return The currency symbol.
   */
  public static String getSymbol(@Subst("") String currency) {
    try {
      if (currency == null || currency.isEmpty()) {
        if (CobbleUtils.config.isDebug()) {
          CobbleUtils.LOGGER.error("Currency is null or empty");
        }
        return CobbleUtils.language.getDefaultSymbol();
      }
      return switch (economyType) {
        case IMPACTOR -> {
          var optionalCurrency = impactorService.currencies().currency(Key.key(currency));
          if (optionalCurrency.isEmpty()) {
            CobbleUtils.LOGGER.error("Currency not found -> " + currency);
            yield CobbleUtils.language.getDefaultSymbol();
          }
          var c = optionalCurrency.get();
          if (c == null) {
            CobbleUtils.LOGGER.error("Currency is null -> " + currency);
            yield CobbleUtils.language.getDefaultSymbol();
          }
          String symbol = GsonComponentSerializer.gson().serialize(c.symbol());
          if (CobbleUtils.config.isDebug()) {
            CobbleUtils.LOGGER.info("Symbol -> " + symbol);
          }
          yield symbol;
        }
        case VAULT -> CobbleUtils.language.getDefaultSymbol();
        case BLANKECONOMY -> BlanketEconomy.INSTANCE.getAPI().getCurrencySymbol(currency);
        case COBBLEDOLLARS -> CobbleUtils.language.getDefaultSymbol();
        default -> CobbleUtils.language.getDefaultSymbol();
      };
    } catch (NoSuchMethodError | Exception e) {
      e.printStackTrace();
      return "Error getting symbol -> " + currency;
    }
  }

  /**
   * Method to get the currency symbol.
   *
   * @param currency The currency to get the symbol for.
   *
   * @return The currency symbol.
   */
  public static String getSymbol(Currency currency) {
    try {
      String key = currency.key().asString();
      return GsonComponentSerializer.gson().serialize(impactorService.currencies().currency(Key.key(key)).get().symbol());

    } catch (NoSuchMethodError | Exception | NoClassDefFoundError e) {
      return CobbleUtils.language.getDefaultSymbol();
    }
  }

  public static String getCurrencyName(Currency currency) {
    try {
      return switch (economyType) {
        case IMPACTOR -> currency.key().asString();

        default -> CobbleUtils.language.getDefaultSymbol();
      };
    } catch (NoSuchMethodError | Exception | NoClassDefFoundError e) {
      e.printStackTrace();
      return CobbleUtils.language.getDefaultSymbol();
    }
  }

  /**
   * Method to get the balance of an account.
   *
   * @param player   The player to get the balance for.
   * @param currency The currency to get the balance for.
   *
   * @return The balance of the account.
   */
  public static BigDecimal getBalance(ServerPlayerEntity player, @Subst("") String currency) {
    return switch (economyType) {
      case IMPACTOR -> getAccount(player.getUuid(), currency).balance();
      case VAULT -> {
        double vaultBalance = vaultEconomy.getBalance(player.getGameProfile().getName());
        // Asegurarse de que el valor tenga 2 decimales y redondeo apropiado
        yield BigDecimal.valueOf(vaultBalance).setScale(2, RoundingMode.HALF_UP);
      }
      case BLANKECONOMY -> {
        BigDecimal blanketBalance = BlanketEconomy.INSTANCE.getAPI().getBalance(player.getUuid(), currency);
        // Redondear el balance si es necesario
        yield blanketBalance.setScale(2, RoundingMode.HALF_UP);
      }
      case COBBLEDOLLARS ->
        BigDecimal.valueOf(((CobbleDollarsPlayer) player).cobbleDollars$getCobbleDollars().longValue());
      case SDM_ECONOMY -> BigDecimal.valueOf(CurrencyHelper.getMoney(player, currency));
      default -> BigDecimal.ZERO;
    };
  }


  public static void setMoney(ServerPlayerEntity player, String curreny, BigDecimal money) {
    switch (economyType) {
      case IMPACTOR -> getAccount(player.getUuid(), curreny).set(money);
      case VAULT -> vaultEconomy.depositPlayer(player.getGameProfile().getName(), money.doubleValue());
      case BLANKECONOMY -> BlanketEconomy.INSTANCE.getAPI().setBalance(player.getUuid(), money, curreny);
      case COBBLEDOLLARS ->
        ((CobbleDollarsPlayer) player).cobbleDollars$setCobbleDollars(BigInteger.valueOf(money.longValue()));
      case SDM_ECONOMY -> CurrencyHelper.setMoney(player, curreny, money.longValue());
    }
  }
}
