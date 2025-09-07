package com.kingpixel.cobbleutils.adapter;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Custom Gson TypeAdapter for serializing and deserializing {@link Instant} objects.
 * This adapter handles ISO 8601 formatted {@link Instant}s and provides custom error handling for malformed date strings.
 *
 * @author Carlos Varas Alonso - 27/08/2025 18:18
 */
public class InstantTypeAdapter extends TypeAdapter<Instant> {
  public static final InstantTypeAdapter INSTANCE = new InstantTypeAdapter();
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;

  /**
   * Serializes an {@link Instant} object into a JSON string.
   *
   * @param out   the JsonWriter to write to
   * @param value the Instant value to serialize
   *
   * @throws IOException if an I/O error occurs
   */
  @Override
  public void write(final JsonWriter out, final Instant value) throws IOException {
    if (value == null) {
      out.nullValue(); // Write 'null' if the value is null
      return;
    }
    out.value(FORMATTER.format(value)); // Serialize the Instant in ISO 8601 format
  }

  /**
   * Deserializes a JSON string into an {@link Instant} object.
   * It attempts to parse the string using ISO 8601 format. If the format is invalid, an exception is thrown.
   *
   * @param in the JsonReader to read from
   *
   * @return the deserialized Instant object
   *
   * @throws IOException        if an I/O error occurs
   * @throws JsonParseException if the string is malformed and cannot be parsed into an Instant
   */
  @Override
  public Instant read(final JsonReader in) throws IOException {
    try {
      // Check if the next token is null in the JSON input
      if (in.peek() == JsonToken.NULL) {
        in.nextNull(); // Skip the null value in the input
        return null; // Return null if the value is null
      }

      // Read the next string and try to parse it as an Instant
      String dateString = in.nextString();
      return Instant.from(FORMATTER.parse(dateString)); // Deserialize the Instant using ISO 8601 format

    } catch (DateTimeParseException e) {
      // Handle malformed date format
      throw new JsonParseException("Malformed ISO instant format: " + e.getMessage(), e);

      // Alternatively, you can return a default Instant, such as Instant.EPOCH:
      // return Instant.EPOCH; // Returns the epoch (1970-01-01T00:00:00Z) if format is invalid
    }
  }
}
