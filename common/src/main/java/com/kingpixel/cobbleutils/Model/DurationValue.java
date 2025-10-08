package com.kingpixel.cobbleutils.Model;

import com.google.gson.*;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a duration of time with support for multiple units.
 * <p>
 * Supports parsing from strings like "10ms", "2h", "3d", and can be serialized/deserialized
 * with Gson. It also implements Brigadier's {@link ArgumentType} for command parsing.
 * </p>
 */
public class DurationValue implements JsonSerializer<DurationValue>, JsonDeserializer<DurationValue>,
  ArgumentType<DurationValue> {

  /**
   * Default instance with 0 milliseconds.
   */
  public static final DurationValue INSTANCE = new DurationValue(0, "0ms");

  /**
   * Regex pattern to parse durations, supporting units: t, ms, s, m, h, d, w, mo, y.
   */
  private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)(ms|s|m|h|d|w|mo|y|t)?");

  /**
   * Duration in milliseconds.
   */
  private final long millis;

  /**
   * Original string representation of the duration.
   */
  private final String original;

  /**
   * Private constructor to create a DurationValue instance.
   *
   * @param millis   duration in milliseconds
   * @param original original string representation
   */
  private DurationValue(long millis, String original) {
    this.millis = millis;
    this.original = original;
  }

  /**
   * Creates a DurationValue from a millisecond value.
   *
   * @param millis duration in milliseconds
   *
   * @return a DurationValue instance
   */
  public static DurationValue ofMillis(long millis) {
    return new DurationValue(millis, millis + "ms");
  }

  /**
   * Parses a string into a DurationValue.
   * <p>
   * Supported units:
   * <ul>
   *     <li>t = ticks (1 tick = 50ms)</li>
   *     <li>ms = milliseconds</li>
   *     <li>s = seconds</li>
   *     <li>m = minutes (default if no unit)</li>
   *     <li>h = hours</li>
   *     <li>d = days</li>
   *     <li>w = weeks</li>
   *     <li>mo = months (30 days)</li>
   *     <li>y = years (365 days)</li>
   * </ul>
   * </p>
   *
   * @param value the duration string to parse
   *
   * @return a DurationValue instance
   *
   * @throws IllegalArgumentException if the string is invalid or contains unsupported units
   */
  public static DurationValue parse(String value) {
    String val = value.trim().toLowerCase();
    long totalMillis = 0;

    Matcher matcher = DURATION_PATTERN.matcher(val);

    while (matcher.find()) {
      long amount = Long.parseLong(matcher.group(1));
      String unit = matcher.group(2) != null ? matcher.group(2) : "m";

      totalMillis += switch (unit) {
        case "t" -> 50 * amount;
        case "ms" -> amount;
        case "s" -> TimeUnit.SECONDS.toMillis(amount);
        case "m" -> TimeUnit.MINUTES.toMillis(amount);
        case "h" -> TimeUnit.HOURS.toMillis(amount);
        case "d" -> TimeUnit.DAYS.toMillis(amount);
        case "w" -> TimeUnit.DAYS.toMillis(amount * 7);
        case "mo" -> TimeUnit.DAYS.toMillis(amount * 30);
        case "y" -> TimeUnit.DAYS.toMillis(amount * 365);
        default -> throw new IllegalArgumentException(
          "Invalid time unit in duration string: " + unit +
            " (valid units: ms, t, s, m, h, d, w, mo, y)"
        );
      };
    }
    return new DurationValue(totalMillis, value);
  }

  /**
   * Returns the duration in milliseconds.
   *
   * @return duration in milliseconds
   */
  public long toMillis() {
    return millis;
  }

  /**
   * Returns the duration in seconds.
   *
   * @return duration in seconds
   */
  public long toSeconds() {
    return TimeUnit.MILLISECONDS.toSeconds(millis);
  }

  @Override
  public String toString() {
    return original;
  }

  // --- Gson Serialization / Deserialization ---

  /**
   * Serializes this DurationValue to a JSON primitive using its original string.
   *
   * @param src       the source DurationValue
   * @param typeOfSrc the actual type
   * @param context   the Gson serialization context
   *
   * @return JSON element representing the duration
   */
  @Override
  public JsonElement serialize(DurationValue src, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(src != null ? src.original : "60m");
  }

  /**
   * Deserializes a JSON element into a DurationValue.
   * <p>
   * Numbers are treated as minutes, strings are parsed with {@link #parse(String)}.
   * </p>
   *
   * @param json    the JSON element to deserialize
   * @param typeOfT the target type
   * @param context the Gson deserialization context
   *
   * @return a DurationValue instance
   *
   * @throws JsonParseException if the JSON cannot be parsed into a DurationValue
   */
  @Override
  public DurationValue deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
    throws JsonParseException {
    if (json.isJsonPrimitive()) {
      JsonPrimitive primitive = json.getAsJsonPrimitive();
      if (primitive.isNumber()) {
        return parse(primitive.getAsLong() + "m"); // number = minutes
      } else if (primitive.isString()) {
        return parse(primitive.getAsString());
      }
    }
    throw new JsonParseException("Invalid JSON for DurationValue: " + json);
  }

  // --- Brigadier ArgumentType Implementation ---

  /**
   * Retrieves a DurationValue argument from the command context.
   *
   * @param context the command context
   * @param name    the argument name
   *
   * @return the parsed DurationValue
   */
  public DurationValue getArgument(CommandContext<?> context, String name) {
    return context.getArgument(name, DurationValue.class);
  }

  /**
   * Parses a DurationValue from a Brigadier StringReader.
   *
   * @param reader the StringReader
   *
   * @return parsed DurationValue
   *
   * @throws CommandSyntaxException if parsing fails
   */
  @Override
  public DurationValue parse(StringReader reader) throws CommandSyntaxException {
    return parse(reader.readString());
  }

  @Override
  public <S> DurationValue parse(StringReader reader, S source) throws CommandSyntaxException {
    return ArgumentType.super.parse(reader, source);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    return ArgumentType.super.listSuggestions(context, builder);
  }

  /**
   * Examples of valid duration strings.
   */
  private static final Collection<String> EXAMPLES = List.of(
    "50ms", "1s", "1m", "1h", "1d", "1w", "1mo", "1y", "10t"
  );

  /**
   * Returns examples of valid duration strings.
   *
   * @return collection of example strings
   */
  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }
}
