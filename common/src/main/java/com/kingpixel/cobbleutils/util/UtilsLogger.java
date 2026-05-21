package com.kingpixel.cobbleutils.util;

import com.kingpixel.cobbleutils.CobbleUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.DayOfWeek;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class for logging with emojis 😎
 */
public class UtilsLogger extends PrefixedLogger {
  private static final Map<String, PrefixedLogger> loggers = new ConcurrentHashMap<>();

  public static Logger getLogger(String modId) {
    String resolvedModId = normalizeModId(modId, CobbleUtils.MOD_ID);
    return loggers.computeIfAbsent(resolvedModId,
      key -> new PrefixedLogger(LogManager.getLogger(CobbleUtils.MOD_NAME), key));
  }

  // Constructor
  public UtilsLogger() {
    super(LogManager.getLogger(CobbleUtils.MOD_NAME), CobbleUtils.MOD_ID);
  }

  @Deprecated(forRemoval = true)
  public UtilsLogger(String name) {
    super(LogManager.getLogger(name), name);
  }

  @Deprecated(forRemoval = true)
  public void info(String modId, String message) {
    getLogger(modId).info(message);
  }

  @Deprecated(forRemoval = true)
  public void info(DayOfWeek dayOfWeek) {
    info(String.valueOf(dayOfWeek));
  }

  @Deprecated(forRemoval = true)
  public void warn(String modId, String message) {
    getLogger(modId).warn(message);
  }

  @Deprecated(forRemoval = true)
  public void error(String modId, String message) {
    getLogger(modId).error(message);
  }

  @Deprecated(forRemoval = true)
  public void fatal(String modId, String message) {
    getLogger(modId).fatal(message);
  }
}
