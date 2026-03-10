package com.kingpixel.cobbleutils.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Async utility class
 * <p>
 * Ejecuta tareas de forma segura:
 * - Usa el executor si está activo
 * - Si el executor está muerto o shutdown → ejecuta en el hilo principal
 */
@Deprecated(forRemoval = true)
public final class UtilsAsync {

  private static final long DEFAULT_TIMEOUT = 30;
  private static final TimeUnit DEFAULT_UNIT = TimeUnit.SECONDS;

  private UtilsAsync() {
    // Prevent instantiation
  }

  /**
   * Ejecuta una tarea sin valor de retorno
   */
  public static CompletableFuture<Void> runAsync(Runnable task, ExecutorService executor) {
    if (executor == null || executor.isShutdown() || executor.isTerminated()) {
      // Ejecutar en hilo principal
      try {
        task.run();
      } catch (Exception e) {
        e.printStackTrace();
      }
      return CompletableFuture.completedFuture(null);
    }

    return CompletableFuture
      .runAsync(task, executor)
      .orTimeout(DEFAULT_TIMEOUT, DEFAULT_UNIT)
      .exceptionally(ex -> {
        ex.printStackTrace();
        return null;
      });
  }

  /**
   * Ejecuta una tarea con valor de retorno
   */
  public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier, ExecutorService executor) {
    if (executor == null || executor.isShutdown() || executor.isTerminated()) {
      // Ejecutar en hilo principal
      try {
        return CompletableFuture.completedFuture(supplier.get());
      } catch (Exception e) {
        e.printStackTrace();
        return CompletableFuture.completedFuture(null);
      }
    }

    return CompletableFuture
      .supplyAsync(supplier, executor)
      .orTimeout(DEFAULT_TIMEOUT, DEFAULT_UNIT)
      .exceptionally(ex -> {
        ex.printStackTrace();
        return null;
      });
  }
}
