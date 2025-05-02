package com.kingpixel.cobbleutils.adapter;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.*;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;

import java.lang.reflect.Type;

public class PokemonAdapter implements JsonSerializer<Pokemon>, JsonDeserializer<Pokemon> {
  public static final PokemonAdapter INSTANCE = new PokemonAdapter();

  @Override
  public JsonElement serialize(Pokemon src, Type typeOfSrc, JsonSerializationContext context) {
    return Pokemon.getCODEC().encodeStart(JsonOps.INSTANCE, src)
      .result()
      .orElseGet(() -> {
        CobbleUtils.LOGGER.error("Error serializing Pokemon: {}" + src);
        return JsonNull.INSTANCE;
      });
  }

  @Override
  public Pokemon deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    return Pokemon.getCODEC().decode(JsonOps.INSTANCE, json)
      .result()
      .map(Pair::getFirst)
      .orElseGet(() -> {
        CobbleUtils.LOGGER.error("Error deserializing Pokemon: {}" + json);
        return null;
      });
  }
}
