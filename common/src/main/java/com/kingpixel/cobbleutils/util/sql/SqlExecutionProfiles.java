package com.kingpixel.cobbleutils.util.sql;

import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.Model.DataBaseType;

/**
 * Resolves SQL execution profiles from defaults and optional config overrides.
 */
public final class SqlExecutionProfiles {

  private SqlExecutionProfiles() {
  }

  public static SqlExecutionProfile resolve(DataBaseConfig config) {
    SqlExecutionProfile base = defaults(config.getType());
    DataBaseConfig.SqlTuning tuning = config.getSqlTuning();
    if (tuning == null || !tuning.isEnabled()) {
      return base;
    }

    return new SqlExecutionProfile(
      or(tuning.getPoolMinIdle(), base.poolMinIdle()),
      or(tuning.getPoolMaxSize(), base.poolMaxSize()),
      or(tuning.getAsyncMinThreads(), base.asyncMinThreads()),
      or(tuning.getAsyncMaxThreads(), base.asyncMaxThreads()),
      or(tuning.getAsyncQueueSize(), base.asyncQueueSize()),
      or(tuning.getOperationTimeoutMs(), base.operationTimeoutMs()),
      or(tuning.getSqliteBusyTimeoutMs(), base.sqliteBusyTimeoutMs()),
      or(tuning.getSqliteWal(), base.sqliteWal()),
      or(tuning.getSqliteSynchronousNormal(), base.sqliteSynchronousNormal())
    );
  }

  private static SqlExecutionProfile defaults(DataBaseType type) {
    return switch (type) {
      case SQLITE -> new SqlExecutionProfile(1, 1, 1, 1, 256, 30_000L, 5_000, true, true);
      case H2 -> new SqlExecutionProfile(1, 1, 1, 1, 256, 30_000L, 0, false, false);
      case MYSQL, MARIADB -> new SqlExecutionProfile(2, 8, 2, 6, 1_500, 30_000L, 0, false, false);
      default -> new SqlExecutionProfile(1, 2, 1, 2, 500, 30_000L, 0, false, false);
    };
  }

  private static int or(Integer value, int fallback) {
    return value != null ? value : fallback;
  }

  private static long or(Long value, long fallback) {
    return value != null ? value : fallback;
  }

  private static boolean or(Boolean value, boolean fallback) {
    return value != null ? value : fallback;
  }
}
