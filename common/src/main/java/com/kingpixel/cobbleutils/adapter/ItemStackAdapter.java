package com.kingpixel.cobbleutils.adapter;

import com.google.gson.*;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryOps;

import java.lang.reflect.Type;

public class ItemStackAdapter implements JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {
  public static final ItemStackAdapter INSTANCE = new ItemStackAdapter();

  @Override
  public JsonElement serialize(ItemStack src, Type typeOfSrc, JsonSerializationContext context) {
    return ItemStack.CODEC.encodeStart(getOps(), src)
      .result()
      .orElseGet(() -> {
        CobbleUtils.LOGGER.error("Error serializing ItemStack: " + src);
        return JsonNull.INSTANCE;
      });
  }

  @Override
  public ItemStack deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    return ItemStack.CODEC.parse(getOps(), json)
      .result()
      .orElseGet(() -> {
        CobbleUtils.LOGGER.error("Error deserializing ItemStack: " + json);
        return ItemStack.EMPTY;
      });
  }

  public static RegistryOps<JsonElement> getOps() {
    if (ops == null) {
      try {
        DynamicRegistryManager registryManager = CobbleUtils.server.getRegistryManager();
        ops = RegistryOps.of(JsonOps.INSTANCE, registryManager);
      } catch (Exception e) {
        CobbleUtils.LOGGER.error("Error initializing RegistryOps for ItemStackAdapter");
        e.printStackTrace();
      }
    }
    return ops;
  }

  private static RegistryOps<JsonElement> ops;

}
