package com.kingpixel.cobbleutils.util;

import com.kingpixel.cobbleutils.CobbleUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.Map;

/**
 * Class for logging with emojis 😎
 */
public class UtilsLogger {
  private static final Map<String, Logger> loggers = new HashMap<>();
  private Logger logger; // Log for the console.

  private Logger getLogger(String modId) {
    Logger logger = loggers.get(modId);
    if (logger == null) {
      logger = LogManager.getLogger(modId);
      loggers.put(modId, logger);
    }
    return logger;
  }

  // Constructor
  public UtilsLogger() {
    logger = LogManager.getLogger(CobbleUtils.MOD_NAME);
  }

  public UtilsLogger(String name) {
    logger = LogManager.getLogger(name);
  }

  // 🔵 INFO
  public void info(String message) {
    logger.info("ℹ️ " + message);
  }

  public void info(String modId, String message) {
    getLogger(modId).info("ℹ️ " + message);
  }

  public void info(DayOfWeek dayOfWeek) {
    logger.info("ℹ️ " + dayOfWeek);
  }

  // ⚠️ WARN
  public void warn(String message) {
    logger.warn("⚠️ " + message);
  }

  public void warn(String modId, String message) {
    getLogger(modId).warn("⚠️ " + message);
  }

  // ❌ ERROR
  public void error(String message) {
    logger.error("❌ " + message);
  }

  public void error(String modId, String message) {
    getLogger(modId).error("❌ " + message);
  }

  // 💀 FATAL
  public void fatal(String message) {
    logger.fatal("💀 " + message);
  }

  public void fatal(String modId, String message) {
    getLogger(modId).fatal("💀 " + message);
  }

  // Enums used for the log file.
  private enum Level {
    INFO,
    ERROR,
    WARN,
    FATAL
  }

}
