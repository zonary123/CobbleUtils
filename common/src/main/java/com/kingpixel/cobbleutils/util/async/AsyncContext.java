package com.kingpixel.cobbleutils.util.async;

import lombok.Getter;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Getter
public class AsyncContext {

  private final ThreadPoolExecutor executor;
  private final ScheduledExecutorService scheduler;

  private final AtomicBoolean running = new AtomicBoolean(true);

  private final long timeout;
  private final TimeUnit timeoutUnit;

  /* ========================================= */
  /* =============== CONSTRUCTOR ============= */
  /* ========================================= */

  public AsyncContext(
    String threadName,
    int minThreads,
    int maxThreads,
    int queueSize,
    long timeout,
    TimeUnit timeoutUnit
  ) {
    this.timeout = timeout;
    this.timeoutUnit = timeoutUnit;

    AtomicInteger counter = new AtomicInteger();

    ThreadFactory factory = r -> {
      Thread t = new Thread(r);
      t.setName(threadName + "-Worker-" + counter.incrementAndGet());
      t.setDaemon(true);
      t.setUncaughtExceptionHandler((thread, ex) -> {
        System.err.println("[AsyncContext] Uncaught exception in " + thread.getName());
        ex.printStackTrace();
      });
      return t;
    };

    this.executor = new ThreadPoolExecutor(
      minThreads,
      maxThreads,
      60L,
      TimeUnit.SECONDS,
      new LinkedBlockingQueue<>(queueSize),
      factory,
      new ThreadPoolExecutor.CallerRunsPolicy()
    );

    this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = factory.newThread(r);
      t.setName(threadName + "-Scheduler");
      return t;
    });
  }

  /* ========================================= */
  /* =============== ASYNC CALLS ============= */
  /* ========================================= */

  public <T> CompletableFuture<T> supply(Supplier<T> supplier) {
    CompletableFuture<T> future = new CompletableFuture<>();

    Runnable task = () -> {
      try {
        T result = supplier.get();
        future.complete(result);
      } catch (Throwable t) {
        future.completeExceptionally(t);
      }
    };

    submitOrFallback(task);

    return future
      .orTimeout(timeout, timeoutUnit)
      .exceptionally(ex -> {
        ex.printStackTrace();
        return null;
      });
  }

  public CompletableFuture<Void> runAsync(Runnable runnable) {
    return supply(() -> {
      runnable.run();
      return null;
    });
  }

  /* ========================================= */
  /* =============== SCHEDULER =============== */
  /* ========================================= */

  public <T> CompletableFuture<T> schedule(Supplier<T> supplier, long delay, TimeUnit unit) {
    CompletableFuture<T> future = new CompletableFuture<>();

    Runnable task = () -> {
      try {
        future.complete(supplier.get());
      } catch (Throwable t) {
        future.completeExceptionally(t);
      }
    };

    if (isSchedulerAlive()) {
      runFallback(task);
    } else {
      try {
        scheduler.schedule(task, delay, unit);
      } catch (RejectedExecutionException ex) {
        runFallback(task);
      }
    }

    return future;
  }

  public <T> CompletableFuture<T> scheduleAtFixedRate(
    Supplier<T> supplier,
    long initialDelay,
    long period,
    TimeUnit unit
  ) {
    CompletableFuture<T> firstRunFuture = new CompletableFuture<>();

    Runnable task = new Runnable() {
      boolean first = true;

      @Override
      public void run() {
        try {
          T result = supplier.get();

          if (first) {
            firstRunFuture.complete(result);
            first = false;
          }

        } catch (Throwable t) {
          if (first) {
            firstRunFuture.completeExceptionally(t);
            first = false;
          } else {
            t.printStackTrace();
          }
        }
      }
    };

    if (isSchedulerAlive()) {
      runFallback(task);
    } else {
      try {
        scheduler.scheduleAtFixedRate(task, initialDelay, period, unit);
      } catch (RejectedExecutionException ex) {
        runFallback(task);
      }
    }

    return firstRunFuture;
  }

  /* ========================================= */
  /* =============== EXECUTION =============== */
  /* ========================================= */

  private void submitOrFallback(Runnable task) {
    if (!isExecutorAlive()) {
      runFallback(task);
      return;
    }

    try {
      executor.submit(task);
    } catch (RejectedExecutionException ex) {
      runFallback(task);
    }
  }

  private boolean isExecutorAlive() {
    return running.get() && !executor.isShutdown() && !executor.isTerminated();
  }

  private boolean isSchedulerAlive() {
    return !running.get() || scheduler.isShutdown() || scheduler.isTerminated();
  }

  /**
   * Fallback seguro.
   * Si quieres puedes redirigir a un executor global.
   */
  private void runFallback(Runnable task) {
    task.run();
  }

  /* ========================================= */
  /* =============== SHUTDOWN ================ */
  /* ========================================= */

  public void shutdown() {
    running.set(false);

    executor.shutdown();
    scheduler.shutdown();

    try {
      if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }

      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }

    } catch (InterruptedException e) {
      executor.shutdownNow();
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  public void shutdownNow() {
    running.set(false);
    executor.shutdownNow();
    scheduler.shutdownNow();
  }

  /* ========================================= */
  /* =============== METRICS ================= */
  /* ========================================= */

  public int getActiveThreads() {
    return executor.getActiveCount();
  }

  public long getCompletedTasks() {
    return executor.getCompletedTaskCount();
  }

  public int getQueueSize() {
    return executor.getQueue().size();
  }
}
