package com.kingpixel.cobbleutils.adapter;

import com.google.gson.*;
import com.kingpixel.cobbleutils.Model.conditions.*;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ConditionAdapter implements JsonDeserializer<Condition>, JsonSerializer<Condition> {

  public static final ConditionAdapter INSTANCE = new ConditionAdapter();

  private static final Map<String, Class<? extends Condition>> TYPES =
    new ConcurrentHashMap<>();

  static {
    // Minecraft conditions
    register(PermissionCondition.TYPE, PermissionCondition.class);
    register(WorldCondition.TYPE, WorldCondition.class);
    register(ServerCondition.TYPE, ServerCondition.class);
    register(ZoneCondition.TYPE, ZoneCondition.class);
    register(HeightCondition.TYPE, HeightCondition.class);
    register(BiomeCondition.TYPE, BiomeCondition.class);
    register(TimeOfDayMinecraftCondition.TYPE, TimeOfDayMinecraftCondition.class);
    register(WeatherCondition.TYPE, WeatherCondition.class);
    register(NearBlockCondition.TYPE, NearBlockCondition.class);
    // Cobblemon conditions
    register(MolangCondition.TYPE, MolangCondition.class);
    register(HasPokemonPartyCondition.TYPE, HasPokemonPartyCondition.class);
    register(HasPokemonPartyAmountCondition.TYPE, HasPokemonPartyAmountCondition.class);
  }

  private ConditionAdapter() {
  }

  public static void register(String id, Class<? extends Condition> clazz) {
    TYPES.put(id, clazz);
  }

  @Override
  public Condition deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
    throws JsonParseException {

    JsonObject obj = json.getAsJsonObject();
    String type = obj.get("type").getAsString();

    Class<? extends Condition> clazz = TYPES.get(type);

    if (clazz == null) {
      throw new JsonParseException("Unknown condition type: " + type);
    }

    return context.deserialize(json, clazz);
  }

  @Override
  public JsonElement serialize(Condition src, Type typeOfSrc, JsonSerializationContext context) {
    JsonElement json = context.serialize(src, src.getClass());
    json.getAsJsonObject().addProperty("type", src.getType());
    return json;
  }
}