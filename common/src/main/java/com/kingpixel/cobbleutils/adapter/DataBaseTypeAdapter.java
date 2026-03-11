package com.kingpixel.cobbleutils.adapter;

/**
 * @author Carlos Varas Alonso - 23/09/2025 23:34
 */

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.model.DataBaseType;

import java.io.IOException;
import java.util.Arrays;

public class DataBaseTypeAdapter extends TypeAdapter<DataBaseType> {
  public static final DataBaseTypeAdapter INSTANCE = new DataBaseTypeAdapter();

  @Override
  public void write(JsonWriter out, DataBaseType value) throws IOException {
    if (value == null) {
      CobbleUtils.LOGGER.fatal("Database type is null, defaulting to JSON");
      out.value(DataBaseType.JSON.name());
      return;
    }
    out.value(value.name());
  }

  @Override
  public DataBaseType read(JsonReader in) throws IOException {
    String value = in.nextString();
    if (value == null) {
      CobbleUtils.LOGGER.fatal("Database type is null, defaulting to JSON");
      return DataBaseType.JSON;
    }
    for (DataBaseType type : DataBaseType.values()) {
      if (type.name().equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("database type not found: " + value + ", the valid types are: " + Arrays.toString(DataBaseType.values()));
  }
}
