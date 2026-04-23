package com.kingpixel.cobbleutils.util.async;

import com.kingpixel.cobbleutils.CobbleUtils;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CancellationException;

/**
 * Production-grade async execution context for Minecraft mods.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Configurable thread pools with queue bounds</li>
 *   <li>Automatic fallback executor when main pool is saturated</li>
 *   <li>Scheduled task execution with graceful cancellation</li>
 *   <li>Comprehensive metrics and monitoring</li>
 *   <li>Safe shutdown with timeout and resource cleanup</li>
 *   <li>Daemon threads with exception tracking</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * AsyncContext ctx = new AsyncContext("MyMod", 2, 4, 1000, 30, TimeUnit.SECONDS);
 *
 * // Async supply with result
 * CompletableFuture<String> result = ctx.supply(() -> expensiveComputation());
 *
 * // Fire-and-forget
 * ctx.runAsync(() -> saveToDB());
 *
 * // Scheduled task
 * ctx.schedule(() -> syncPlayers(), 5, TimeUnit.SECONDS);
 *
 * // On server shutdown
 * ctx.shutdown();
 * }</pre>
 */
public class AsyncContext {

  private final ThreadPoolExecutor executor;
  private final ScheduledThreadPoolExecutor scheduler;
  private final ExecutorService fallbackExecutor;
  private final Set<ScheduledFuture<?>> scheduledTasks = ConcurrentHashMap.newKeySet();

  private final AtomicBoolean running = new AtomicBoolean(true);
  private final AtomicInteger fallbackExecutions = new AtomicInteger();

  private final long defaultTimeout;
  private final TimeUnit defaultTimeoutUnit;
  private final String threadNamePrefix;

  /* ========================================= */
  /* =============== CONSTRUCTOR ============= */
  /* ========================================= */

  /**
   * Creates an AsyncContext with default queue size and timeout.
   *
   * @param threadName   prefix for thread names
   * @param minThreads   minimum pool threads
   * @param maxThreads   maximum pool threads
   */
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

