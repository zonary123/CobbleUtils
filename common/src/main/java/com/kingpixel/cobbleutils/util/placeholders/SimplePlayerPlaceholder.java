package com.kingpixel.cobbleutils.util.placeholders;

import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Functional interface for simple player-scoped placeholders.
 */
@FunctionalInterface
public interface SimplePlayerPlaceholder {
  /**
   * Evaluates the placeholder request for a given player.
   *
   * @param player   The player entity.
   * @param argument The placeholder argument.
   * @return The result value.
   */
  Object handle(@Nullable ServerPlayerEntity player, @Nullable String argument);
}
