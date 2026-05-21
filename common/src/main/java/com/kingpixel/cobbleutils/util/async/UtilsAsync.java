package com.kingpixel.cobbleutils.util.async;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.NonNull;

import java.util.concurrent.TimeUnit;

/**
 * Centralized manager for per-mod AsyncContexts.
 *
 * <p>Each mod can register and maintain its own isolated AsyncContext (containing its own executor
 * pool and task scheduler). This utility provides global entry points to safely instantiate,
 * fetch, and tear down contexts across lifecycle stages.</p>
 */
public class UtilsAsync {

  private UtilsAsync() {
    // Utility class instantiation guard
  }

  /**
   * Internal thread-safe cache mapping unique Mod IDs to their respective active execution contexts.
   */
  private static final Cache<@NonNull String, AsyncContext> contexts = Caffeine.newBuilder()
    .build();

  /**
   * Returns a guaranteed healthy context for the given mod identifier.
   *
   * @param modId      Unique context tracking identifier.
   * @param threadName The foundational naming prefix mapped onto created threads.
   * @param minThreads Minimum core worker threads allocated to the primary pool.
   * @param maxThreads Maximum boundary limit of hilos allocated under heavy strain.
   *
   * @return A healthy, verified {@link AsyncContext} ready to process work.
   */
  public static AsyncContext createContext(
    String modId,
    String threadName,
    int minThreads,
    int maxThreads
  ) {
    // Uses mapping compute atomically to safely resolve and eliminate multi-threaded race conditions.
    return contexts.asMap().compute(modId, (id, existing) -> {
      if (existing != null && existing.isHealthy()) {
        return existing;
      }
      if (existing != null) {
        // Force-terminate dead or unhealthy contexts before reclaiming their mapping slot
        existing.shutdownNow();
      }
      // Allocation default optimized for Minecraft mod dependencies (2500 queue buffer, 60s timeout)
      return new AsyncContext(threadName, minThreads, maxThreads, 2500, 60, TimeUnit.SECONDS);
    });
  }

  /**
   * Convenience overload provisioning a healthy single-threaded fixed context.
   *
   * @param modId      Unique context tracking identifier.
   * @param threadName The foundational naming prefix mapped onto created threads.
   *
   * @return A healthy, single-threaded {@link AsyncContext} instance.
   */
  public static AsyncContext createContext(String modId, String threadName) {
    return createContext(modId, threadName, 1, 1);
  }

  /**
   * Retrieves a healthy context for a mod using default naming schemes and standard core thread allocations.
   *
   * @param modId Unique tracking identifier for the mod.
   *
   * @return A verified healthy {@link AsyncContext} instance.
   */
  public static AsyncContext getContext(String modId) {
    // Directly routes to base creation logic; safely handles fallback parameters out-of-box.
    return createContext(modId, modId + "-async");
  }

  /**
   * Triggers an orderly, graceful shutdown sequence across all registered AsyncContexts.
   *
   * <p>Highly recommended to be invoked during the standard Minecraft server stopping phase.
   * This guarantees that ongoing tasks, data flushes, and database writes conclude safely
   * without encountering corrupted file saving anomalies.</p>
   */
  public static void shutdownAll() {
    contexts.asMap().values().forEach(AsyncContext::shutdown);
    contexts.invalidateAll();
    contexts.cleanUp();
  }

  /**
   * Triggers an immediate, aggressive emergency shutdown sequence across all registered AsyncContexts.
   */
  public static void shutdownAllNow() {
    contexts.asMap().values().forEach(AsyncContext::shutdownNow);
    contexts.invalidateAll();
    contexts.cleanUp();
  }
}