package com.kingpixel.cobbleutils.adapter;

import com.google.gson.*;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Type;

/**
 * @author Carlos Varas Alonso - 10/03/2025 22:56
 */
public class ItemStackAdapter implements JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {
  public static final ItemStackAdapter INSTANCE = new ItemStackAdapter();

  @Override
  public JsonElement serialize(ItemStack src, Type typeOfSrc, JsonSerializationContext context) {
    JsonElement jsonElement = ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, src).getOrThrow();
    if (jsonElement == null) CobbleUtils.LOGGER.info("Error serializing pokemon");
    return jsonElement;
  }

  @Override
  public ItemStack deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    ItemStack itemStack = ItemStack.CODEC.parse(JsonOps.INSTANCE, json).result().orElse(null);
    if (itemStack == null) CobbleUtils.LOGGER.info("Error deserializing pokemon");
    return itemStack;
  }
}
