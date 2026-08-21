package com.kingpixel.cobbleutils.util.placeholders;

import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter provider for PB4 PlaceholderAPI (eu.pb4.placeholders.api).
 */
public class PB4PlaceholderProvider implements PlaceholderProvider {
  private static final String ID = "PlaceholderAPI";
  private static final Logger LOGGER = LogManager.getLogger("CobbleUtils-Placeholders");
  private final Map<String, Identifier> registeredIdentifiers = new ConcurrentHashMap<>();
  private Boolean available = null;

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public boolean isAvailable() {
    if (available == null) {
      try {
        Class.forName("eu.pb4.placeholders.api.Placeholders");
        available = true;
      } catch (Throwable e) {
        available = false;
      }
    }
    return available;
  }

  @Override
  public void register(
    String namespace,
    String key,
    UnifiedPlaceholderHandler handler,
    boolean isAudience,
    boolean isRelational
  ) {
    if (!isAvailable()) return;
    try {
      String fullKey = (namespace + ":" + key).toLowerCase();
      Identifier identifier = Identifier.of(namespace.toLowerCase(), key.toLowerCase());

      Placeholders.register(identifier, (PlaceholderContext ctx, String argument) -> {
        try {
          ServerPlayerEntity player = ctx.hasPlayer() ? ctx.player() : null;
          CobblePlaceholderContext cobbleContext = new CobblePlaceholderContext(
            player,
            null,
            null,
            null,
            ctx.server(),
            ctx.world(),
            ctx.view(),
            argument,
            null,
            null
          );
          Object value = handler.handle(cobbleContext);
          return PlaceholderValueConverter.toPB4Result(value, player);
        } catch (Throwable e) {
          LOGGER.error("Error evaluating PB4 placeholder for " + fullKey, e);
          return PlaceholderResult.invalid();
        }
      });

      registeredIdentifiers.put(fullKey, identifier);
    } catch (Throwable e) {
      LOGGER.error("Failed to register PB4 placeholder [" + namespace + ":" + key + "]", e);
    }
  }

  @Override
  public void unregister(String namespace, String key) {
    if (!isAvailable()) return;
    try {
      String fullKey = (namespace + ":" + key).toLowerCase();
      Identifier id = registeredIdentifiers.remove(fullKey);
      if (id != null) {
        Placeholders.remove(id);
      }
    } catch (Throwable e) {
      LOGGER.error("Failed to unregister PB4 placeholder [" + namespace + ":" + key + "]", e);
    }
  }

  @Override
  public void unregisterNamespace(String namespace) {
    if (!isAvailable()) return;
    try {
      String prefix = namespace.toLowerCase() + ":";
      registeredIdentifiers.entrySet().removeIf(entry -> {
        if (entry.getKey().startsWith(prefix)) {
          try {
            Placeholders.remove(entry.getValue());
          } catch (Throwable ignored) {
          }
          return true;
        }
        return false;
      });
    } catch (Throwable e) {
      LOGGER.error("Failed to unregister PB4 namespace [" + namespace + "]", e);
    }
  }
}
