package com.kingpixel.cobbleutils.adapter;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

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
    JsonToken token = in.peek();

    // 1) Null → return null
    if (token == JsonToken.NULL) {
      in.nextNull();
      return null;
    }

    // 2) Objeto => puede ser { "$date": "..." }
    if (token == JsonToken.BEGIN_OBJECT) {
      in.beginObject();

      String name = in.nextName(); // debería ser "$date"
      String value = in.nextString(); // el valor real

      in.endObject();

      return parseInstant(value); // reutilizamos la lógica de parseo
    }

    // 3) String normal
    if (token == JsonToken.STRING) {
      String value = in.nextString();
      return parseInstant(value);
    }

    // 4) Número => epoch millis
    if (token == JsonToken.NUMBER) {
      long epochMillis = in.nextLong();
      return Instant.ofEpochMilli(epochMillis);
    }

    throw new JsonParseException("Tipo no soportado para Instant: " + token);
  }

// ---- MÉTODO AUXILIAR PARA PARSEAR EN ORDEN ----

  private Instant parseInstant(String dateString) {
    // 1) Tu FORMATTER primero
    try {
      return Instant.from(FORMATTER.parse(dateString));
    } catch (Exception ignored) {
    }

    // 2) ISO_OFFSET_DATE_TIME
    try {
      return OffsetDateTime.parse(dateString, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
    } catch (Exception ignored) {
    }

    // 3) Epoch millis
    try {
      long millis = Long.parseLong(dateString);
      return Instant.ofEpochMilli(millis);
    } catch (Exception ignored) {
    }

    throw new JsonParseException("Formato de fecha inválido: " + dateString);
  }


}
