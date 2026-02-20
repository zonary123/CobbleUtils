package com.kingpixel.cobbleutils.adapter;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.*;
import com.mojang.datafixers.util.Pair;

import java.lang.reflect.Type;

public class PokemonAdapter implements JsonSerializer<Pokemon>, JsonDeserializer<Pokemon> {

  public static final PokemonAdapter INSTANCE = new PokemonAdapter();

  @Override
  public JsonElement serialize(Pokemon src, Type typeOfSrc, JsonSerializationContext context) {
    if (src == null) {
      throw new JsonParseException("Attempted to serialize a null Pokemon.");
    }

    try {
      return Pokemon.getCODEC().encodeStart(ItemStackAdapter.getOps(), src)
        .result()
        .orElseThrow(() -> new JsonParseException("Failed to serialize Pokemon: " + src));
    } catch (Exception e) {
      throw new JsonParseException("Exception while serializing Pokemon: " + e.getMessage(), e);
    }
  }

  @Override
  public Pokemon deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    if (json == null || json.isJsonNull()) {
      throw new JsonParseException("Attempted to deserialize a null or empty Pokemon JSON.");
    }

    try {
      return Pokemon.getCODEC().decode(ItemStackAdapter.getOps(), json)
        .result()
        .map(Pair::getFirst)
        .orElseThrow(() -> new JsonParseException("Failed to deserialize Pokemon: " + json));
    } catch (Exception e) {
      throw new JsonParseException("Exception while deserializing Pokemon: " + e.getMessage(), e);
    }
  }
}