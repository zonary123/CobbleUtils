package com.kingpixel.cobbleutils.util.placeholders;

import org.jetbrains.annotations.Nullable;

/**
 * Functional interface for simple global placeholders (no player required).
 */
@FunctionalInterface
public interface SimpleGlobalPlaceholder {
  /**
   * Evaluates the global placeholder request.
   *
   * @param argument The placeholder argument.
   * @return The result value.
   */
  Object handle(@Nullable String argument);
}
