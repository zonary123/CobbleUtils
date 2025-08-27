package com.kingpixel.cobbleutils.adapter;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * @author Carlos Varas Alonso - 27/08/2025 18:18
 */
public class InstantTypeAdapter extends TypeAdapter<Instant> {
  public static final InstantTypeAdapter INSTANCE = new InstantTypeAdapter();
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;

  @Override
  public void write(final JsonWriter out, final Instant value) throws IOException {
    if (value == null) {
      out.nullValue();
      return;
    }
    out.value(FORMATTER.format(value));
  }

  @Override
  public Instant read(final JsonReader in) throws IOException {
    try {
      if (in.peek() == null) {
        in.nextNull();
        return null;
      }
      return Instant.from(FORMATTER.parse(in.nextString()));
    } catch (final DateTimeParseException e) {
      throw new JsonParseException("Malformed ISO instant format");
    }
  }
}