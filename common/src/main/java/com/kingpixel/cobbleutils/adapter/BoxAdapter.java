package com.kingpixel.cobbleutils.adapter;

import com.google.gson.*;
import net.minecraft.util.math.Box;

import java.lang.reflect.Type;

public class BoxAdapter implements JsonSerializer<Box>, JsonDeserializer<Box> {

  public static final BoxAdapter INSTANCE = new BoxAdapter();

  /**
   * Obtiene un valor double de un JsonObject o devuelve 0 si no existe o es null
   */
  private static double getDouble(JsonObject obj, String key) {
    if (obj.has(key) && !obj.get(key).isJsonNull()) {
      return obj.get(key).getAsDouble();
    }
    return 0;
  }

  @Override
  public Box deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    if (!json.isJsonObject()) {
      throw new JsonParseException("Expected JsonObject for Box");
    }

    JsonObject obj = json.getAsJsonObject();

    double minX = getDouble(obj, "minX");
    double minY = getDouble(obj, "minY");
    double minZ = getDouble(obj, "minZ");
    double maxX = getDouble(obj, "maxX");
    double maxY = getDouble(obj, "maxY");
    double maxZ = getDouble(obj, "maxZ");

    return new Box(minX, minY, minZ, maxX, maxY, maxZ);
  }

  @Override
  public JsonElement serialize(Box src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject obj = new JsonObject();
    obj.addProperty("minX", src.minX);
    obj.addProperty("minY", src.minY);
    obj.addProperty("minZ", src.minZ);
    obj.addProperty("maxX", src.maxX);
    obj.addProperty("maxY", src.maxY);
    obj.addProperty("maxZ", src.maxZ);
    return obj;
  }
}
