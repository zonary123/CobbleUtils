package com.kingpixel.cobbleutils.util.async;

import com.kingpixel.cobbleutils.CobbleUtils;
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
  private final ExecutorService fallbackExecutor;

  private final AtomicBoolean running = new AtomicBoolean(true);
  private final AtomicInteger fallbackExecutions = new AtomicInteger();

  private final long defaultTimeout;
  private final TimeUnit defaultTimeoutUnit;

  /* ========================================= */
  /* =============== CONSTRUCTOR ============= */
  /* ========================================= */
  public AsyncContext(String threadName, int minThreads, int maxThreads) {
    this(
      threadName,
      minThreads,
      maxThreads,
      1000,
      30,
      TimeUnit.SECONDS
    );
  }

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
      t.setUncaughtExceptionHandler((thread, ex) ->
        CobbleUtils.LOGGER_RAW.error("[AsyncContext] Uncaught error in {}", thread.getName(), ex));
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

    int fallbackMaxThreads = Math.max(2, maxThreads);
    int fallbackQueueSize = Math.max(128, queueSize / 2);
    this.fallbackExecutor = new ThreadPoolExecutor(
      1,
      fallbackMaxThreads,
      60L,
      TimeUnit.SECONDS,
      new LinkedBlockingQueue<>(fallbackQueueSize),
      factory,
      new ThreadPoolExecutor.AbortPolicy()
    );
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
      } catch (Exception exception) {
        future.completeExceptionally(exception);
      }
    };

    submit(task, future);

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
      executeOnFallback(task, null);
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
      executeOnFallback(task, null);
    }
  }

  /* ========================================= */
  /* =============== INTERNAL ================= */
  /* ========================================= */

  private void submit(Runnable task) {
    submit(task, null);
  }

  private void submit(Runnable task, CompletableFuture<?> future) {
    if (!isExecutorAlive()) {
      executeOnFallback(task, future);
      return;
    }

    try {
      executor.submit(task);
    } catch (RejectedExecutionException ex) {
      executeOnFallback(task, future);
    }
  }

  private void executeOnFallback(Runnable task, CompletableFuture<?> future) {
    fallbackExecutions.incrementAndGet();
    try {
      fallbackExecutor.execute(task);
    } catch (RejectedExecutionException rejectedExecutionException) {
      handleRejectedSubmission(future, rejectedExecutionException);
    }
  }

  private void handleRejectedSubmission(CompletableFuture<?> future, RejectedExecutionException exception) {
    if (future != null) {
      future.completeExceptionally(exception);
      return;
    }
    CobbleUtils.LOGGER_RAW.warn("[AsyncContext] Async task rejected", exception);
  }

  private void safeRun(Runnable task) {
    try {
      task.run();
    } catch (Exception exception) {
      CobbleUtils.LOGGER_RAW.error("[AsyncContext] Async task failed", exception);
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
    fallbackExecutor.shutdown();

    try {
      if (!executor.awaitTermination(defaultTimeout, defaultTimeoutUnit)) {
        executor.shutdownNow();
      }

      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }

      if (!fallbackExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
        fallbackExecutor.shutdownNow();
      }

    } catch (InterruptedException e) {
      executor.shutdownNow();
      scheduler.shutdownNow();
      fallbackExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  public void shutdownNow() {
    running.set(false);
    executor.shutdownNow();
    scheduler.shutdownNow();
    fallbackExecutor.shutdownNow();
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