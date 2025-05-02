package com.kingpixel.cobbleutils.adapter;

import com.google.gson.*;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Type;

public class ItemStackAdapter implements JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {
  public static final ItemStackAdapter INSTANCE = new ItemStackAdapter();

  @Override
  public JsonElement serialize(ItemStack src, Type typeOfSrc, JsonSerializationContext context) {
    return ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, src)
      .result()
      .orElseGet(() -> {
        CobbleUtils.LOGGER.error("Error serializing ItemStack: {}" + src);

        return JsonNull.INSTANCE;
      });
  }

  @Override
  public ItemStack deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    return ItemStack.CODEC.parse(JsonOps.INSTANCE, json)
      .result()
      .orElseGet(() -> {
        CobbleUtils.LOGGER.error("Error deserializing ItemStack: {}" + json);
        return null;
      });
  }
}
