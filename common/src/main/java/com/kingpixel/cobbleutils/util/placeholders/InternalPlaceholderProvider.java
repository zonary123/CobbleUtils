package com.kingpixel.cobbleutils.util.placeholders;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Internal fallback placeholder provider that enables direct evaluation within
 * CobbleUtils strings, lore, and chat without requiring third-party mods.
 */
public class InternalPlaceholderProvider implements PlaceholderProvider {
  private static final String ID = "Internal";
  private static final Logger LOGGER = LogManager.getLogger("CobbleUtils-Placeholders");
  private static final Pattern PATTERN_PERCENT = Pattern.compile("%([a-zA-Z0-9_-]+)[:_]([a-zA-Z0-9_-]+)(?:[ _]([a-zA-Z0-9_.-]+))?%");
  private static final Pattern PATTERN_BRACKET = Pattern.compile("<([a-zA-Z0-9_-]+):([a-zA-Z0-9_-]+)(?::([a-zA-Z0-9_.-]+))?>");

  private final Map<String, UnifiedPlaceholderHandler> registry = new ConcurrentHashMap<>();

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public boolean isAvailable() {
    return true;
  }

  @Override
  public void register(
    String namespace,
    String key,
    UnifiedPlaceholderHandler handler,
    boolean isAudience,
    boolean isRelational
  ) {
    if (namespace == null || key == null || handler == null) return;
    try {
      String id = (namespace + ":" + key).toLowerCase();
      registry.put(id, handler);
    } catch (Throwable e) {
      LOGGER.error("Failed to register internal placeholder [" + namespace + ":" + key + "]", e);
    }
  }

  @Override
  public void unregister(String namespace, String key) {
    if (namespace == null || key == null) return;
    try {
      String id = (namespace + ":" + key).toLowerCase();
      registry.remove(id);
    } catch (Throwable e) {
      LOGGER.error("Failed to unregister internal placeholder [" + namespace + ":" + key + "]", e);
    }
  }

  @Override
  public void unregisterNamespace(String namespace) {
    if (namespace == null) return;
    try {
      String prefix = namespace.toLowerCase() + ":";
      registry.keySet().removeIf(k -> k.startsWith(prefix));
    } catch (Throwable e) {
      LOGGER.error("Failed to unregister internal namespace [" + namespace + "]", e);
    }
  }

  /**
   * Evaluates and replaces all registered placeholders in the given message string.
   */
  public String replace(String message, CobblePlaceholderContext baseContext) {
    if (message == null || message.isEmpty() || registry.isEmpty()) {
      return message;
    }

    String result = message;
    try {
      // 1. Replace %namespace:key% or %namespace_key_arg%
      if (result.contains("%")) {
        Matcher matcher = PATTERN_PERCENT.matcher(result);
        StringBuilder sb = new StringBuilder(result.length());
        while (matcher.find()) {
          String ns = matcher.group(1).toLowerCase();
          String key = matcher.group(2).toLowerCase();
          String arg = matcher.group(3);

          String id = ns + ":" + key;
          UnifiedPlaceholderHandler handler = registry.get(id);
          if (handler != null) {
            try {
              CobblePlaceholderContext ctx = new CobblePlaceholderContext(
                baseContext.getPlayer(),
                baseContext.getAudience(),
                baseContext.getTargetPlayer(),
                baseContext.getTargetAudience(),
                baseContext.getServer(),
                baseContext.getWorld(),
                baseContext.getTarget(),
                arg,
                null,
                null
              );
              Object val = handler.handle(ctx);
              String strVal = PlaceholderValueConverter.toStringValue(val);
              matcher.appendReplacement(sb, Matcher.quoteReplacement(strVal != null ? strVal : matcher.group(0)));
            } catch (Throwable e) {
              LOGGER.error("Error evaluating internal placeholder " + id, e);
              matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
          } else {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
          }
        }
        matcher.appendTail(sb);
        result = sb.toString();
      }

      // 2. Replace <namespace:key:arg> if present
      if (result.contains("<") && result.contains(">")) {
        Matcher matcher = PATTERN_BRACKET.matcher(result);
        StringBuilder sb = new StringBuilder(result.length());
        while (matcher.find()) {
          String ns = matcher.group(1).toLowerCase();
          String key = matcher.group(2).toLowerCase();
          String arg = matcher.group(3);

          String id = ns + ":" + key;
          UnifiedPlaceholderHandler handler = registry.get(id);
          if (handler != null) {
            try {
              CobblePlaceholderContext ctx = new CobblePlaceholderContext(
                baseContext.getPlayer(),
                baseContext.getAudience(),
                baseContext.getTargetPlayer(),
                baseContext.getTargetAudience(),
                baseContext.getServer(),
                baseContext.getWorld(),
                baseContext.getTarget(),
                arg,
                null,
                null
              );
              Object val = handler.handle(ctx);
              String strVal = PlaceholderValueConverter.toStringValue(val);
              matcher.appendReplacement(sb, Matcher.quoteReplacement(strVal != null ? strVal : matcher.group(0)));
            } catch (Throwable e) {
              matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
          } else {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
          }
        }
        matcher.appendTail(sb);
        result = sb.toString();
      }
    } catch (Throwable e) {
      LOGGER.error("Error during internal placeholder replacement", e);
    }
    return result;
  }
}
