package com.kingpixel.cobbleutils.util.economys;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.Priority;
import com.kingpixel.cobbleutils.Model.PriorityEconomy;
import com.kingpixel.cobbleutils.util.economys.providers.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Modern Economy API (V2) for CobbleUtils.
 * Provides a registry for async-first economy providers.
 */
public class EconomyApi {
  private static final Map<String, Economy> ECONOMIES = new ConcurrentHashMap<>();
  private static boolean initialized = false;

  private EconomyApi() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * Initializes the economy system by registering built-in providers.
   * Only runs once.
   */
  public static synchronized void init() {
    if (initialized) return;

    registerBuiltIn(new ImpactorEconomy());
    registerBuiltIn(new VaultEconomy());
    registerBuiltIn(new BeEconomy());
    registerBuiltIn(new CobbleDollarsEconomy());
    registerBuiltIn(new PebbleEconomy());
    registerBuiltIn(new SDMEconomy());
    registerBuiltIn(new UltraEEconomy());

    initialized = true;
    CobbleUtils.LOGGER_RAW.info("Economy System V2 initialized. Registered: {}",
      ECONOMIES.values().stream().map(Economy::getIdentify).collect(Collectors.joining(", ")));
  }

  private static void registerBuiltIn(Economy economy) {
    try {
      if (economy.isPresent()) {
        register(economy);
      }
    } catch (Throwable ignored) {
      // Silently ignore if provider dependencies are missing
    }
  }

  /**
   * Registers a new economy provider.
   *
   * @param economy The economy provider to register.
   */
  public static void register(Economy economy) {
    ECONOMIES.put(economy.getIdentify().toUpperCase(), economy);
  }

  /**
   * Gets an economy provider by its ID.
   *
   * @param id The ID of the economy (e.g., "IMPACTOR", "VAULT").
   * @return The economy provider, or null if not found.
   */
  public static Economy getEconomy(String id) {
    if (!initialized) init();
    if (id == null) return getHighestPriorityEconomy();

    Economy economy = ECONOMIES.get(id.toUpperCase());
    return (economy != null) ? economy : getHighestPriorityEconomy();
  }

  /**
   * Gets all registered economies.
   *
   * @return A collection of registered economies.
   */
  public static Collection<Economy> getEconomies() {
    if (!initialized) init();
    return Collections.unmodifiableCollection(ECONOMIES.values());
  }

  /**
   * Gets the highest priority economy from the registered ones.
   *
   * @return The highest priority economy, or null if no economies are registered.
   */
  public static Economy getHighestPriorityEconomy() {
    if (!initialized) init();
    if (ECONOMIES.isEmpty()) return null;

    List<Economy> available = new ArrayList<>(ECONOMIES.values());
    if (available.size() == 1) return available.getFirst();

    return available.stream()
      .min(Comparator.comparing(e -> getPriority(e.getIdentify())))
      .orElse(available.getFirst());
  }

  private static Priority getPriority(String id) {
    return CobbleUtils.config.getPriorityEconomy().stream()
      .filter(pe -> pe.getEconomyId().equalsIgnoreCase(id))
      .findFirst()
      .map(PriorityEconomy::getPriority)
      .orElse(Priority.LOWEST);
  }
}
