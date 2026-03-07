package com.kingpixel.cobbleutils.util;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.economy.EconomySelector;
import org.pokesplash.gts.api.economy.GtsEconomy;
import org.pokesplash.gts.api.economy.GtsEconomyProvider;
import org.pokesplash.gts.enumeration.Priority;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 14/03/2025 1:47
 */

/**
 * This class bridges the CobbleUtils economys system with the GTS economys system.
 * It conditionally registers the GTS economys provider if the GTS economys class is available.
 */

public class CobbleUtilsBridgeGTS implements GtsEconomy {
  private static boolean introduced = false;

  public CobbleUtilsBridgeGTS() {
    if (!introduced) {
      CobbleUtils.LOGGER.info("GtsEconomyProvider initialized");
      try {
        Class.forName("org.pokesplash.gts.api.economy.GtsEconomy");
        GtsEconomyProvider.putEconomy(Priority.HIGHEST, this);
        introduced = true;
      } catch (NoClassDefFoundError | NoSuchMethodError | Exception ignored) {
        CobbleUtils.LOGGER.warn("GtsEconomy class not found, skipping GtsEconomyProvider initialization.");
      }
    }
  }

  @Override
  public boolean add(UUID uuid, double v) {
    try {
      return getEconomy().deposit(uuid, BigDecimal.valueOf(v), "GTS").join().isSuccess();
    } catch (NoClassDefFoundError | NoSuchMethodError | Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  private EconomySelector getEconomy() {
    return CobbleUtils.config.getGtsEconomyToUse();
  }

  @Override
  public boolean remove(UUID uuid, double v) {
    try {
      return getEconomy().withdraw(uuid, BigDecimal.valueOf(v), "GTS").join().isSuccess();
    } catch (NoClassDefFoundError | NoSuchMethodError | Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  @Override
  public double balance(UUID uuid) {
    try {
      return getEconomy().getBalance(uuid).join().getAmount().doubleValue();
    } catch (NoClassDefFoundError | NoSuchMethodError | Exception e) {
      e.printStackTrace();
      return 0;
    }
  }
}

