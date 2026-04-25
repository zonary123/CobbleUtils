package com.kingpixel.cobbleutils.util.async;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.NonNull;

import java.util.concurrent.TimeUnit;

/**
 * UtilsAsync is a centralized manager for per-mod AsyncContexts.
 * - Each mod can have its own AsyncContext (executor + scheduler)
 * - Provides methods to create, retrieve, and shutdown contexts globally
 *
 * <h3>Recommended usage</h3>
 * Always retrieve the context through this class right before using it,
 * instead of storing {@link AsyncContext} in long-lived fields.
 * This guarantees health checks and automatic recreation when a context
 * was previously shutdown or became unhealthy.
 *
 * <pre>{@code
 * // Recommended: request context each time
 * UtilsAsync.getContext("my-mod").runAsync(() -> doWork());
 * UtilsAsync.createContext("my-mod", "MyMod-IO", 1, 2)
 *   .supply(() -> loadData());
 * }</pre>
 *
 * <h3>Avoid</h3>
 * Do not keep a cached reference like this:
 * <pre>{@code
 * // Avoid: could become stale after shutdown/reload
 * private static final AsyncContext CTX = UtilsAsync.getContext("my-mod");
 * }</pre>
 */
public class UtilsAsync {

  private UtilsAsync() {
    // Utility class
  }

  private static final Cache<@NonNull String, AsyncContext> contexts = Caffeine.newBuilder()
    .build();

  /**
   * Returns a healthy context for the given mod id.
   * <p>
   * If a context exists and is healthy, it is reused.
   * If it exists but is unhealthy, it is shutdown and replaced.
   * If it does not exist, a new one is created.
   *
   * <h4>Example</h4>
   * <pre>{@code
   * UtilsAsync.createContext("cobbleutils-sql", "SQL-IO", 2, 4)
   *   .runAsync(() -> manager.execute("UPDATE ..."));
   * }</pre>
   *
   * @param modId       Unique context identifier.
   * @param threadName  Base name for created threads.
   * @param minThreads  Minimum threads for the main executor.
   * @param maxThreads  Maximum threads for the main executor.
   * @return A healthy {@link AsyncContext} instance.
   */
  public static AsyncContext createContext(
    String modId,
    String threadName,
    int minThreads,
    int maxThreads
  ) {
    return contexts.asMap().compute(modId, (id, existing) -> {
      if (existing != null && existing.isHealthy()) {
        return existing;
      }
      if (existing != null) {
        existing.shutdownNow();
      }
      return new AsyncContext(threadName, minThreads, maxThreads, 2500, 60, TimeUnit.SECONDS);
    });
  }

  /**
   * Returns a healthy context with explicit queue and timeout configuration.
   */
  public static AsyncContext createContext(
    String modId,
    String threadName,
    int minThreads,
    int maxThreads,
    int queueSize,
    long timeout,
    TimeUnit unit
  ) {
    return contexts.asMap().compute(modId, (id, existing) -> {
      if (existing != null && existing.isHealthy()) {
        return existing;
      }
      if (existing != null) {
        existing.shutdownNow();
      }
      return new AsyncContext(threadName, minThreads, maxThreads, queueSize, timeout, unit);
    });
  }

  // Overload simple (por compatibilidad)
  /**
   * Convenience overload with a fixed single-thread executor.
   *
   * @param modId Unique context identifier.
   * @param threadName Base name for created threads.
   * @return A healthy {@link AsyncContext} instance.
   */
  public static AsyncContext createContext(String modId, String threadName) {
    return createContext(modId, threadName, 1, 1);
  }

  /**
   * Retrieves a healthy context for a mod using default thread name and sizes.
   *
   * <h4>Example</h4>
   * <pre>{@code
   * UtilsAsync.getContext("my-mod").runAsync(() -> {
   *   // async logic here
   * });
   * }</pre>
   *
   * @param modId Unique identifier for the mod
   * @return A healthy {@link AsyncContext} instance.
   */
  public static AsyncContext getContext(String modId) {
    return createContext(modId, modId + "-async");
  }

  /**
   * Shuts down all AsyncContexts and clears the manager.
   * Should be called on server shutdown.
   */
  public static void shutdownAll() {
    contexts.asMap().values().forEach(AsyncContext::shutdownNow);
    contexts.invalidateAll();
    contexts.cleanUp();
  }
}
