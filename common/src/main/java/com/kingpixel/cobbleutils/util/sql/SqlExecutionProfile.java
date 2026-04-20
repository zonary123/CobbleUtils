package com.kingpixel.cobbleutils.util.sql;

/**
 * Immutable SQL execution profile used to tune pool and async behavior.
 */
public record SqlExecutionProfile(
  int poolMinIdle,
  int poolMaxSize,
  int asyncMinThreads,
  int asyncMaxThreads,
  int asyncQueueSize,
  long operationTimeoutMs,
  int sqliteBusyTimeoutMs,
  boolean sqliteWal,
  boolean sqliteSynchronousNormal
) {
}
