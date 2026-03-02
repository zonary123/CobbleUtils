package com.kingpixel.cobbleutils.util.async;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.NonNull;

import java.util.concurrent.TimeUnit;

/**
 * UtilsAsync is a centralized manager for per-mod AsyncContexts.
 * - Each mod can have its own AsyncContext (executor + scheduler)
 * - Provides methods to create, retrieve, and shutdown contexts globally
 */
public class UtilsAsync {

  private static final Cache<@NonNull String, AsyncContext> contexts = Caffeine.newBuilder()
    .build();

  public static AsyncContext createContext(
    String modId,
    String threadName,
    int minThreads,
    int maxThreads
  ) {
    return contexts.get(modId, id ->
      new AsyncContext(threadName, minThreads, maxThreads, 1000, 60, TimeUnit.SECONDS)
    );
  }

  // Overload simple (por compatibilidad)
  public static AsyncContext createContext(String modId, String threadName) {
    return createContext(modId, threadName, 1, 1);
  }

  /**
   * Retrieves an existing AsyncContext for a mod.
   *
   * @param modId Unique identifier for the mod
   * @return The AsyncContext if it exists, otherwise null
   */
  public static AsyncContext getContext(String modId) {
    AsyncContext context = contexts.getIfPresent(modId);
    if (context == null) {
      context = createContext(modId, modId + "-async");
      contexts.put(modId, context);
    }
    return context;
  }

  /**
   * Shuts down all AsyncContexts and clears the manager.
   * Should be called on server shutdown.
   */
  public static void shutdownAll() {
    contexts.asMap().values().forEach(AsyncContext::shutdownNow);
  }
}
