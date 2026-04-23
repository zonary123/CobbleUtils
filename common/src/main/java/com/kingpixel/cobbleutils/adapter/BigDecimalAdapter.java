package com.kingpixel.cobbleutils.adapter;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.math.BigDecimal;

/**
 * Gson adapter for {@link BigDecimal}.
 *
 * <p>Serializes as a JSON string to preserve full precision (avoids double rounding).
 * This is especially important for economy values, prices, and weights.</p>
 *
 * <h3>JSON representation</h3>
 * <pre>
 * BigDecimal("123.456789012345") → "123.456789012345"
 * </pre>
 *
 * <h3>Registration</h3>
 * <pre>{@code
 * UtilsFile.registerAdapter(BigDecimal.class, BigDecimalAdapter.INSTANCE);
 * }</pre>
 */
public class BigDecimalAdapter implements JsonSerializer<BigDecimal>, JsonDeserializer<BigDecimal> {

  public static final BigDecimalAdapter INSTANCE = new BigDecimalAdapter();

  private BigDecimalAdapter() {
  }

  @Override
  public JsonElement serialize(BigDecimal src, Type typeOfSrc, JsonSerializationContext context) {
    // Stored as string to avoid any IEEE-754 precision loss
    return new JsonPrimitive(src.toPlainString());
  }

  @Override
  public BigDecimal deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    try {
      if (json.isJsonPrimitive()) {
        JsonPrimitive prim = json.getAsJsonPrimitive();
        return new BigDecimal(prim.getAsString());
      }
      throw new JsonParseException("Expected a JSON primitive for BigDecimal, got: " + json);
    } catch (NumberFormatException e) {
      throw new JsonParseException("Cannot parse BigDecimal from: " + json.getAsString(), e);
    }
  }
}

