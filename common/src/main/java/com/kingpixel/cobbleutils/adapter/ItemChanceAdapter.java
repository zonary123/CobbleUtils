package com.kingpixel.cobbleutils.adapter;

import com.google.gson.*;
import com.kingpixel.cobbleutils.Model.ItemChance;

import java.lang.reflect.Type;
import java.util.UUID;

public class ItemChanceAdapter implements JsonSerializer<ItemChance>, JsonDeserializer<ItemChance> {

  public static final ItemChanceAdapter INSTANCE = new ItemChanceAdapter();

  @Override
  public JsonElement serialize(ItemChance src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject obj = new JsonObject();
    obj.addProperty("item", src.getItem());
    obj.addProperty("chance", src.getChance());
    obj.addProperty("display", src.getDisplay());
    obj.addProperty("displayname", src.getDisplayname());

    boolean unique = src.getUnique() != null && src.getUnique();
    obj.addProperty("unique", unique);

    if (unique) {
      String identifier = src.getIdentifier() != null ? src.getIdentifier() : UUID.randomUUID().toString();
      int amount = src.getAmount() != null ? src.getAmount() : 1;
      int cooldown = src.getCooldown() != null ? src.getCooldown() : 60;

      obj.addProperty("identifier", identifier);
      obj.addProperty("amount", amount);
      obj.addProperty("cooldown", cooldown);

      // Guardar los valores por defecto en el objeto también
      src.setIdentifier(identifier);
      src.setAmount(amount);
      src.setCooldown(cooldown);
    }

    return obj;
  }

  @Override
  public ItemChance deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
    throws JsonParseException {

    JsonObject obj = json.getAsJsonObject();
    String item = obj.has("item") ? obj.get("item").getAsString() : "";
    double chance = obj.has("chance") ? obj.get("chance").getAsDouble() : 0.0;

    ItemChance itemChance = new ItemChance(item, chance);

    if (obj.has("display")) itemChance.setDisplay(obj.get("display").getAsString());
    if (obj.has("displayname")) itemChance.setDisplayname(obj.get("displayname").getAsString());

    boolean unique = obj.has("unique") && obj.get("unique").getAsBoolean();
    itemChance.setUnique(unique);

    if (unique) {
      if (obj.has("identifier")) itemChance.setIdentifier(obj.get("identifier").getAsString());
      itemChance.setAmount(obj.has("amount") ? obj.get("amount").getAsInt() : 1);
      itemChance.setCooldown(obj.has("cooldown") ? obj.get("cooldown").getAsInt() : 60);
    }

    return itemChance;
  }
}
