package com.kingpixel.cobbleutils.adapter;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.*;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.mojang.datafixers.util.Pair;

import java.lang.reflect.Type;

public class PokemonAdapter implements JsonSerializer<Pokemon>, JsonDeserializer<Pokemon> {
    public static final PokemonAdapter INSTANCE = new PokemonAdapter();

    @Override
    public JsonElement serialize(Pokemon src, Type typeOfSrc, JsonSerializationContext context) {
        if (src == null) {
            CobbleUtils.LOGGER.error("Attempted to serialize a null Pokemon.");
            return JsonNull.INSTANCE;
        }
        try {
            return Pokemon.getCODEC().encodeStart(ItemStackAdapter.getOps(), src)
                .result()
                .orElseGet(() -> {
                    CobbleUtils.LOGGER.error("Error serializing Pokemon: {}" + src);
                    return JsonNull.INSTANCE;
                });
        } catch (Exception e) {
            CobbleUtils.LOGGER.error("Exception while serializing Pokemon: {}", e.getMessage());
            return JsonNull.INSTANCE; // or throw a custom exception if needed
        }
    }

    @Override
    public Pokemon deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json == null || json.isJsonNull()) {
            CobbleUtils.LOGGER.error("Attempted to deserialize a null or empty Pokemon JSON.");
            return null;
        }
        try {
            return Pokemon.getCODEC().decode(ItemStackAdapter.getOps(), json)
                .result()
                .map(Pair::getFirst)
                .orElseGet(() -> {
                    CobbleUtils.LOGGER.error("Error deserializing Pokemon: {}" + json);
                    return null;
                });
        } catch (Exception e) {
            CobbleUtils.LOGGER.error("Exception while deserializing Pokemon: {}", e.getMessage());
            return null; // or throw a custom exception if needed
        }
    }
}
