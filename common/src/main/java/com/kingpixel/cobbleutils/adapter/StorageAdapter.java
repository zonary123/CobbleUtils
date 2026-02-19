package com.kingpixel.cobbleutils.adapter;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.*;
import com.kingpixel.cobbleutils.Model.rewards.Reward;
import com.kingpixel.cobbleutils.database.users.models.Storage;
import com.kingpixel.cobbleutils.database.users.models.StorageItemStack;
import com.kingpixel.cobbleutils.database.users.models.StoragePokemon;
import com.kingpixel.cobbleutils.database.users.models.StorageRewards;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * StorageAdapter with strict auditing and validation
 */
public class StorageAdapter implements JsonSerializer<Storage>, JsonDeserializer<Storage> {

  public static final StorageAdapter INSTANCE = new StorageAdapter();

  private static final Set<String> VALID_TYPES = Set.of(
    "pokemon",
    "itemStack",
    "reward"
  );

  @Override
  public Storage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
    throws JsonParseException {

    // ===============================
    // LEVEL 0 VALIDATION (BASE JSON)
    // ===============================

    if (json == null) {
      throw new JsonParseException("Storage JSON element is null");
    }

    if (!json.isJsonObject()) {
      throw new JsonParseException("Storage must be a JsonObject, found: " + json);
    }

    JsonObject obj = json.getAsJsonObject();

    if (obj.entrySet().isEmpty()) {
      throw new JsonParseException("Storage JSON object is empty: {}");
    }

    // ===============================
    // LEVEL 1 VALIDATION (BASE FIELDS)
    // ===============================

    assertFieldExists(obj, "type");
    assertFieldExists(obj, "id");

    String type = assertString(obj, "type");
    String idRaw = assertString(obj, "id");

    if (!VALID_TYPES.contains(type)) {
      throw new JsonParseException(
        "Invalid Storage type '" + type + "'. Allowed values: " + VALID_TYPES + " → " + obj
      );
    }

    UUID id;
    try {
      id = UUID.fromString(idRaw);
    } catch (Exception e) {
      throw new JsonParseException(
        "Invalid UUID format for field 'id': '" + idRaw + "' → " + obj,
        e
      );
    }

    // ===============================
    // LEVEL 2 VALIDATION (UNKNOWN FIELDS)
    // ===============================

    validateNoUnknownFields(obj, type);

    // ===============================
    // LEVEL 3 VALIDATION (BY TYPE)
    // ===============================

    return switch (type) {

      case "pokemon" -> {
        assertFieldExists(obj, "pokemon");
        JsonObject pokemonObj = assertObject(obj, "pokemon");

        Pokemon pokemon;
        try {
          pokemon = PokemonAdapter.INSTANCE.deserialize(
            pokemonObj,
            Pokemon.class,
            context
          );
        } catch (Exception e) {
          throw new JsonParseException(
            "Failed to deserialize Pokemon object → " + pokemonObj,
            e
          );
        }

        if (pokemon == null) {
          throw new JsonParseException(
            "Deserialized Pokemon is null → " + obj
          );
        }

        yield new StoragePokemon(id, pokemon);
      }

      case "itemStack" -> {
        assertFieldExists(obj, "itemStack");
        JsonObject itemStackObj = assertObject(obj, "itemStack");

        ItemStack stack;
        try {
          stack = ItemStackAdapter.INSTANCE.deserialize(
            itemStackObj,
            ItemStack.class,
            context
          );
        } catch (Exception e) {
          throw new JsonParseException(
            "Failed to deserialize ItemStack → " + itemStackObj,
            e
          );
        }

        if (stack == null || stack.isEmpty()) {
          throw new JsonParseException(
            "ItemStack is null or empty → " + obj
          );
        }

        yield new StorageItemStack(id, stack);
      }

      case "reward" -> {
        assertFieldExists(obj, "reward");
        JsonObject rewardObj = assertObject(obj, "reward");

        Reward reward;
        try {
          reward = RewardAdapter.INSTANCE.deserialize(
            rewardObj,
            null,
            context
          );
        } catch (Exception e) {
          throw new JsonParseException(
            "Failed to deserialize Reward object → " + rewardObj,
            e
          );
        }

        if (reward == null) {
          throw new JsonParseException(
            "Reward object is null → " + obj
          );
        }

        yield new StorageRewards(id, reward);
      }

      default -> throw new AssertionError(
        "Unreachable code: unsupported Storage type"
      );
    };
  }

