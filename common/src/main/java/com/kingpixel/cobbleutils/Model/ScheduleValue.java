package com.kingpixel.cobbleutils.Model;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.google.gson.*;
import lombok.Data;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;

/**
 * Unified schedule value that supports relative duration or cron expression.
 *
 * <h3>Important: delay vs epoch</h3>
 * <ul>
 *   <li>{@link #toDelayMillis()} returns a <b>relative delay</b> (milliseconds from now).</li>
 *   <li>It is <b>not</b> an absolute timestamp and should not be compared directly
 *   against {@link System#currentTimeMillis()}.</li>
 *   <li>Use {@link #toNextEpochMillis()} if you need an absolute time to compare/store.</li>
 * </ul>
 *
 * <p>JSON examples:</p>
 * <pre>
 * {"type":"duration","value":"30m"}
 * {"type":"cron","expression":"0 0 1 * *","zoneId":"UTC"}
 * </pre>
 *
 * <p>Compatibility shortcuts accepted while reading:</p>
 * <ul>
 *   <li>Number: {@code 30} => duration (minutes)</li>
 *   <li>String duration: {@code "30m"}</li>
 *   <li>String cron: {@code "0 0 1 * *"}</li>
 * </ul>
 */
@Data
public class ScheduleValue implements JsonSerializer<ScheduleValue>, JsonDeserializer<ScheduleValue> {

  public enum Mode {
    DURATION,
    CRON
  }

