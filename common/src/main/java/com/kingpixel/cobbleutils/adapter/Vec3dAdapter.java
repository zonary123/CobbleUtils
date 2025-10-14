package com.kingpixel.cobbleutils.adapter;

/**
 * @author Carlos Varas Alonso - 23/09/2025 23:34
 */

import com.google.gson.*;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Type;

public class Vec3dAdapter implements JsonSerializer<Vec3d>, JsonDeserializer<Vec3d> {
  public static final Vec3dAdapter INSTANCE = new Vec3dAdapter();

  @Override
  public JsonElement serialize(Vec3d src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject o = new JsonObject();
    o.addProperty("x", src.x);
    o.addProperty("y", src.y);
    o.addProperty("z", src.z);
    return o;
  }

  @Override
  public Vec3d deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
    JsonObject o = json.getAsJsonObject();
    return new Vec3d(o.get("x").getAsDouble(), o.get("y").getAsDouble(), o.get("z").getAsDouble());
  }
}

