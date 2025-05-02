package com.kingpixel.cobbleutils.util;

import com.kingpixel.cobbleutils.CobbleUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Class for logging.
 */
public class UtilsLogger {
  private static final Map<String, Logger> loggers = new HashMap<>();
  private Logger logger; // Log for the console.

  /**
   * Get the logger for the mod.
   *
   * @param modId The mod id.
   *
   * @return The logger for the mod.
   */
  private Logger getLogger(String modId) {
    Logger logger = loggers.get(modId);
    if (logger == null) {
      logger = LogManager.getLogger(modId);
      loggers.put(modId, logger);
    }
    return logger;
  }

  /**
   * Error log method.
   *
   * @param message The message to log.
   */
  public void warn(String message) {
    logger.warn(message);
  }

  /**
   * Warn log method.
   *
   * @param modId   The mod id.
   * @param message The message to log.
   */
  public void warn(String modId, String message) {
    getLogger(modId).warn(message);
  }


  public void info(DayOfWeek dayOfWeek) {
    logger.info(dayOfWeek);
  }

  // Enums used for the log file.
  private enum Level {
    INFO,
    ERROR,
    WARN,
    FATAL
  }

  // Constructor that creates the logger.
  public UtilsLogger() {
    logger = LogManager.getLogger(CobbleUtils.MOD_NAME);
  }

  public UtilsLogger(String name) {
    logger = LogManager.getLogger(name);
  }

  /**
   * Info log method.
   *
   * @param message The message to log.
   */
  public void info(String message) {
    logger.info(message);
//		write(Level.INFO, message);
  }

  public void info(String modId, String message) {
    getLogger(modId).info(message);
  }

  /**
   * Error log method.
   *
   * @param message The message to log.
   */
  public void error(String message) {
    logger.error(message);
//		write(Level.ERROR, message);
  }

  public void error(String modId, String message) {
    getLogger(modId).error(message);
  }

  /**
   * Fatal log method.
   *
   * @param message The message to log.
   */
  public void fatal(String message) {
    logger.fatal(message);
//		write(Level.FATAL, message);
  }

  public void fatal(String modId, String message) {
    getLogger(modId).fatal(message);
  }

  /**
   * Write method to save the logs to file.
   *
   * @param level   The level that the log is (INFO, ERROR or FATAL).
   * @param message The message to log.
   */
  private void write(Level level, String message) {
    // TODO Can't append to file.

    String output = "[" + level + "]: " + message;

    CompletableFuture<Boolean> future = Utils.writeFileAsync(CobbleUtils.PATH, "logs.txt", output);

    System.out.println(": " + future.join());
  }
}