  /**
   * Creates a fully configured AsyncContext.
   *
   * @param threadName       prefix for thread names
   * @param minThreads       minimum pool threads
   * @param maxThreads       maximum pool threads
   * @param queueSize        max pending tasks before fallback
   * @param timeout          default timeout for operations
   * @param timeoutUnit      unit of timeout
   */
  public AsyncContext(
    String threadName,
    int minThreads,
    int maxThreads,
    int queueSize,
    long timeout,
    TimeUnit timeoutUnit
  ) {
    if (minThreads <= 0 || maxThreads < minThreads) {
      throw new IllegalArgumentException("Invalid thread bounds: min=" + minThreads + " max=" + maxThreads);
    }

    this.threadNamePrefix = threadName;
    this.defaultTimeout = timeout;
    this.defaultTimeoutUnit = timeoutUnit;

    AtomicInteger counter = new AtomicInteger();

    ThreadFactory factory = r -> {
      Thread t = new Thread(r);
      t.setName(threadName + "-" + counter.incrementAndGet());
      t.setDaemon(true);
      t.setUncaughtExceptionHandler((thread, ex) ->
        CobbleUtils.LOGGER_RAW.error("[AsyncContext:{}] Uncaught error in {}: {}",
            threadName, thread.getName(), ex.getMessage(), ex));
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

    this.scheduler = new ScheduledThreadPoolExecutor(1, r -> {
      Thread t = factory.newThread(r);
      t.setName(threadName + "-Scheduler");
      return t;
    });
    this.scheduler.setRemoveOnCancelPolicy(true);
    this.scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    this.scheduler.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);

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

  /**
   * Executes a supplier asynchronously with default timeout.
   *
   * @param supplier the computation to execute
   * @param <T>      result type
   * @return future with result or exception
   */
  public <T> CompletableFuture<T> supply(Supplier<T> supplier) {
    return supply(supplier, defaultTimeout, defaultTimeoutUnit);
  }

  /**
   * Executes a supplier asynchronously with custom timeout.
   *
   * <p>The returned future will complete exceptionally if:
   * <ul>
   *   <li>Timeout expires</li>
   *   <li>Supplier throws an exception</li>
   *   <li>AsyncContext is shutting down</li>
   *   <li>All executors are saturated</li>
   * </ul>
   *
   * @param supplier the computation to execute
   * @param timeout  max time to wait
   * @param unit     unit of timeout
   * @param <T>      result type
   * @return future with result or exception
   */
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

  /**
   * Executes a runnable asynchronously (fire-and-forget).
   *
   * <p>Exceptions are logged but don't propagate. For exception handling,
   * use {@link #supply(Supplier)} with a wrapped runnable.
   *
   * @param runnable the task to execute
   * @return future that completes when task finishes
   */
  public CompletableFuture<Void> runAsync(Runnable runnable) {
    return supply(() -> {
      runnable.run();
      return null;
    });
  }

  /* ========================================= */
  /* =============== SCHEDULER =============== */
  /* ========================================= */

  /**
   * Schedules a one-time task to execute after a delay (returns ScheduledFuture).
   *
   * @param task   the task to execute
   * @param delay  time to wait before execution
   * @param unit   unit of delay
   * @return future representing the scheduled task (can be cancelled)
   */
  public ScheduledFuture<?> scheduleWithFuture(Runnable task, long delay, TimeUnit unit) {
    if (isSchedulerAlive()) {
      try {
        ScheduledFuture<?> future = scheduler.schedule(() -> safeRun(task), delay, unit);
        registerScheduledTask(future);
        return future;
      } catch (RejectedExecutionException ex) {
        executeOnFallback(task, null);
        CompletableFuture<Void> failed = new CompletableFuture<>();
        failed.completeExceptionally(ex);
        return new ScheduledFutureCompletableFutureAdapter(failed);
      }
    } else {
      executeOnFallback(task, null);
      CompletableFuture<Void> failed = new CompletableFuture<>();
      failed.completeExceptionally(new IllegalStateException("Scheduler is not alive"));
      return new ScheduledFutureCompletableFutureAdapter(failed);
    }
  }

  /**
   * Schedules a one-time task to execute after a delay (fire-and-forget, void return).
   *
   * <p>This method maintains binary compatibility with older code.
   * Prefer {@link #scheduleWithFuture} to get a ScheduledFuture for cancellation.
   *
   * @param task   the task to execute
   * @param delay  time to wait before execution
   * @param unit   unit of delay
   */
  public void schedule(Runnable task, long delay, TimeUnit unit) {
    scheduleWithFuture(task, delay, unit);
  }

  /**
   * Schedules a periodic task that repeats at fixed rate (returns ScheduledFuture).
   *
   * <p>If the task throws an exception, it will be logged but the periodic
   * execution continues. To stop a periodic task, cancel the returned future.
   *
   * @param task         the task to repeat
   * @param initialDelay delay before first execution
   * @param period       time between executions
   * @param unit         unit of delays
   * @return future representing the periodic task (can be cancelled to stop)
   */
  public ScheduledFuture<?> scheduleAtFixedRateWithFuture(
    Runnable task,
    long initialDelay,
    long period,
    TimeUnit unit
  ) {
    if (isSchedulerAlive()) {
      try {
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
          () -> safeRun(task),
          initialDelay,
          period,
          unit
        );
        registerScheduledTask(future);
        return future;
      } catch (RejectedExecutionException ex) {
        executeOnFallback(task, null);
        CompletableFuture<Void> failed = new CompletableFuture<>();
        failed.completeExceptionally(ex);
        return new ScheduledFutureCompletableFutureAdapter(failed);
      }
    } else {
      executeOnFallback(task, null);
      CompletableFuture<Void> failed = new CompletableFuture<>();
      failed.completeExceptionally(new IllegalStateException("Scheduler is not alive"));
      return new ScheduledFutureCompletableFutureAdapter(failed);
    }
  }

