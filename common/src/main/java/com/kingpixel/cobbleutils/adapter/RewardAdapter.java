package com.kingpixel.cobbleutils.adapter;

import com.google.gson.*;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DurationValue;
import com.kingpixel.cobbleutils.Model.rewards.Reward;
import com.kingpixel.cobbleutils.Model.rewards.RewardRegistry;

import java.lang.reflect.Type;
import java.util.Set;

public class RewardAdapter implements JsonSerializer<Reward>, JsonDeserializer<Reward> {

  public static final RewardAdapter INSTANCE = new RewardAdapter();

  // 🔥 Prefijos que se consideran comandos y se auto-fijan
  private static final Set<String> COMMAND_PREFIX_FIX = Set.of(
    "lp ",
    "luckperms ",
    "give "
  );

  // =====================================================
  // =================== DESERIALIZE =====================
  // =====================================================

  @Override
  public Reward deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
    throws JsonParseException {

    if (json == null || !json.isJsonObject()) {
      throw new JsonParseException("Reward must be a JsonObject");
    }

    JsonObject obj = json.getAsJsonObject();

    // ===============================
    // REWARD VALUE
    // ===============================

    String rewardValue = null;

    if (obj.has("reward") && !obj.get("reward").isJsonNull()) {
      rewardValue = obj.get("reward").getAsString();
    } else if (obj.has("item") && !obj.get("item").isJsonNull()) {
      rewardValue = obj.get("item").getAsString();
    }

    if (rewardValue == null || rewardValue.isBlank()) {
      rewardValue = "item:1:minecraft:stone";
    }

    String[] parts = rewardValue.split("\\|");

    for (int i = 0; i < parts.length; i++) {

      String part = parts[i].trim();
      if (part.isEmpty()) continue;

      String type = part.contains(":")
        ? part.substring(0, part.indexOf(":"))
        : part;

      if (RewardRegistry.getRewardExecutor(type) == null) {

        String lower = part.toLowerCase();

        boolean isCommandLike = COMMAND_PREFIX_FIX.stream()
          .anyMatch(lower::startsWith);

        if (isCommandLike) {
          parts[i] = "command:" + part;

          CobbleUtils.LOGGER.warn(
            "RewardAdapter",
            "Auto-fixed command reward → command:" + part
          );
        } else {
          parts[i] = "item:1:" + part;
          
          CobbleUtils.LOGGER.warn(
            "RewardAdapter",
            "Auto-fixed invalid reward → item:1:" + part
          );
        }
      }
    }

    rewardValue = String.join("|", parts);

    // ===============================
    // WEIGHT / CHANCE
    // ===============================

    double weight = 1.0;

    if (obj.has("weight") && !obj.get("weight").isJsonNull()) {
      weight = obj.get("weight").getAsDouble();
    } else if (obj.has("chance") && !obj.get("chance").isJsonNull()) {
      weight = obj.get("chance").getAsDouble();
    }

    // ===============================
    // OTHER FIELDS
    // ===============================

    Boolean unique = obj.has("unique") && !obj.get("unique").isJsonNull()
      && obj.get("unique").getAsBoolean();

    Integer amount = obj.has("amount") && !obj.get("amount").isJsonNull()
      ? obj.get("amount").getAsInt()
      : null;

    String identifier = obj.has("identifier") && !obj.get("identifier").isJsonNull()
      ? obj.get("identifier").getAsString()
      : null;

    DurationValue cooldown = obj.has("cooldown") && !obj.get("cooldown").isJsonNull()
      ? DurationValue.parse(obj.get("cooldown").getAsString())
      : null;

    String display = obj.has("display") && !obj.get("display").isJsonNull()
      ? obj.get("display").getAsString()
      : null;

    String displayname = obj.has("displayname") && !obj.get("displayname").isJsonNull()
      ? obj.get("displayname").getAsString()
      : null;

    // ===============================
    // BUILD
    // ===============================

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

  // =====================================================
  // =================== SERIALIZE =======================
  // =====================================================

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