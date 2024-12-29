package com.kingpixel.cobbleutils.util;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.features.breeding.Breeding;
import fr.harmex.cobbledollars.common.CobbleDollars;
import fr.harmex.cobbledollars.common.utils.CobbleDollarsPlayer;
import net.impactdev.impactor.api.economy.EconomyService;
import net.impactdev.impactor.api.economy.accounts.Account;
import net.impactdev.impactor.api.economy.currency.Currency;
import net.impactdev.impactor.api.economy.transactions.EconomyTransaction;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.milkbowl.vault.economy.Economy;
import net.minecraft.server.network.ServerPlayerEntity;
import org.blanketeconomy.api.BlanketEconomy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.intellij.lang.annotations.Subst;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
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
    COBBLEDOLLARS
  }

  public static String getBalance(ServerPlayerEntity player, String currency, int digits) {
    // Supongamos que obtienes el balance como un BigDecimal desde algún método
    BigDecimal balance = getBalance(player, currency);

    if (balance != null) {
      return formatCurrency(balance, currency, player.getUuid());
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
    } else {
      economyType = null;
      CobbleUtils.LOGGER.error("No economy api found");
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

    switch (economyType) {
      case IMPACTOR: {
        Account account = getAccount(player.getUuid(), currency);
        EconomyTransaction transaction = account.deposit(amount);
        if (transaction.successful()) {
          PlayerUtils.sendMessage(CobbleUtils.server.getPlayerManager().getPlayer(account.owner()),
            CobbleUtils.shopLang.getMessageAddMoney()
              .replace("%price%", formatCurrency(amount, account.currency(), account.owner()))
              .replace("%amount%", formatCurrency(amount, account.currency(), account.owner()))
              .replace("%balance%", formatCurrency(account.balance(), account.currency(), account.owner()))
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
      PlayerUtils.sendMessage(CobbleUtils.server.getPlayerManager().getPlayer(account.owner()),
        messageNotHaveMoney
          .replace("%price%", formatCurrency(amount, account.currency(), account.owner()))
          .replace("%balance%", formatCurrency(account.balance(), account.currency(), account.owner()))
          .replace("%symbol%", getSymbol(account.currency()))
          .replace("%currency%", getCurrencyName(account.currency())),
        CobbleUtils.shopLang.getPrefix());
    } catch (NoSuchMethodError | Exception e) {
      e.printStackTrace();
    }
  }

  private static void sendMessage(ServerPlayerEntity player, BigDecimal amount, String messageNotHaveMoney) {
    try {
      PlayerUtils.sendMessage(player,
        messageNotHaveMoney
          .replace("%price%", formatCurrency(amount, "", player.getUuid()))
          .replace("%balance%", formatCurrency(getBalance(player, ""), "", player.getUuid()))
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

    switch (economyType) {
      case IMPACTOR:
        return hasEnoughImpactor(getAccount(player.getUuid(), currency), amount);
      case VAULT:
        if (vaultEconomy.has(player.getGameProfile().getName(), amount.doubleValue())) {
          vaultEconomy.withdrawPlayer(player.getGameProfile().getName(), amount.doubleValue());
          sendMessage(player, amount, CobbleUtils.shopLang.getMessageBought());
          return true;
        }
        sendMessage(player, amount, CobbleUtils.shopLang.getMessageNotHaveMoney());
        return false;
      case BLANKECONOMY:
        BigDecimal bal = getBalance(player, currency);
        if (bal.compareTo(amount) >= 0) {
          BlanketEconomy.INSTANCE.getAPI().setBalance(player.getUuid(), bal.subtract(amount), currency);
          sendMessage(player, amount, CobbleUtils.shopLang.getMessageBought());
          return true;
        }
        sendMessage(player, amount, CobbleUtils.shopLang.getMessageNotHaveMoney());
        return false;
      case COBBLEDOLLARS:
        BigInteger balance = ((CobbleDollarsPlayer) player).cobbleDollars$getCobbleDollars();
        if (balance.compareTo(BigInteger.valueOf(amount.longValue())) >= 0) {
          ((CobbleDollarsPlayer) player).cobbleDollars$setCobbleDollars(balance.subtract(BigInteger.valueOf(amount.longValue())));
          sendMessage(player, amount, CobbleUtils.shopLang.getMessageBought());
          return true;
        }
        sendMessage(player, amount, CobbleUtils.shopLang.getMessageNotHaveMoney());
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
   * @param player   The player to get the country from.
   *
   * @return The formatted balance with the format of Country player.
   */
  public static String formatCurrency(BigDecimal amount, Currency currency, UUID player) {
    return formatCurrency(amount, getCurrencyName(currency), player);
  }

  /**
   * Method to format a BigDecimal to a currency string.
   *
   * @param amount   The balance to format.
   * @param currency The currency to format the balance to.
   * @param player   The player to get the country from.
   *
   * @return The formatted balance with the format of Country player.
   */
  public static String formatCurrency(BigDecimal amount, String currency, UUID player) {
    // Validación del nombre de la moneda
    if (currency == null || currency.isEmpty()) {
      currency = "impactor:dollars";
    }

    // Obtener información de usuario, si existe
    Breeding.UserInfo userinfo = (player != null) ? Breeding.playerCountry.get(player) : null;

    // Definir la localidad predeterminada o la del jugador
    Locale locale = (userinfo != null && userinfo.language() != null && userinfo.countryCode() != null)
      ? new Locale(userinfo.language(), userinfo.countryCode())
      : Locale.getDefault();

    // Crear el formateador de moneda para la localidad
    NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(locale);

    // Definir el número de decimales según la moneda
    int decimals = getDecimals(currency);
    currencyFormatter.setMinimumFractionDigits(decimals);
    currencyFormatter.setMaximumFractionDigits(decimals);

    // Configurar el formateador si es instancia de DecimalFormat
    if (currencyFormatter instanceof DecimalFormat) {
      DecimalFormat decimalFormat = (DecimalFormat) currencyFormatter;
      DecimalFormatSymbols symbols = decimalFormat.getDecimalFormatSymbols();

      // Establecer el símbolo de moneda (verifica que no sea nulo)
      String currencySymbol = getSymbol(currency);
      symbols.setCurrencySymbol(currencySymbol != null ? currencySymbol : "");
      decimalFormat.setDecimalFormatSymbols(symbols);

      // Aplicar el patrón de formato si es válido
      String moneyPattern = CobbleUtils.language.getFormatMoney();
      if (moneyPattern != null && !moneyPattern.isEmpty()) {
        try {
          decimalFormat.applyPattern(moneyPattern);
        } catch (IllegalArgumentException e) {
          System.err.println("Error aplicando el patrón de formato: " + e.getMessage());
        }
      }
    }

    // Formatear y devolver la cantidad
    return currencyFormatter.format(amount);
  }


  /**
   * Method to get the currency from the impactor api.
   *
   * @param currency The currency to get.
   *
   * @return The currency.
   */
  public static Currency getCurrency(String currency) {
    try {
      if (currency == null || currency.isEmpty()) {
        return impactorService.currencies().primary();
      }
      if (!currency.contains(":")) {
        currency = "impactor:" + currency;
      }
      return impactorService.currencies().currency(Key.key(currency)).orElseGet(() -> impactorService.currencies().primary());
    } catch (NoSuchMethodError e) {
      e.printStackTrace();
      return impactorService.currencies().primary();
    } catch (InvalidKeyException e) {
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
      return switch (economyType) {
        case IMPACTOR -> {
          String symbol = getCurrency(currency).symbol().insertion();
          if (symbol == null || symbol.isEmpty())
            symbol = CobbleUtils.language.getImpactorSymbols().getOrDefault(currency,
              CobbleUtils.language.getDefaultSymbol());
          yield symbol;
        }
        case VAULT -> CobbleUtils.language.getDefaultSymbol();
        case BLANKECONOMY ->
          currency.isEmpty() ? CobbleUtils.language.getDefaultSymbol() : BlanketEconomy.INSTANCE.getAPI().getCurrencySymbol(currency);
        case COBBLEDOLLARS -> CobbleUtils.language.getDefaultSymbol();
        default -> CobbleUtils.language.getDefaultSymbol();
      };
    } catch (NoSuchMethodError | Exception e) {
      return CobbleUtils.language.getImpactorSymbols().getOrDefault(currency,
        CobbleUtils.language.getDefaultSymbol());
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
      String symbol = currency.symbol().insertion();
      if (symbol == null)
        symbol = CobbleUtils.language.getImpactorSymbols().getOrDefault(currency, CobbleUtils.language.getDefaultSymbol());
      return symbol;
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
      default -> BigDecimal.ZERO;
    };
  }

}
