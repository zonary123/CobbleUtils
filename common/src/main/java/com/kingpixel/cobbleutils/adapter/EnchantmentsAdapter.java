package com.kingpixel.cobbleutils.adapter;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.serialization.JsonOps;
import net.minecraft.enchantment.Enchantment;

import java.lang.reflect.Type;

/**
 * @author Carlos Varas Alonso - 18/04/2025 0:01
 */
public class EnchantmentsAdapter implements JsonSerializer<Enchantment>, JsonDeserializer<Enchantment> {
  public static final EnchantmentsAdapter INSTANCE = new EnchantmentsAdapter();

  @Override public JsonElement serialize(Enchantment src, Type typeOfSrc, JsonSerializationContext context) {
    return Enchantment.CODEC.encodeStart(JsonOps.INSTANCE, src).getOrThrow();
  }

  @Override
  public Enchantment deserialize(JsonElement json, Type typeOfT, com.google.gson.JsonDeserializationContext context) {
    return Enchantment.CODEC.parse(JsonOps.INSTANCE, json).result().orElse(null);
  }
}
