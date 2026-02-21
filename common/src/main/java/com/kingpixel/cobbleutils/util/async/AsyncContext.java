package com.kingpixel.cobbleutils.util.async;

import lombok.Getter;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Getter
public class AsyncContext {

  /* ========================================= */
  /* =============== FIELDS ================== */
  /* ========================================= */

  private final ThreadPoolExecutor executor;
  private final ScheduledExecutorService scheduler;
  private final Executor fallbackExecutor;

  private final AtomicBoolean running = new AtomicBoolean(true);

  private final long defaultTimeout;
  private final TimeUnit defaultTimeoutUnit;

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

    this.defaultTimeout = timeout;
    this.defaultTimeoutUnit = timeoutUnit;

    AtomicInteger counter = new AtomicInteger();

    ThreadFactory factory = r -> {
      Thread t = new Thread(r);
      t.setName(threadName + "-" + counter.incrementAndGet());
      t.setDaemon(true);
      t.setUncaughtExceptionHandler((thread, ex) -> {
        System.err.println("[AsyncContext] Error en " + thread.getName());
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
      new ThreadPoolExecutor.AbortPolicy()
    );

    this.executor.prestartAllCoreThreads();

    this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = factory.newThread(r);
      t.setName(threadName + "-Scheduler");
      return t;
    });

    this.fallbackExecutor = Executors.newCachedThreadPool(factory);
  }

  /* ========================================= */
  /* =============== ASYNC =================== */
  /* ========================================= */

  public <T> CompletableFuture<T> supply(Supplier<T> supplier) {
    return supply(supplier, defaultTimeout, defaultTimeoutUnit);
  }

  public <T> CompletableFuture<T> supply(
    Supplier<T> supplier,
    long timeout,
    TimeUnit unit
  ) {

    CompletableFuture<T> future = new CompletableFuture<>();

    Runnable task = () -> {
      try {
        future.complete(supplier.get());
      } catch (Throwable t) {
        future.completeExceptionally(t);
      }
    };

    submit(task);

    return future.orTimeout(timeout, unit);
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

  public void schedule(Runnable task, long delay, TimeUnit unit) {
    if (isSchedulerAlive()) {
      scheduler.schedule(() -> safeRun(task), delay, unit);
    } else {
      fallbackExecutor.execute(task);
    }
  }

  public void scheduleAtFixedRate(
    Runnable task,
    long initialDelay,
    long period,
    TimeUnit unit
  ) {
    if (isSchedulerAlive()) {
      scheduler.scheduleAtFixedRate(
        () -> safeRun(task),
        initialDelay,
        period,
        unit
      );
    } else {
      fallbackExecutor.execute(task);
    }
  }

  /* ========================================= */
  /* =============== INTERNAL ================= */
  /* ========================================= */

  private void submit(Runnable task) {
    if (!isExecutorAlive()) {
      fallbackExecutor.execute(task);
      return;
    }

    try {
      executor.submit(task);
    } catch (RejectedExecutionException ex) {
      fallbackExecutor.execute(task);
    }
  }

  private void safeRun(Runnable task) {
    try {
      task.run();
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  private boolean isExecutorAlive() {
    return running.get()
      && !executor.isShutdown()
      && !executor.isTerminated();
  }

  private boolean isSchedulerAlive() {
    return running.get()
      && !scheduler.isShutdown()
      && !scheduler.isTerminated();
  }

  /* ========================================= */
  /* =============== SHUTDOWN ================= */
  /* ========================================= */

  public void shutdown() {
    running.set(false);

    executor.shutdown();
    scheduler.shutdown();

    try {
      if (!executor.awaitTermination(defaultTimeout, defaultTimeoutUnit)) {
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
  /* =============== METRICS ================== */
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

  public double getLoadFactor() {
    return (double) executor.getActiveCount() / executor.getMaximumPoolSize();
  }

  public boolean isHealthy() {
    return isExecutorAlive() && isSchedulerAlive();
  }
}