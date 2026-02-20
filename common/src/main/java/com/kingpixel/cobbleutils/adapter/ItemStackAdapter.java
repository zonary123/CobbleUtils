package com.kingpixel.cobbleutils.adapter;

import com.google.gson.*;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryOps;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

public class ItemStackAdapter implements JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {

  public static final ItemStackAdapter INSTANCE = new ItemStackAdapter();
  private static RegistryOps<JsonElement> ops;

  @Override
  public JsonElement serialize(ItemStack src, Type typeOfSrc, JsonSerializationContext context) {
    if (src == null) {
      throw new JsonParseException("Attempted to serialize a null ItemStack.");
    }

    try {
      return ItemStack.CODEC.encodeStart(getOps(), src)
        .result()
        .orElseThrow(() -> new JsonParseException("Failed to serialize ItemStack: " + src));
    } catch (Exception e) {
      throw new JsonParseException("Exception while serializing ItemStack: " + e.getMessage(), e);
    }
  }

  @Override
  public ItemStack deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    if (json == null || json.isJsonNull()) {
      throw new JsonParseException("Attempted to deserialize a null or empty ItemStack JSON.");
    }

    try {
      return ItemStack.CODEC.parse(getOps(), json)
        .result()
        .orElseThrow(() -> new JsonParseException("Failed to deserialize ItemStack: " + json));
    } catch (Exception e) {
      throw new JsonParseException("Exception while deserializing ItemStack: " + e.getMessage(), e);
    }
  }

  @Nullable
  public static RegistryOps<JsonElement> getOps() {
    if (ops == null) {
      try {
        if (CobbleUtils.server == null) {
          throw new JsonParseException("Cannot initialize RegistryOps for ItemStackAdapter: server is null");
        }
        DynamicRegistryManager registryManager = CobbleUtils.server.getRegistryManager();
        ops = RegistryOps.of(JsonOps.INSTANCE, registryManager);
      } catch (Exception e) {
        throw new JsonParseException("Error initializing RegistryOps for ItemStackAdapter", e);
      }
    }
    return ops;
  }
}