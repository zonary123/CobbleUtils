package com.kingpixel.cobbleutils.util.placeholders;

/**
 * Service provider interface for placeholder engines.
 */
public interface PlaceholderProvider {
  /**
   * Identifies the provider engine.
   */
  String getId();

  /**
   * Verifies if the provider engine classes and runtime environment are active.
   */
  boolean isAvailable();

  /**
   * Registers a placeholder into this provider.
   *
   * @param namespace    The namespace (e.g. "cobbleutils")
   * @param key          The placeholder key (e.g. "party_count")
   * @param handler      The unified evaluation handler
   * @param isAudience   Whether this placeholder targets an audience/player
   * @param isRelational Whether this placeholder is relational (two players)
   */
  void register(String namespace, String key, UnifiedPlaceholderHandler handler, boolean isAudience, boolean isRelational);

  /**
   * Unregisters a specific placeholder.
   */
  void unregister(String namespace, String key);

  /**
   * Unregisters all placeholders belonging to a namespace.
   */
  void unregisterNamespace(String namespace);
}
