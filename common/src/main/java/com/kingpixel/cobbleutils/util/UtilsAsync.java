package com.kingpixel.cobbleutils.util;

import com.kingpixel.cobbleutils.CobbleUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Deprecated(forRemoval = true)
public final class UtilsAsync {

  private static final long DEFAULT_TIMEOUT = 30;
  private static final TimeUnit DEFAULT_UNIT = TimeUnit.SECONDS;

  private UtilsAsync() {
  }

  public static CompletableFuture<Void> runAsync(Runnable task, ExecutorService executor) {
    if (executor == null || executor.isShutdown() || executor.isTerminated()) {
      try {
        task.run();
      } catch (Exception e) {
        CobbleUtils.LOGGER_RAW.error("Error executing urgent synchronous task (Executor is shutdown): ", e);
      }
      return CompletableFuture.completedFuture(null);
    }

    return CompletableFuture
      .runAsync(task, executor)
      .orTimeout(DEFAULT_TIMEOUT, DEFAULT_UNIT)
      .exceptionally(ex -> {
        CobbleUtils.LOGGER_RAW.error("Exception in async task: ", ex);
        return null;
      });
  }

  public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier, ExecutorService executor) {
    if (executor == null || executor.isShutdown() || executor.isTerminated()) {
      try {
        return CompletableFuture.completedFuture(supplier.get());
      } catch (Exception e) {
        CobbleUtils.LOGGER_RAW.error("Error executing urgent synchronous supplier (Executor is shutdown): ", e);
        return CompletableFuture.completedFuture(null);
      }
    }

    return CompletableFuture
      .supplyAsync(supplier, executor)
      .orTimeout(DEFAULT_TIMEOUT, DEFAULT_UNIT)
      .exceptionally(ex -> {
        CobbleUtils.LOGGER_RAW.error("Exception in async supplier: ", ex);
        return null;
      });
  }
}