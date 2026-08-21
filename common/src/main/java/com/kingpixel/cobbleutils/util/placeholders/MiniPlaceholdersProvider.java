package com.kingpixel.cobbleutils.util.placeholders;

import io.github.miniplaceholders.api.Expansion;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter provider for MiniPlaceholders (io.github.miniplaceholders.api).
 */
public class MiniPlaceholdersProvider implements PlaceholderProvider {
  private static final String ID = "MiniPlaceholders";
  private static final Logger LOGGER = LogManager.getLogger("CobbleUtils-Placeholders");
  private final Map<String, Map<String, PlaceholderDefinition>> namespaceDefinitions = new ConcurrentHashMap<>();
  private final Map<String, Expansion> activeExpansions = new ConcurrentHashMap<>();
  private Boolean available = null;

  @Getter
  @RequiredArgsConstructor
  private static class PlaceholderDefinition {
    private final String key;
    private final UnifiedPlaceholderHandler handler;
    private final boolean isAudience;
    private final boolean isRelational;
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public boolean isAvailable() {
    if (available == null) {
      try {
        Class.forName("io.github.miniplaceholders.api.MiniPlaceholders");
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
      String ns = namespace.toLowerCase();
      String k = key.toLowerCase();
      namespaceDefinitions.computeIfAbsent(ns, ignored -> new ConcurrentHashMap<>())
        .put(k, new PlaceholderDefinition(k, handler, isAudience, isRelational));

      rebuildNamespaceExpansion(ns);
    } catch (Throwable e) {
      LOGGER.error("Failed to register MiniPlaceholders placeholder <" + namespace + ":" + key + ">", e);
    }
  }

  @Override
  public void unregister(String namespace, String key) {
    if (!isAvailable()) return;
    try {
      String ns = namespace.toLowerCase();
      String k = key.toLowerCase();
      Map<String, PlaceholderDefinition> map = namespaceDefinitions.get(ns);
      if (map != null && map.remove(k) != null) {
        rebuildNamespaceExpansion(ns);
      }
    } catch (Throwable e) {
      LOGGER.error("Failed to unregister MiniPlaceholders placeholder <" + namespace + ":" + key + ">", e);
    }
  }

  @Override
  public void unregisterNamespace(String namespace) {
    if (!isAvailable()) return;
    try {
      String ns = namespace.toLowerCase();
      namespaceDefinitions.remove(ns);
      Expansion existing = activeExpansions.remove(ns);
      if (existing != null) {
        try {
          existing.unregister();
        } catch (Throwable ignored) {
        }
      }
    } catch (Throwable e) {
      LOGGER.error("Failed to unregister MiniPlaceholders namespace <" + namespace + ">", e);
    }
  }

  private synchronized void rebuildNamespaceExpansion(String namespace) {
    try {
      Expansion existing = activeExpansions.get(namespace);
      if (existing != null) {
        try {
          existing.unregister();
        } catch (Throwable ignored) {
        }
      }

      Map<String, PlaceholderDefinition> definitions = namespaceDefinitions.get(namespace);
      if (definitions == null || definitions.isEmpty()) {
        activeExpansions.remove(namespace);
        return;
      }

      Expansion.Builder builder = Expansion.builder(namespace)
        .author("CobbleUtils")
        .version("1.0.0");

      for (Map.Entry<String, PlaceholderDefinition> entry : definitions.entrySet()) {
        String key = entry.getKey();
        PlaceholderDefinition def = entry.getValue();

        if (def.isRelational()) {
          builder.relationalPlaceholder(key, (audience, otherAudience, queue, ctx) -> {
            try {
              CobblePlaceholderContext cobbleContext = CobblePlaceholderContext.ofMiniPlaceholdersRelational(
                audience,
                otherAudience,
                queue,
                ctx
              );
              Object value = def.getHandler().handle(cobbleContext);
              return PlaceholderValueConverter.toMiniPlaceholdersTag(value);
            } catch (Throwable e) {
              LOGGER.error("Error evaluating MiniPlaceholders relational for <" + namespace + ":" + key + ">", e);
              return null;
            }
          });
        } else if (def.isAudience()) {
          builder.audiencePlaceholder(key, (audience, queue, ctx) -> {
            try {
              CobblePlaceholderContext cobbleContext = CobblePlaceholderContext.ofMiniPlaceholders(audience, queue, ctx);
              Object value = def.getHandler().handle(cobbleContext);
              return PlaceholderValueConverter.toMiniPlaceholdersTag(value);
            } catch (Throwable e) {
              LOGGER.error("Error evaluating MiniPlaceholders audience for <" + namespace + ":" + key + ">", e);
              return null;
            }
          });
        } else {
          builder.globalPlaceholder(key, (queue, ctx) -> {
            try {
              CobblePlaceholderContext cobbleContext = CobblePlaceholderContext.ofMiniPlaceholders(null, queue, ctx);
              Object value = def.getHandler().handle(cobbleContext);
              return PlaceholderValueConverter.toMiniPlaceholdersTag(value);
            } catch (Throwable e) {
              LOGGER.error("Error evaluating MiniPlaceholders global for <" + namespace + ":" + key + ">", e);
              return null;
            }
          });
        }
      }

      Expansion newExpansion = builder.build();
      newExpansion.register();
      activeExpansions.put(namespace, newExpansion);
    } catch (Throwable e) {
      LOGGER.error("Failed to build MiniPlaceholders expansion for namespace [" + namespace + "]", e);
    }
  }
}
