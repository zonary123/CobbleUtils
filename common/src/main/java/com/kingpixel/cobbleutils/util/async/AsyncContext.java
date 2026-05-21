package com.kingpixel.cobbleutils.util.async;

import com.kingpixel.cobbleutils.CobbleUtils;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Production-grade asynchronous execution context custom-tailored for Minecraft mods.
 *
 * <p>This class safely manages background worker threads, preventing heavy computations
 * from clogging the server's Main Tick Loop. It features an automated fallback mechanism
 * to absorb unexpected traffic bursts, immediate self-cleaning scheduled tasks to prevent
 * memory leaks, and a multi-threaded scheduler shielded from pool exhaustion.</p>
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
   * Creates an AsyncContext initialized with a default queue capacity of 1000 tasks and a 30-second timeout.
   *
   * @param threadName The naming prefix assigned to worker threads for profile debugging (e.g., "MyMod").
   * @param minThreads The minimum core size of active threads maintained in the main worker pool.
   * @param maxThreads The maximum pool ceiling allowed for concurrent workers under high load conditions.
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
   * Creates a fully optimized and customized AsyncContext instance.
   *
   * @param threadName  The naming prefix assigned to worker threads for profile debugging.
   * @param minThreads  The minimum core size of active threads maintained in the main worker pool.
   * @param maxThreads  The maximum pool ceiling allowed for concurrent workers under high load conditions.
   * @param queueSize   Max pending tasks tolerated by the queue before routing to fallback execution.
   * @param timeout     Default time duration threshold applied to guard async transactions.
   * @param timeoutUnit The dimensional unit of time mapping to the timeout duration parameter.
   *
   * @throws IllegalArgumentException If core bounds are negative, or maximum pool sizes drop below core values.
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

    // Thread Factory: Configures clear names and marks threads as daemon so they do not block JVM shutdown.
    ThreadFactory factory = r -> {
      Thread t = new Thread(r);
      t.setName(threadName + "-" + counter.incrementAndGet());
      t.setDaemon(true);
      t.setUncaughtExceptionHandler((thread, ex) ->
        CobbleUtils.LOGGER_RAW.error("[AsyncContext:{}] Uncaught error in {}: {}",
          threadName, thread.getName(), ex.getMessage(), ex));
      return t;
    };

    // Primary Asynchronous Executor Pool
    this.executor = new ThreadPoolExecutor(
      minThreads,
      maxThreads,
      60L,
      TimeUnit.SECONDS,
      new LinkedBlockingQueue<>(queueSize),
      factory,
      new ThreadPoolExecutor.AbortPolicy()
    );

    // Warm up core threads to prevent performance drops (lag spikes) during first tasks
    this.executor.prestartAllCoreThreads();

    // Optimized Scheduler: Uses multiple threads to prevent slow tasks from delaying other scheduled events
    int schedulerThreads = Math.max(2, Runtime.getRuntime().availableProcessors() / 4);
    this.scheduler = new ScheduledThreadPoolExecutor(schedulerThreads, r -> {
      Thread t = factory.newThread(r);
      t.setName(threadName + "-Scheduler");
      return t;
    });
    this.scheduler.setRemoveOnCancelPolicy(true);
    this.scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    this.scheduler.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);

    // Fallback executor pool for emergencies and tasks overflow
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
   * Executes a computing supplier asynchronously, bound to the contextual default timeout limit.
   *
   * @param supplier The operational calculation logic requiring out-of-core evaluation.
   * @param <T>      The target evaluation return type.
   *
   * @return A CompletableFuture object slated to mirror the resulting computed output value.
   */
  public <T> CompletableFuture<T> supply(Supplier<T> supplier) {
    return supply(supplier, defaultTimeout, defaultTimeoutUnit);
  }

  /**
   * Executes a computing supplier asynchronously, bound to a customized operation timeout limit.
   */
  public <T> CompletableFuture<T> supply(
    Supplier<T> supplier,
    long timeout,
    TimeUnit unit
  ) {
    CompletableFuture<T> future = new CompletableFuture<>();

    Runnable task = () -> {
      try {
        if (!future.isDone()) {
          future.complete(supplier.get());
        }
      } catch (Exception exception) {
        future.completeExceptionally(exception);
      }
    };

    submit(task, future);
    return future.orTimeout(timeout, unit);
  }

  /**
   * Submits a fire-and-forget task scheduled to run natively on a background worker thread.
   *
   * @param runnable The targeted executable instructions targeted for background evaluation.
   *
   * @return A CompletableFuture tracking abstract completion (Void) of the background routine.
   */
  public CompletableFuture<Void> runAsync(Runnable runnable) {
    CompletableFuture<Void> future = new CompletableFuture<>();

    Runnable task = () -> {
      try {
        runnable.run();
        future.complete(null);
      } catch (Exception exception) {
        // Enforces transparency, preventing critical database/file saving failures from masking out.
        CobbleUtils.LOGGER_RAW.error("[AsyncContext:{}] Error caught in async fire-and-forget background task", threadNamePrefix, exception);
        future.completeExceptionally(exception);
      }
    };

    submit(task, future);
    return future.orTimeout(defaultTimeout, defaultTimeoutUnit);
  }

  /* ========================================= */
  /* =============== SCHEDULER =============== */
  /* ========================================= */

  /**
   * Schedules a one-time execution sequence triggered precisely after a specific delay expires.
   */
  public ScheduledFuture<?> scheduleWithFuture(Runnable task, long delay, TimeUnit unit) {
    if (!isSchedulerAlive()) {
      executeOnFallback(task, null);
      CompletableFuture<Void> failed = new CompletableFuture<>();
      failed.completeExceptionally(new IllegalStateException("Scheduler infrastructure is offline"));
      return new ScheduledFutureCompletableFutureAdapter(failed);
    }

    try {
      // Internal reference holder array utilized to let the inner routine slice itself out upon teardown.
      final ScheduledFuture<?>[] futureHolder = new ScheduledFuture<?>[1];

      Runnable wrappedTask = () -> {
        try {
          safeRun(task);
        } finally {
          // Automatic scoped removal: Garbage collector reclaims metadata objects immediately.
          if (futureHolder[0] != null) {
            scheduledTasks.remove(futureHolder[0]);
          }
        }
      };

      ScheduledFuture<?> future = scheduler.schedule(wrappedTask, delay, unit);
      futureHolder[0] = future;
      scheduledTasks.add(future);
      return future;
    } catch (RejectedExecutionException ex) {
      executeOnFallback(task, null);
      CompletableFuture<Void> failed = new CompletableFuture<>();
      failed.completeExceptionally(ex);
      return new ScheduledFutureCompletableFutureAdapter(failed);
    }
  }

  /**
   * Schedules a one-time execution sequence triggered after a delay (Maintains backwards binary compatibility).
   */
  public void schedule(Runnable task, long delay, TimeUnit unit) {
    scheduleWithFuture(task, delay, unit);
  }

  /**
   * Establishes a repeating periodic execution sequence anchored to a strict fixed rate interval.
   */
  public ScheduledFuture<?> scheduleAtFixedRateWithFuture(
    Runnable task,
    long initialDelay,
    long period,
    TimeUnit unit
  ) {
    if (!isSchedulerAlive()) {
      executeOnFallback(task, null);
      CompletableFuture<Void> failed = new CompletableFuture<>();
      failed.completeExceptionally(new IllegalStateException("Scheduler infrastructure is offline"));
      return new ScheduledFutureCompletableFutureAdapter(failed);
    }

    try {
      // Recurrent execution tasks sit inside the collection until canceled externally or during context teardown.
      ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
        () -> safeRun(task),
        initialDelay,
        period,
        unit
      );
      scheduledTasks.add(future);
      return future;
    } catch (RejectedExecutionException ex) {
      executeOnFallback(task, null);
      CompletableFuture<Void> failed = new CompletableFuture<>();
      failed.completeExceptionally(ex);
      return new ScheduledFutureCompletableFutureAdapter(failed);
    }
  }

  /**
   * Establishes a repeating periodic execution sequence (Maintains backwards binary compatibility).
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
      // CRITICAL ARCHITECTURAL OPTIMIZATION: .execute() is chosen over .submit() intentionally to prevent
      // ThreadPoolExecutor from wrapping the task into a nested FutureTask, which breaks CompletableFuture try-catch bubbles.
      executor.execute(task);
    } catch (RejectedExecutionException ex) {
      executeOnFallback(task, future);
    }
  }

  /**
   * Internal proxy adapter transforming vanilla CompletableFuture states to fit ScheduledFuture signatures.
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
        future.completeExceptionally(new CancellationException("AsyncContext has already initiated a shutdown cycle"));
      }
      return;
    }
    if (future != null) {
      future.completeExceptionally(exception);
      return;
    }
    CobbleUtils.LOGGER_RAW.warn("[AsyncContext:{}] Task rejected: All fallback core execution layers are fully saturated under stress", threadNamePrefix, exception);
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
      CobbleUtils.LOGGER_RAW.error("[AsyncContext:{}] Critical failure encountered inside internal task processing", threadNamePrefix, exception);
    }
  }

  private boolean isExecutorAlive() {
    return running.get() && !executor.isShutdown() && !executor.isTerminated();
  }

  private boolean isSchedulerAlive() {
    return running.get() && !scheduler.isShutdown() && !scheduler.isTerminated();
  }

  /* ========================================= */
  /* =============== SHUTDOWN ================= */
  /* ========================================= */

  /**
   * Orchestrates an orderly, phased Graceful Shutdown sequence across all threaded sub-pools.
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
        CobbleUtils.LOGGER_RAW.warn("[AsyncContext:{}] Primary executor pool failed to drain timely. Enforcing forced teardown.", threadNamePrefix);
      }

      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
        CobbleUtils.LOGGER_RAW.warn("[AsyncContext:{}] Core scheduler pool failed to drain timely. Enforcing forced teardown.", threadNamePrefix);
      }

      if (!fallbackExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
        fallbackExecutor.shutdownNow();
        CobbleUtils.LOGGER_RAW.warn("[AsyncContext:{}] Secondary fallback execution pool failed to drain timely. Enforcing forced teardown.", threadNamePrefix);
      }

      CobbleUtils.LOGGER_RAW.info("[AsyncContext:{}] Graceful execution pool teardown completed successfully.", threadNamePrefix);

    } catch (InterruptedException e) {
      executor.shutdownNow();
      scheduler.shutdownNow();
      fallbackExecutor.shutdownNow();
      Thread.currentThread().interrupt();
      CobbleUtils.LOGGER_RAW.error("[AsyncContext:{}] Teardown cycle was violently interrupted during termination waits", threadNamePrefix, e);
    }
  }

  /**
   * Triggers an immediate, aggressive shutdown sweep across the entire execution environment, forcing thread interrupts.
   */
  public void shutdownNow() {
    running.set(false);
    cancelScheduledTasks(true);
    executor.shutdownNow();
    scheduler.shutdownNow();
    fallbackExecutor.shutdownNow();
    CobbleUtils.LOGGER_RAW.warn("[AsyncContext:{}] Emergency hard termination sequence invoked.", threadNamePrefix);
  }

  private void cancelScheduledTasks(boolean mayInterrupt) {
    int cancelled = 0;
    for (ScheduledFuture<?> future : scheduledTasks) {
      try {
        if (future.cancel(mayInterrupt)) {
          cancelled++;
        }
      } catch (Exception ignored) {
      }
    }
    if (cancelled > 0) {
      CobbleUtils.LOGGER_RAW.debug("[AsyncContext:{}] Revoked a total of {} active scheduled tasks during core pool teardown", threadNamePrefix, cancelled);
    }
    scheduledTasks.clear();
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

  public int getFallbackExecutions() {
    return fallbackExecutions.get();
  }

  public int getPendingScheduledTasks() {
    scheduledTasks.removeIf(ScheduledFuture::isDone);
    return scheduledTasks.size();
  }

  public boolean isHealthy() {
    return isExecutorAlive() && isSchedulerAlive();
  }

  public String getStatsSummary() {
    return String.format(
      "[%s] Workers: %d/%d | Queue: %d | Resolved: %d | Fallbacks: %d | Scheduled: %d | Load: %.1f%%",
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