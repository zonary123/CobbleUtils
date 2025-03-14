package com.kingpixel.cobbleutils.adapter;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.*;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.mojang.serialization.JsonOps;

import java.lang.reflect.Type;

/**
 * @author Carlos Varas Alonso - 10/03/2025 22:56
 */
public class PokemonAdapter implements JsonSerializer<Pokemon>, JsonDeserializer<Pokemon> {
  public static final PokemonAdapter INSTANCE = new PokemonAdapter();

  @Override
  public JsonElement serialize(Pokemon src, Type typeOfSrc, JsonSerializationContext context) {
    JsonElement jsonElement = Pokemon.getCODEC().encodeStart(JsonOps.INSTANCE, src).getOrThrow();
    if (jsonElement == null) CobbleUtils.LOGGER.info("Error serializing pokemon");
    return jsonElement;
  }

  @Override
  public Pokemon deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    Pokemon pokemon = Pokemon.getCODEC().decode(JsonOps.INSTANCE, json).getOrThrow().getFirst();
    if (pokemon == null) CobbleUtils.LOGGER.info("Error deserializing pokemon");
    return pokemon;
  }
}
