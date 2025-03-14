package com.kingpixel.cobbleutils.util;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.api.EconomyApi;
import net.minecraft.server.network.ServerPlayerEntity;
import org.pokesplash.gts.api.economy.GtsEconomy;
import org.pokesplash.gts.api.economy.GtsEconomyProvider;
import org.pokesplash.gts.enumeration.Priority;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 14/03/2025 1:47
 */

/**
 * This class bridges the CobbleUtils economy system with the GTS economy system.
 * It conditionally registers the GTS economy provider if the GTS economy class is available.
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

  @Override public boolean add(UUID uuid, double v) {
    try {
      ServerPlayerEntity player = getPlayer(uuid);
      if (player == null) {
        CobbleUtils.LOGGER.warn("Player " + uuid + " not found!");
        return false;
      }
      return EconomyApi.addMoney(player, BigDecimal.valueOf(v), "");
    } catch (NoClassDefFoundError | NoSuchMethodError | Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  @Override public boolean remove(UUID uuid, double v) {
    try {
      ServerPlayerEntity player = getPlayer(uuid);
      if (player == null) {
        CobbleUtils.LOGGER.warn("Player " + uuid + " not found!");
        return false;
      }
      return EconomyApi.removeMoney(player, BigDecimal.valueOf(v), "");
    } catch (NoClassDefFoundError | NoSuchMethodError | Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  @Override public double balance(UUID uuid) {
    try {
      ServerPlayerEntity player = getPlayer(uuid);
      if (player == null) {
        CobbleUtils.LOGGER.warn("Player " + uuid + " not found!");
        return 0;
      }
      double money = EconomyApi.getMoney(player, "").doubleValue();
      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER.info("Player " + uuid + " has " + money);
      }
      return money;
    } catch (NoClassDefFoundError | NoSuchMethodError | Exception e) {
      e.printStackTrace();
      return 0;
    }
  }

  private ServerPlayerEntity getPlayer(UUID uuid) {
    try {
      return CobbleUtils.server.getPlayerManager().getPlayer(uuid);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}

