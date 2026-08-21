package com.kingpixel.cobbleutils.util.placeholders;

import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Functional interface for relational placeholders between two players.
 */
@FunctionalInterface
public interface RelationalPlaceholderHandler {
  /**
   * Evaluates the relational placeholder request between two players.
   *
   * @param one      The first player.
   * @param two      The second player.
   * @param argument The argument string.
   * @return The result object, or null/empty if invalid or unhandled.
   */
  Object handle(@Nullable ServerPlayerEntity one, @Nullable ServerPlayerEntity two, @Nullable String argument);
}
