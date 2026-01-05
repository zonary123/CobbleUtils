package com.kingpixel.cobbleutils.adapter;

import com.google.gson.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicReference;

public class AtomicReferenceAdapter implements JsonSerializer<AtomicReference<?>>, JsonDeserializer<AtomicReference<?>> {

  public static final AtomicReferenceAdapter INSTANCE = new AtomicReferenceAdapter();

  @Override
  public JsonElement serialize(AtomicReference<?> src, Type typeOfSrc, JsonSerializationContext context) {
    return context.serialize(src.get()); // serializa solo el valor interno
  }

  @Override
  public AtomicReference<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    // Obtiene el tipo contenido dentro del AtomicReference<T>
    Type valueType = Object.class;

    if (typeOfT instanceof ParameterizedType paramType) {
      Type[] typeArgs = paramType.getActualTypeArguments();
      if (typeArgs.length == 1) {
        valueType = typeArgs[0]; // el tipo T
      }
    }

    Object value = context.deserialize(json, valueType);
    return new AtomicReference<>(value);
  }
}


