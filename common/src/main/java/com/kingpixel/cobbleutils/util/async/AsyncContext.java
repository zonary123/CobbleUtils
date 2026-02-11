package com.kingpixel.cobbleutils.util.async;

import lombok.Data;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * AsyncContext provides a per-mod asynchronous execution context with:
 * - A dedicated single-thread executor for async tasks
 * - A dedicated single-thread scheduler for delayed tasks
 * - Methods returning CompletableFuture for easy chaining and result handling
 * - Automatic fallback to the main thread if the executor or scheduler is shut down
 * <p>
 * Each mod can have its own AsyncContext to isolate tasks.
 * Tasks can return any type (String, Integer, Boolean, custom objects, etc.)
 * <p>
 * Example usage:
 * <pre>
 * AsyncContext economy = new AsyncContext("EconomyThread");
 *
 * // Run async task with result
 * CompletableFuture<String> future = economy.supply(() -> "Balance saved");
 * future.thenAccept(result -> System.out.println(result))
 *       .exceptionally(ex -> { ex.printStackTrace(); return null; });
 *
 * // Run async Runnable
 * economy.runAsync(() -> System.out.println("Saving data"))
 *       .thenRun(() -> System.out.println("Task completed"))
 *       .exceptionally(ex -> { ex.printStackTrace(); return null; });
 *
 * // Schedule a task after a delay
 * economy.schedule(() -> "Scheduled task executed", 5, TimeUnit.SECONDS)
 *       .thenAccept(msg -> System.out.println(msg));
 * </pre>
 */
@Data
public class AsyncContext {
  private final ExecutorService executor;
  private final ScheduledExecutorService scheduler;
  private final AtomicBoolean running = new AtomicBoolean(true);

  /**
   * Creates a new AsyncContext with its own executor and scheduler.
   *
   * @param threadName Base name for the threads. Threads will be named:
   *                   {threadName}-Worker and {threadName}-Scheduler
   */
  public AsyncContext(String threadName, int minThreads, int maxThreads) {
    AtomicInteger counter = new AtomicInteger();

    ThreadFactory factory = r -> {
      Thread t = new Thread(r);
      t.setName(threadName + "-" + counter.incrementAndGet());
      t.setDaemon(true);
      return t;
    };

    this.executor = new ThreadPoolExecutor(
      minThreads,
      maxThreads,
      60L,
      TimeUnit.SECONDS,
      new LinkedBlockingQueue<>(),
      factory
    );

    this.scheduler = Executors.newSingleThreadScheduledExecutor(factory);
  }

  /**
   * Executes a Supplier asynchronously and returns a CompletableFuture with the result.
   * If the executor is shutdown or terminated, the task is run on the main thread.
   *
   * @param supplier Supplier producing a value of type T
   * @param <T>      Return type
   * @return CompletableFuture<T> for result handling and chaining
   */
  public <T> CompletableFuture<T> supply(Supplier<T> supplier) {
    CompletableFuture<T> future = new CompletableFuture<>();
    Runnable task = () -> {
      try {
        T result = supplier.get();
        future.complete(result);
      } catch (Exception e) {
        future.completeExceptionally(e);
      }
    };

    submitOrFallback(task);
    return future
      .orTimeout(30, TimeUnit.SECONDS)
      .exceptionally(e -> {
        e.printStackTrace();
        return null;
      });
  }

  /**
   * Executes a Runnable asynchronously and returns a CompletableFuture<Void>
   * for chaining or exception handling.
   *
   * @param supplier Supplier producing a value of type T
   * @return CompletableFuture<Void>
   */
  public <T> CompletableFuture<T> runAsync(Supplier<T> supplier) {
    return supply(supplier);
  }


  /**
   * Schedules a Supplier to run after a delay, returning a CompletableFuture.
   * If the scheduler is shutdown or terminated, the task is executed immediately on the main thread.
   *
   * @param supplier Supplier producing a value of type T
   * @param delay    Delay before execution
   * @param unit     TimeUnit of the delay
   * @param <T>      Return type
   * @return CompletableFuture<T> for result handling
   */
  public <T> CompletableFuture<T> schedule(Supplier<T> supplier, long delay, TimeUnit unit) {
    CompletableFuture<T> future = new CompletableFuture<>();
    Runnable task = () -> {
      try {
        T result = supplier.get();
        future.complete(result);
      } catch (Exception e) {
        future.completeExceptionally(e);
      }
    };

    if (!running.get() || scheduler.isShutdown() || scheduler.isTerminated()) {
      runOnMainThread(task);
    } else {
      try {
        scheduler.schedule(task, delay, unit);
      } catch (RejectedExecutionException e) {
        runOnMainThread(task);
      }
    }

    return future;
  }

  /**
   * Schedules a recurring task at a fixed rate.
   * - Executes the Supplier every 'period' units, after an initial delay of 'initialDelay'.
   * - If the scheduler is shut down, falls back to main thread for the first run.
   * - Returns a CompletableFuture that completes exceptionally if the task throws.
   *
   * @param supplier     Supplier producing a value of type T
   * @param initialDelay Initial delay before first execution
   * @param period       Period between executions
   * @param unit         TimeUnit of delay and period
   * @param <T>          Return type
   * @return CompletableFuture<T> representing the **first execution** result
   */
  public <T> CompletableFuture<T> scheduleAtFixedRate(Supplier<T> supplier, long initialDelay, long period, TimeUnit unit) {
    CompletableFuture<T> firstRunFuture = new CompletableFuture<>();

    Runnable task = new Runnable() {
      boolean firstRun = true;

      @Override
      public void run() {
        try {
          T result = supplier.get();

          // Complete the future on the first execution
          if (firstRun) {
            firstRunFuture.complete(result);
            firstRun = false;
          }

        } catch (Exception e) {
          if (firstRun) {
            firstRunFuture.completeExceptionally(e);
            firstRun = false;
          } else {
            e.printStackTrace(); // log subsequent exceptions
          }
        }
      }
    };

    if (!running.get() || scheduler.isShutdown() || scheduler.isTerminated()) {
      runOnMainThread(task);
    } else {
      try {
        scheduler.scheduleAtFixedRate(task, initialDelay, period, unit);
      } catch (RejectedExecutionException e) {
        runOnMainThread(task);
      }
    }

    return firstRunFuture;
  }

  /**
   * Safely shuts down the executor and scheduler.
   * - Running tasks may complete.
   * - Pending scheduled tasks may be canceled.
   * - New tasks submitted after shutdown are executed on the main thread as fallback.
   */
  public void shutdown() {
    running.set(false);
    executor.shutdown();
    scheduler.shutdown();
    try {
      if (!executor.awaitTermination(5, TimeUnit.SECONDS)) executor.shutdownNow();
      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) scheduler.shutdownNow();
    } catch (InterruptedException e) {
      e.printStackTrace();
      executor.shutdownNow();
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Submits a Runnable to the executor or falls back to the main thread if the executor is unavailable.
   *
   * @param task Runnable task
   */
  private void submitOrFallback(Runnable task) {
    if (!running.get() || executor.isShutdown() || executor.isTerminated()) {
      runOnMainThread(task);
    } else {
      try {
        executor.submit(task);
      } catch (RejectedExecutionException e) {
        runOnMainThread(task);
      }
    }
  }

  /**
   * Executes a task on the main thread.
   * Replace with your server's main thread executor if needed.
   *
   * @param task Runnable task
   */
  private void runOnMainThread(Runnable task) {
    // TODO: Replace with your server's main thread execution method
    task.run();
  }
}