  // =====================================================
  // =================== SERIALIZATION ===================
  // =====================================================

  @Override
  public JsonElement serialize(Storage src, Type typeOfSrc, JsonSerializationContext context) {

    if (src == null) {
      throw new IllegalStateException(
        "Attempted to serialize a null Storage instance"
      );
    }

    if (src.getId() == null) {
      throw new IllegalStateException(
        "Storage instance has no ID: " + src.getClass().getName()
      );
    }

    JsonObject obj = new JsonObject();
    obj.addProperty("id", src.getId().toString());

    switch (src) {

      case StoragePokemon p -> {
        if (p.getPokemon() == null) {
          throw new IllegalStateException(
            "StoragePokemon contains a null Pokemon"
          );
        }
        obj.addProperty("type", "pokemon");
        obj.add("pokemon",
          PokemonAdapter.INSTANCE.serialize(
            p.getPokemon(),
            Pokemon.class,
            null
          )
        );
      }

      case StorageItemStack s -> {
        if (s.getItemStack() == null || s.getItemStack().isEmpty()) {
          throw new IllegalStateException(
            "StorageItemStack contains a null or empty ItemStack"
          );
        }
        obj.addProperty("type", "itemStack");
        obj.add("itemStack",
          ItemStackAdapter.INSTANCE.serialize(
            s.getItemStack(),
            ItemStack.class,
            null
          )
        );
      }

      case StorageRewards r -> {
        if (r.getReward() == null) {
          throw new IllegalStateException(
            "StorageRewards contains a null reward"
          );
        }
        obj.addProperty("type", "reward");
        obj.add("reward",
          RewardAdapter.INSTANCE.serialize(
            r.getReward(),
            null,
            null
          )
        );
      }

      default -> throw new IllegalStateException(
        "Unsupported Storage implementation: " + src.getClass().getName()
      );
    }

    return obj;
  }

  // =====================================================
  // =================== HELPERS =========================
  // =====================================================

  private static void assertFieldExists(JsonObject obj, String field) {
    if (!obj.has(field)) {
      throw new JsonParseException(
        "Missing required field '" + field + "' → " + obj
      );
    }
    if (obj.get(field).isJsonNull()) {
      throw new JsonParseException(
        "Field '" + field + "' must not be null → " + obj
      );
    }
  }

  private static String assertString(JsonObject obj, String field) {
    JsonElement e = obj.get(field);
    if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) {
      throw new JsonParseException(
        "Field '" + field + "' must be a String → " + obj
      );
    }
    return e.getAsString();
  }

  private static JsonObject assertObject(JsonObject obj, String field) {
    JsonElement e = obj.get(field);
    if (!e.isJsonObject()) {
      throw new JsonParseException(
        "Field '" + field + "' must be a JsonObject → " + obj
      );
    }
    return e.getAsJsonObject();
  }

  private static void validateNoUnknownFields(JsonObject obj, String type) {
    Set<String> allowed = switch (type) {
      case "pokemon" -> Set.of("id", "type", "pokemon");
      case "itemStack" -> Set.of("id", "type", "itemStack");
      case "reward" -> Set.of("id", "type", "reward");
      default -> Set.of();
    };

    for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
      if (!allowed.contains(entry.getKey())) {
        throw new JsonParseException(
          "Unknown field '" + entry.getKey() + "' is not allowed → " + obj
        );
      }
    }
  }
}