  /**
   * Schedules a periodic task that repeats at fixed rate (fire-and-forget, void return).
   *
   * <p>This method maintains binary compatibility with older code.
   * Prefer {@link #scheduleAtFixedRateWithFuture} to get a ScheduledFuture for cancellation.
   *
   * @param task         the task to repeat
   * @param initialDelay delay before first execution
   * @param period       time between executions
   * @param unit         unit of delays
   */
  public void scheduleAtFixedRate(
    Runnable task,
    long initialDelay,
    long period,
    TimeUnit unit
  ) {
    scheduleAtFixedRateWithFuture(task, initialDelay, period, unit);
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

  /**
   * Adapter to make CompletableFuture implement ScheduledFuture.
   * Used for failed scheduled tasks.
   */
  private static class ScheduledFutureCompletableFutureAdapter implements ScheduledFuture<Void> {
    private final CompletableFuture<Void> delegate;

    ScheduledFutureCompletableFutureAdapter(CompletableFuture<Void> delegate) {
      this.delegate = delegate;
    }

    @Override
    public long getDelay(TimeUnit unit) {
      return 0;
    }

    @Override
    public int compareTo(Delayed o) {
      return 0;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      return delegate.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
      return delegate.isCancelled();
    }

    @Override
    public boolean isDone() {
      return delegate.isDone();
    }

    @Override
    public Void get() throws InterruptedException, ExecutionException {
      return delegate.get();
    }

    @Override
    public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      return delegate.get(timeout, unit);
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
    if (!running.get()) {
      if (future != null) {
        future.completeExceptionally(new CancellationException("AsyncContext is shutting down"));
      }
      return;
    }
    if (future != null) {
      future.completeExceptionally(exception);
      return;
    }
    CobbleUtils.LOGGER_RAW.warn("[AsyncContext:{}] Async task rejected: all executors saturated", threadNamePrefix, exception);
  }

  private void safeRun(Runnable task) {
    if (!running.get()) {
      return;
    }
    try {
      task.run();
    } catch (Exception exception) {
      if (!running.get()) {
        return;
      }
      CobbleUtils.LOGGER_RAW.error("[AsyncContext:{}] Task execution failed", threadNamePrefix, exception);
    }
  }

  private void registerScheduledTask(ScheduledFuture<?> future) {
    scheduledTasks.add(future);
    // Periodic cleanup of completed tasks to prevent memory growth
    scheduledTasks.removeIf(ScheduledFuture::isDone);
  }

  private void cancelScheduledTasks(boolean mayInterrupt) {
    int cancelled = 0;
    for (ScheduledFuture<?> future : scheduledTasks) {
      try {
        if (future.cancel(mayInterrupt)) {
          cancelled++;
        }
      } catch (Exception ignored) {
        // ignore cancellation errors
      }
    }
    if (cancelled > 0) {
      CobbleUtils.LOGGER_RAW.debug("[AsyncContext:{}] Cancelled {} scheduled tasks", threadNamePrefix, cancelled);
    }
    scheduledTasks.clear();
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

  /**
   * Gracefully shuts down all executors with timeout.
   *
   * <p>Tries to complete pending tasks within the default timeout,
   * then forces shutdown if necessary. All scheduled tasks are cancelled.
   */
  public void shutdown() {
    running.set(false);
    cancelScheduledTasks(false);

    executor.shutdown();
    scheduler.shutdown();
    fallbackExecutor.shutdown();

    try {
      if (!executor.awaitTermination(defaultTimeout, defaultTimeoutUnit)) {
        executor.shutdownNow();
        CobbleUtils.LOGGER_RAW.warn("[AsyncContext:{}] Executor did not terminate gracefully, forced shutdown", threadNamePrefix);
      }

      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
        CobbleUtils.LOGGER_RAW.warn("[AsyncContext:{}] Scheduler did not terminate gracefully, forced shutdown", threadNamePrefix);
      }

      if (!fallbackExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
        fallbackExecutor.shutdownNow();
        CobbleUtils.LOGGER_RAW.warn("[AsyncContext:{}] Fallback executor did not terminate gracefully, forced shutdown", threadNamePrefix);
      }

      CobbleUtils.LOGGER_RAW.info("[AsyncContext:{}] Graceful shutdown complete", threadNamePrefix);

    } catch (InterruptedException e) {
      executor.shutdownNow();
      scheduler.shutdownNow();
      fallbackExecutor.shutdownNow();
      Thread.currentThread().interrupt();
      CobbleUtils.LOGGER_RAW.error("[AsyncContext:{}] Interrupted during shutdown", threadNamePrefix, e);
    }
  }

  /**
   * Forces immediate shutdown, cancelling all pending tasks.
   *
   * <p>This is aggressive and should only be used in emergency scenarios.
   */
  public void shutdownNow() {
    running.set(false);
    cancelScheduledTasks(true);
    executor.shutdownNow();
    scheduler.shutdownNow();
    fallbackExecutor.shutdownNow();
    CobbleUtils.LOGGER_RAW.warn("[AsyncContext:{}] Emergency shutdown triggered", threadNamePrefix);
  }

  /* ========================================= */
  /* =============== METRICS ================== */
  /* ========================================= */

  /**
   * Returns the number of threads currently executing tasks.
   */
  public int getActiveThreads() {
    return executor.getActiveCount();
  }

  /**
   * Returns the total number of tasks completed by the executor.
   */
  public long getCompletedTasks() {
    return executor.getCompletedTaskCount();
  }

  /**
   * Returns the number of tasks pending in the executor queue.
   */
  public int getQueueSize() {
    return executor.getQueue().size();
  }

  /**
   * Returns the load factor (active threads / max threads) [0.0, 1.0].
   */
  public double getLoadFactor() {
    return (double) executor.getActiveCount() / executor.getMaximumPoolSize();
  }

  /**
   * Returns the number of times the fallback executor was used due to saturation.
   */
  public int getFallbackExecutions() {
    return fallbackExecutions.get();
  }

  /**
   * Returns the number of scheduled tasks currently pending or running.
   */
  public int getPendingScheduledTasks() {
    return scheduledTasks.size();
  }

  /**
   * Returns comprehensive health status.
   */
  public boolean isHealthy() {
    return isExecutorAlive() && isSchedulerAlive();
  }

  /**
   * Returns a summary of executor statistics.
   */
  public String getStatsSummary() {
    return String.format(
      "[%s] Threads: %d/%d | Queue: %d | Completed: %d | Fallback: %d | Scheduled: %d | Load: %.1f%%",
      threadNamePrefix,
      getActiveThreads(),
      executor.getMaximumPoolSize(),
      getQueueSize(),
      getCompletedTasks(),
      getFallbackExecutions(),
      getPendingScheduledTasks(),
      getLoadFactor() * 100
    );
  }
}

