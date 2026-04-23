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
    register(StructureCondition.TYPE, StructureCondition.class);
    // Player state conditions
    register(GameModeCondition.TYPE, GameModeCondition.class);
    register(HealthCondition.TYPE, HealthCondition.class);
    register(HungerCondition.TYPE, HungerCondition.class);
    register(ExperienceLevelCondition.TYPE, ExperienceLevelCondition.class);
    register(HoldingItemCondition.TYPE, HoldingItemCondition.class);
    register(HasItemCondition.TYPE, HasItemCondition.class);
    register(SneakingCondition.TYPE, SneakingCondition.class);
    register(OnFireCondition.TYPE, OnFireCondition.class);
    register(InWaterCondition.TYPE, InWaterCondition.class);
    register(LightLevelCondition.TYPE, LightLevelCondition.class);
    register(EffectCondition.TYPE, EffectCondition.class);
    register(RealTimeCondition.TYPE, RealTimeCondition.class);
    register(DayOfWeekCondition.TYPE, DayOfWeekCondition.class);
    register(DayOfMonthCondition.TYPE, DayOfMonthCondition.class);
    register(MonthCondition.TYPE, MonthCondition.class);
    register(YearCondition.TYPE, YearCondition.class);
    register(DateRangeCondition.TYPE, DateRangeCondition.class);
    register(WeekOfYearCondition.TYPE, WeekOfYearCondition.class);
    register(RandomChanceCondition.TYPE, RandomChanceCondition.class);
    // Logic conditions
    register(NotCondition.TYPE, NotCondition.class);
    register(AndCondition.TYPE, AndCondition.class);
    register(OrCondition.TYPE, OrCondition.class);
    // Cobblemon conditions
    register(MolangCondition.TYPE, MolangCondition.class);
    register(HasPokemonPartyCondition.TYPE, HasPokemonPartyCondition.class);
    register(HasPokemonPartyAmountCondition.TYPE, HasPokemonPartyAmountCondition.class);
    register(PokemonLevelCondition.TYPE, PokemonLevelCondition.class);
  }

  private ConditionAdapter() {
  }

  public static void register(String id, Class<? extends Condition> clazz) {
    if (TYPES.containsKey(id)) throw new IllegalArgumentException("Condition type already registered: " + id);
    TYPES.put(id, clazz);
  }

  public static Map<String, Class<? extends Condition>> getRegisteredTypes() {
    return Map.copyOf(TYPES);
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