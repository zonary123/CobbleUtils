package com.kingpixel.cobbleutils.util;

import com.kingpixel.cobbleutils.CobbleUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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

  public UtilsLogger(String name) {
    super(LogManager.getLogger(name), name);
  }

  public void info(String modId, String message) {
    getLogger(modId).info(message);
  }

  public void info(DayOfWeek dayOfWeek) {
    info(String.valueOf(dayOfWeek));
  }

  public void warn(String modId, String message) {
    getLogger(modId).warn(message);
  }

  public void error(String modId, String message) {
    getLogger(modId).error(message);
  }


  public void fatal(String modId, String message) {
    getLogger(modId).fatal(message);
  }

  // Enums used for the log file.
  private enum Level {
    INFO,
    ERROR,
    WARN,
    FATAL
  }

  // Write method to save logs to file (async)
  private void write(Level level, String message) {
    String emoji = switch (level) {
      case INFO -> "ℹ️";
      case WARN -> "⚠️";
      case ERROR -> "❌";
      case FATAL -> "💀";
    };

    String output = emoji + " [" + level + "]: " + message;

    Path path = Path.of(CobbleUtils.PATH).resolve("logs.txt");
    CompletableFuture<Void> future = UtilsFile.writeTextAsync(path, output);

    future.join();
  }
}
