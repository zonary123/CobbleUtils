package com.kingpixel.cobbleutils.api;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.economys.Economy;
import com.kingpixel.cobbleutils.util.economys.EconomyResponse;
import com.kingpixel.cobbleutils.util.economys.EconomySelector;
import com.kingpixel.cobbleutils.util.economys.providers.*;
import lombok.Data;
import org.jetbrains.annotations.UnknownNullability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Carlos Varas Alonso - 05/11/2024 23:58
 */
@Data
public class EconomyApi {
  private static final Map<String, Economy> ECONOMIES = new ConcurrentHashMap<>();
  private static final AtomicReference<Economy> DEFAULT_ECONOMY_REF = new AtomicReference<>();

  public synchronized static void loadEconomies() {
    if (!ECONOMIES.isEmpty()) return;
    var economies = List.of(
      new UltraEEconomy(),
      new ImpactorEconomy(),
      new CobbleDollarsEconomy(),
      new BeEconomy(),
      new PebbleEconomy(),
      new SDMEconomy(),
      new VaultEconomy()
    );
    for (Economy economy : economies) {
      try {
        if (!economy.isPresent()) {
          CobbleUtils.LOGGER.info("Economy found: " + economy.getIdentify());
          continue;
        }
        registerEconomy(economy);
      } catch (NoClassDefFoundError | IncompatibleClassChangeError | Exception e) {
        CobbleUtils.LOGGER.info("Economy not found: " + economy.getIdentify());
      }
    }
  }


  public static void registerEconomy(@Nonnull @UnknownNullability Economy economy) {
    String economyId = economy.getIdentify();
    try {
      if (ECONOMIES.putIfAbsent(economyId, economy) != null) {
        CobbleUtils.LOGGER.warn("Economy with ID '%s' is already registered.".formatted(economyId));
        return;
      }

      UUID testPlayer = UUID.randomUUID();
      String currencyId = "";

      economy.getBalance(testPlayer, currencyId)
        .whenComplete((result, ex) -> {
          if (ex == null) {
            CobbleUtils.LOGGER.warn("Economy '%s' registered successfully.".formatted(economyId));

            // Solo asigna si DEFAULT_ECONOMY es null
            if (DEFAULT_ECONOMY_REF.compareAndSet(null, economy)) {
              CobbleUtils.LOGGER.warn("Economy '%s' set as default economy.".formatted(economyId));
            }
          } else {
            ECONOMIES.remove(economyId);
          }
        });
    } catch (NoClassDefFoundError | NoSuchMethodError | Exception e) {
      ECONOMIES.remove(economyId);
    }
  }

  @Nullable
  public static Economy getEconomy(@Nonnull String economyId) {
    if (ECONOMIES.size() == 1) return DEFAULT_ECONOMY_REF.get();
    Economy economy = ECONOMIES.get(economyId);
    if (economy == null) economy = DEFAULT_ECONOMY_REF.get();
    return economy;
  }

  @Nonnull
  public static Map<String, Economy> getEconomies() {
    return ECONOMIES;
  }


  @Nonnull
  public static CompletableFuture<EconomyResponse> getBalance(@Nonnull UUID playerId, @Nonnull EconomySelector selector) {
    return selector.getBalance(playerId);
  }


  @Nonnull
  public static CompletableFuture<EconomyResponse> setBalance(@Nonnull UUID playerId, @Nonnull EconomySelector selector, @Nonnull BigDecimal amount, @Nonnull String reason) {
    return selector.setBalance(playerId, amount, reason);
  }

  @Nonnull
  public static CompletableFuture<EconomyResponse> deposit(@Nonnull UUID playerId, @Nonnull BigDecimal amount, @Nonnull String reason, @Nonnull EconomySelector selector) {
    return selector.deposit(playerId, amount, reason);
  }

  @Nonnull
  public static CompletableFuture<EconomyResponse> withdraw(@Nonnull UUID playerId, @Nonnull BigDecimal amount, @Nonnull String reason, @Nonnull EconomySelector selector) {
    return selector.withdraw(playerId, amount, reason);
  }


  public static CompletableFuture<EconomyResponse> hasEnoughMoney(@Nonnull UUID playerId, @Nonnull BigDecimal amount, @Nonnull EconomySelector selector) {
    return selector.hasEnoughMoney(playerId, amount);
  }

  @Nonnull
  public static CompletableFuture<EconomyResponse> transfer(@Nonnull UUID fromPlayerId, @Nonnull UUID toPlayerId, @Nonnull BigDecimal amount, @Nonnull String reason, @Nonnull EconomySelector selector) {
    return selector.transfer(fromPlayerId, toPlayerId, amount, reason);
  }

}
