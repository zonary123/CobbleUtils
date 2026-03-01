package com.kingpixel.cobbleutils.adapter;

import com.google.gson.*;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DurationValue;
import com.kingpixel.cobbleutils.Model.rewards.Reward;
import com.kingpixel.cobbleutils.Model.rewards.RewardRegistry;

import java.lang.reflect.Type;

public class RewardAdapter implements JsonSerializer<Reward>, JsonDeserializer<Reward> {
  public static final RewardAdapter INSTANCE = new RewardAdapter();

  @Override
  public Reward deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    JsonObject obj = json.getAsJsonObject();

    // -----------------------------------------------
    // Migración reward/item
    String rewardValue = obj.has("reward") && !obj.get("reward").isJsonNull() ? obj.get("reward").getAsString() : null;
    if ((rewardValue == null || rewardValue.isEmpty()) && obj.has("item") && !obj.get("item").isJsonNull()) {
      rewardValue = obj.get("item").getAsString();
    }
    if (rewardValue == null || rewardValue.isEmpty()) rewardValue = "item:1:minecraft:stone";

    // Validar prefijo tipo para cada parte
    String[] parts = rewardValue.split("\\|");
    for (int i = 0; i < parts.length; i++) {
      String part = parts[i].trim();
      if (part.isEmpty()) continue;

      String type = part.contains(":") ? part.split(":", 2)[0] : "item";
      if (RewardRegistry.getRewardExecutor(type) == null) {
        parts[i] = "item:1:" + part;
        CobbleUtils.LOGGER.warn("RewardAdapter", "Reward part '" + part + "' does not have a valid type prefix. Defaulting to 'item:1:'. Full reward: '" + rewardValue + "'");
      }
    }
    rewardValue = String.join("|", parts);
    if (CobbleUtils.config != null && CobbleUtils.config.isDebug()) {
      CobbleUtils.LOGGER.info("Deserialized reward value: '" + rewardValue);
    }
    // -----------------------------------------------

    // -----------------------------------------------
    // Migración chance -> weight
    double weight = 1.0;
    if (obj.has("weight") && !obj.get("weight").isJsonNull()) {
      weight = obj.get("weight").getAsDouble();
    } else if (obj.has("chance") && !obj.get("chance").isJsonNull()) {
      weight = obj.get("chance").getAsDouble();
    }
    // -----------------------------------------------

    Boolean unique = obj.has("unique") && !obj.get("unique").isJsonNull() && obj.get("unique").getAsBoolean();
    Integer amount = obj.has("amount") && !obj.get("amount").isJsonNull() ? obj.get("amount").getAsInt() : null;
    String identifier = obj.has("identifier") && !obj.get("identifier").isJsonNull() ? obj.get("identifier").getAsString() : null;
    DurationValue cooldown = obj.has("cooldown") && !obj.get("cooldown").isJsonNull()
      ? DurationValue.parse(obj.get("cooldown").getAsString()) : null;
    String display = obj.has("display") && !obj.get("display").isJsonNull() ? obj.get("display").getAsString() : null;
    String displayname = obj.has("displayname") && !obj.get("displayname").isJsonNull() ? obj.get("displayname").getAsString() : null;

    Reward reward = Reward.builder()
      .reward(rewardValue)
      .weight(weight)
      .unique(unique)
      .amount(amount)
      .identifier(identifier)
      .cooldown(cooldown)
      .display(display)
      .displayname(displayname)
      .build();

    reward.fix();
    return reward;
  }


  @Override
  public JsonElement serialize(Reward src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject obj = new JsonObject();
    obj.addProperty("reward", src.getReward());
    obj.addProperty("weight", src.getWeight());
    if (src.getUnique() != null) obj.addProperty("unique", src.getUnique());
    if (src.getAmount() != null) obj.addProperty("amount", src.getAmount());
    if (src.getIdentifier() != null) obj.addProperty("identifier", src.getIdentifier());
    if (src.getCooldown() != null) obj.addProperty("cooldown", src.getCooldown().toString());
    if (src.getDisplay() != null) obj.addProperty("display", src.getDisplay());
    if (src.getDisplayname() != null) obj.addProperty("displayname", src.getDisplayname());
    return obj;
  }

}
