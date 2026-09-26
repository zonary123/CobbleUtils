package com.kingpixel.cobbleutils.events;

/**
 * Utility tracker to manage campfire pot interaction state across mixins.
 */
public final class CampfirePotTracker {

  private CampfirePotTracker() {
    // Utility class
  }

  public static final ThreadLocal<Boolean> IN_QUICK_MOVE = ThreadLocal.withInitial(() -> Boolean.FALSE);
}
