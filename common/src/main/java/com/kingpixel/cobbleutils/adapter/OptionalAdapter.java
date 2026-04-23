package com.kingpixel.cobbleutils.adapter;

import com.google.gson.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * Gson adapter for {@link Optional}.
 * <ul>
 *   <li>Serializes {@code Optional.empty()} as JSON {@code null}.</li>
 *   <li>Serializes {@code Optional.of(value)} as the serialized value.</li>
 *   <li>Deserializes JSON {@code null} or missing field as {@code Optional.empty()}.</li>
 * </ul>
 *
 * <h3>Registration</h3>
 * <pre>{@code
 * UtilsFile.registerAdapter(Optional.class, OptionalAdapter.INSTANCE);
 * // or inside GsonBuilder:
 * new GsonBuilder().registerTypeHierarchyAdapter(Optional.class, OptionalAdapter.INSTANCE);
 * }</pre>
 *
 * <h3>Usage in a model</h3>
 * <pre>{@code
 * public class PlayerData {
 *     private Optional<String> nickname = Optional.empty();
 *     private Optional<Integer> score   = Optional.of(100);
 * }
 * }</pre>
 */
public class OptionalAdapter implements JsonSerializer<Optional<?>>, JsonDeserializer<Optional<?>> {

  public static final OptionalAdapter INSTANCE = new OptionalAdapter();

  private OptionalAdapter() {
  }

  @Override
  public JsonElement serialize(Optional<?> src, Type typeOfSrc, JsonSerializationContext context) {
    return src.map(value -> context.serialize(value, resolveValueType(typeOfSrc)))
        .orElse(JsonNull.INSTANCE);
  }

  @Override
  public Optional<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    if (json == null || json.isJsonNull()) {
      return Optional.empty();
    }
    Type valueType = resolveValueType(typeOfT);
    return Optional.ofNullable(context.deserialize(json, valueType));
  }

  /**
   * Extracts the type argument {@code T} from {@code Optional<T>}.
   * Falls back to {@link Object} if the type is raw.
   */
  private static Type resolveValueType(Type optionalType) {
    if (optionalType instanceof ParameterizedType paramType) {
      Type[] args = paramType.getActualTypeArguments();
      if (args.length == 1) return args[0];
    }
    return Object.class;
  }
}

