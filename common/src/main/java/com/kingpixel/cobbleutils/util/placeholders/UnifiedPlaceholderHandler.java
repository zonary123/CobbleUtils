package com.kingpixel.cobbleutils.util.placeholders;

/**
 * Functional interface for unified placeholders.
 * Returns any value (Component, Text, String, Number, Boolean, or Object) that will be safely
 * converted to the target placeholder engine's expected format.
 */
@FunctionalInterface
public interface UnifiedPlaceholderHandler {
  /**
   * Evaluates the placeholder request.
   *
   * @param context The mutual placeholder context.
   * @return The result object, or null/empty if invalid or unhandled.
   */
  Object handle(CobblePlaceholderContext context);
}
