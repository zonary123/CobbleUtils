package com.kingpixel.cobbleutils.adapter;

import com.google.gson.*;
import com.kingpixel.cobbleutils.Model.zones.zoneshapes.*;

import java.lang.reflect.Type;
import java.util.Map;

public class ZoneShapeAdapter implements JsonDeserializer<ZoneShape>, JsonSerializer<ZoneShape> {
  public static final ZoneShapeAdapter INSTANCE = new ZoneShapeAdapter();
  public static final Map<String, Class<? extends ZoneShape>> SHAPE_TYPES = Map.of(
    CompositeShape.TYPE, CompositeShape.class,
    CuboidShape.TYPE, CuboidShape.class,
    CylinderShape.TYPE, CylinderShape.class,
    PolygonShape.TYPE, PolygonShape.class,
    SphereShape.TYPE, SphereShape.class
  );


  @Override
  public ZoneShape deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    JsonObject obj = json.getAsJsonObject();
    String type = obj.get("type").getAsString();
    Class<? extends ZoneShape> clazz = SHAPE_TYPES.get(type);
    if (clazz == null) {
      throw new JsonParseException("Unknown ZoneShape type: " + type);
    }
    ZoneShape shape = context.deserialize(json, clazz);
    shape.fix();
    return shape;
  }

  @Override
  public JsonElement serialize(ZoneShape src, Type typeOfSrc, JsonSerializationContext context) {
    src.fix();
    return context.serialize(src, src.getClass());
  }
}
