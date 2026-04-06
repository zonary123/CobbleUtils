package com.kingpixel.cobbleutils.util.economys.v1;

import com.kingpixel.cobbleutils.CobbleUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 16/03/2025 3:30
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class VaultEconomy extends EconomyAbstract {
  public static final String IDENTIFY = "VAULT";
  private Economy service;

  public VaultEconomy() {

  }

  @Override
  public String getIdentify() {
    return IDENTIFY;
  }

  @Override
  public boolean isPresent() {
    if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
      CobbleUtils.LOGGER_RAW.info("Cannot find Vault!");
      List<String> plugins = new ArrayList<>();
      for (Plugin plugin : Bukkit.getServer().getPluginManager().getPlugins()) {
        plugins.add(plugin.getName());
      }
      CobbleUtils.LOGGER_RAW.info("Report this to zonary123 Plugins to Vault -> " + plugins);
    } else {
      RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
      if (rsp == null) {
        CobbleUtils.LOGGER_RAW.info("Registered Service Provider for Economy.class not found");
      } else {
        service = rsp.getProvider();
        CobbleUtils.LOGGER_RAW.info("Economy successfully hooked up");
        CobbleUtils.LOGGER_RAW.info("Economy: " + service.getName());
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean deposit(UUID playerUuid, BigDecimal money, String currency) {
    return service.depositPlayer(Bukkit.getOfflinePlayer(playerUuid), money.doubleValue()).transactionSuccess();
  }

  @Override
  public boolean withdraw(UUID playerUuid, BigDecimal money, String currency) {
    return service.withdrawPlayer(Bukkit.getOfflinePlayer(playerUuid), money.doubleValue()).transactionSuccess();
  }

  @Override
  public BigDecimal getBalance(UUID playerUuid, String currency) {
    return BigDecimal.valueOf(service.getBalance(Bukkit.getOfflinePlayer(playerUuid)));
  }

  @Override
  public String format(BigDecimal money, String currency) {
    return service.format(money.doubleValue());
  }

  @Override
  public boolean setBalance(UUID playerUuid, BigDecimal money, String currency) {
    var offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
    if (service.hasAccount(offlinePlayer)) {
      service.withdrawPlayer(offlinePlayer, getBalance(playerUuid, currency).doubleValue());
      return service.depositPlayer(offlinePlayer, money.doubleValue()).transactionSuccess();
    }
    return false;
  }

  @Override
  public int getDecimals(String currency) {
    return CobbleUtils.config.getDecimals();
  }
}