  public static final ScheduleValue INSTANCE = ScheduleValue.ofDuration(DurationValue.parse("60m"));
  private static final CronDefinition CRON_DEFINITION = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX);
  private static final CronParser CRON_PARSER = new CronParser(CRON_DEFINITION);
  private static final long DEFAULT_MAX_DELAY_MILLIS = ChronoUnit.DAYS.getDuration().toMillis() * 365L * 5L;

  private final Mode type;
  private final DurationValue duration;
  private final String expression;
  private final String zoneId;

  private ScheduleValue(Mode type, DurationValue duration, String expression, String zoneId) {
    this.type = type;
    this.duration = duration;
    this.expression = expression;
    this.zoneId = zoneId;
  }

  public static ScheduleValue ofDuration(DurationValue duration) {
    return new ScheduleValue(Mode.DURATION, duration, null, null);
  }

  public static ScheduleValue ofCron(String expression, String zoneId) {
    return new ScheduleValue(Mode.CRON, null, expression, zoneId);
  }

  /**
   * Computes delay from now to next execution in milliseconds.
   *
   * <p>This value is relative (duration), not an absolute epoch timestamp.</p>
   */
  public long toDelayMillis() {
    return toDelayMillis(Instant.now());
  }

  /**
   * Computes delay from a given instant to next execution in milliseconds.
   *
   * <p>This value is relative (duration), not an absolute epoch timestamp.</p>
   */
  public long toDelayMillis(Instant from) {
    if (type == Mode.DURATION) {
      return duration == null ? 0L : Math.max(0L, duration.toMillis());
    }

    if (expression == null || expression.isBlank()) {
      throw new IllegalStateException("Cron expression cannot be null/blank");
    }

    ZoneId zone = zoneId == null || zoneId.isBlank() ? ZoneId.systemDefault() : ZoneId.of(zoneId);
    ZonedDateTime base = ZonedDateTime.ofInstant(from, zone).truncatedTo(ChronoUnit.SECONDS);
    Cron cron = parseCron(expression);
    ExecutionTime executionTime = ExecutionTime.forCron(cron);
    Optional<ZonedDateTime> next = executionTime.nextExecution(base);

    if (next.isEmpty()) {
      throw new IllegalStateException("Could not compute next execution for cron: " + expression);
    }

    return Math.max(0L, ChronoUnit.MILLIS.between(from, next.get().toInstant()));
  }

  /**
   * Computes the next execution absolute time as epoch milliseconds.
   *
   * <p>Use this when you need to compare with {@link System#currentTimeMillis()}.</p>
   *
   * @return absolute next execution timestamp (epoch millis)
   */
  public long toNextEpochMillis() {
    return toNextEpochMillis(Instant.now());
  }

  /**
   * Computes the next execution absolute time as epoch milliseconds from a base instant.
   *
   * @param from base instant used to compute next execution
   * @return absolute next execution timestamp (epoch millis)
   */
  public long toNextEpochMillis(Instant from) {
    long delay = toDelayMillis(from);
    try {
      return Math.addExact(from.toEpochMilli(), delay);
    } catch (ArithmeticException ignored) {
      return Long.MAX_VALUE;
    }
  }

  /**
   * Returns a sanitized delay using this schedule as source of truth.
   *
   * <p>Use this when you load old/stored delay values and want to guarantee a valid result
   * even if config changed (e.g. duration reduced, cron changed, corrupt long values).</p>
   *
   * <p>Rules:</p>
   * <ul>
   *   <li>Negative or zero values => recompute from current schedule.</li>
   *   <li>Values greater than maxAllowedMillis => recompute from current schedule.</li>
   *   <li>Otherwise returns the provided delay as-is.</li>
   * </ul>
   *
   * @param rawDelayMillis   loaded/stored delay to validate
   * @param maxAllowedMillis safety upper bound (must be > 0)
   * @return normalized delay in millis
   */
  public long normalizeDelayMillis(long rawDelayMillis, long maxAllowedMillis) {
    long cap = maxAllowedMillis > 0L ? maxAllowedMillis : DEFAULT_MAX_DELAY_MILLIS;
    if (rawDelayMillis <= 0L || rawDelayMillis > cap) {
      return clamp(toDelayMillis(), cap);
    }
    return rawDelayMillis;
  }

  /**
   * Same as {@link #normalizeDelayMillis(long, long)} but with default max cap.
   */
  public long normalizeDelayMillis(long rawDelayMillis) {
    return normalizeDelayMillis(rawDelayMillis, DEFAULT_MAX_DELAY_MILLIS);
  }

  /**
   * Normalizes an absolute epoch target and returns a safe delay from now.
   *
   * <p>This is useful when persisted data stores "next run at epoch millis" instead of delay.</p>
   *
   * @param targetEpochMillis absolute timestamp in millis
   * @param now               current instant
   * @param maxAllowedMillis  safety upper bound (must be > 0)
   * @return normalized delay in millis
   */
  public long normalizeTargetEpochMillis(long targetEpochMillis, Instant now, long maxAllowedMillis) {
    long cap = maxAllowedMillis > 0L ? maxAllowedMillis : DEFAULT_MAX_DELAY_MILLIS;

    if (targetEpochMillis <= 0L) {
      return clamp(toDelayMillis(now), cap);
    }

    long computed;
    try {
      computed = Math.max(0L, ChronoUnit.MILLIS.between(now, Instant.ofEpochMilli(targetEpochMillis)));
    } catch (RuntimeException ex) {
      return clamp(toDelayMillis(now), cap);
    }

    if (computed <= 0L || computed > cap) {
      return clamp(toDelayMillis(now), cap);
    }
    return computed;
  }

  private static long clamp(long value, long max) {
    if (value < 0L) return 0L;
    return Math.min(value, max);
  }

  @Override
  public JsonElement serialize(ScheduleValue src, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
    if (src == null) {
      return JsonNull.INSTANCE;
    }

    JsonObject obj = new JsonObject();
    obj.addProperty("type", src.type.name().toLowerCase(Locale.ROOT));

    if (src.type == Mode.DURATION) {
      obj.addProperty("value", src.duration != null ? src.duration.toString() : "60m");
    } else {
      obj.addProperty("expression", src.expression);
      if (src.zoneId != null && !src.zoneId.isBlank()) {
        obj.addProperty("zoneId", src.zoneId);
      }
    }

    return obj;
  }

  @Override
  public ScheduleValue deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context)
    throws JsonParseException {
    if (json == null || json.isJsonNull()) {
      return INSTANCE;
    }

    if (json.isJsonPrimitive()) {
      JsonPrimitive p = json.getAsJsonPrimitive();
      if (p.isNumber()) {
        return ofDuration(DurationValue.parse(p.getAsLong() + "m"));
      }
      if (p.isString()) {
        String raw = p.getAsString().trim();
        if (isCronExpression(raw)) {
          return ofCron(raw, null);
        }
        return ofDuration(DurationValue.parse(raw));
      }
      throw new JsonParseException("Invalid primitive for ScheduleValue: " + json);
    }

    if (!json.isJsonObject()) {
      throw new JsonParseException("Expected object for ScheduleValue: " + json);
    }

    JsonObject obj = json.getAsJsonObject();

    String modeText = getAsString(obj, "type");
    String zone = getAsString(obj, "zoneId");
    if (zone == null) {
      zone = getAsString(obj, "timezone");
    }

    if (modeText != null) {
      String normalized = modeText.trim().toLowerCase(Locale.ROOT);
      if ("cron".equals(normalized)) {
        String expr = firstNonBlank(getAsString(obj, "expression"), getAsString(obj, "cron"), getAsString(obj, "value"));
        if (expr == null) {
          throw new JsonParseException("Cron schedule requires 'expression' (or 'cron'/'value')");
        }
        return ofCron(expr, zone);
      }

      if ("duration".equals(normalized)) {
        String value = firstNonBlank(getAsString(obj, "value"), getAsString(obj, "duration"));
        if (value == null) {
          throw new JsonParseException("Duration schedule requires 'value' (or 'duration')");
        }
        return ofDuration(DurationValue.parse(value));
      }
    }

    // Type not provided: infer by fields
    String expr = firstNonBlank(getAsString(obj, "expression"), getAsString(obj, "cron"));
    if (expr != null) {
      return ofCron(expr, zone);
    }

    String value = firstNonBlank(getAsString(obj, "value"), getAsString(obj, "duration"));
    if (value != null) {
      return ofDuration(DurationValue.parse(value));
    }

    throw new JsonParseException("Invalid schedule object: " + obj);
  }

  private static String getAsString(JsonObject obj, String key) {
    JsonElement e = obj.get(key);
    if (e == null || e.isJsonNull()) return null;
    if (!e.isJsonPrimitive()) return null;
    JsonPrimitive p = e.getAsJsonPrimitive();
    return p.isString() || p.isNumber() || p.isBoolean() ? p.getAsString() : null;
  }

  private static String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) return value;
    }
    return null;
  }

  private static boolean isCronExpression(String raw) {
    try {
      parseCron(raw);
      return true;
    } catch (RuntimeException ignored) {
      return false;
    }
  }

  private static Cron parseCron(String expression) {
    Cron cron = CRON_PARSER.parse(expression);
    cron.validate();
    return cron;
  }
}
