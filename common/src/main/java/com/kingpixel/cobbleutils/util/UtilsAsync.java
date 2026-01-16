package com.kingpixel.cobbleutils.util;

import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Async utility class
 *
 * @author Carlos Varas Alonso
 */
public final class UtilsAsync {

  private static final long DEFAULT_TIMEOUT = 30;
  private static final TimeUnit DEFAULT_UNIT = TimeUnit.SECONDS;

  private UtilsAsync() {
    // Prevent instantiation
  }

  private static Executor resolveExecutor(ExecutorService executor) {
    return (executor == null || executor.isShutdown() || executor.isTerminated())
      ? ForkJoinPool.commonPool()
      : executor;
  }

  /**
   * Run async task without return value
   */
  public static CompletableFuture<Void> runAsync(Runnable task, ExecutorService executor) {
    return CompletableFuture
      .runAsync(task, resolveExecutor(executor))
      .orTimeout(DEFAULT_TIMEOUT, DEFAULT_UNIT)
      .exceptionally(ex -> {
        ex.printStackTrace();
        return null;
      });
  }

  /**
   * Run async task with return value
   */
  public static <T> CompletableFuture<T> supplyAsync(
    Supplier<T> supplier,
    ExecutorService executor
  ) {
    return CompletableFuture
      .supplyAsync(supplier, resolveExecutor(executor))
      .orTimeout(DEFAULT_TIMEOUT, DEFAULT_UNIT)
      .exceptionally(ex -> {
        ex.printStackTrace();
        return null;
      });
  }
}
