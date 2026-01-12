package com.kingpixel.cobbleutils.adapter;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.*;
import com.kingpixel.cobbleutils.Model.ItemChance;
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
 * StorageAdapter en modo auditoría extrema
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
    // VALIDACIÓN NIVEL 0 (JSON BASE)
    // ===============================

    if (json == null) {
      throw new JsonParseException("Storage es null");
    }

    if (!json.isJsonObject()) {
      throw new JsonParseException("Storage no es JsonObject: " + json);
    }

    JsonObject obj = json.getAsJsonObject();

    if (obj.entrySet().isEmpty()) {
      throw new JsonParseException("Storage vacío: {}");
    }

    // ===============================
    // VALIDACIÓN NIVEL 1 (CAMPOS BASE)
    // ===============================

    assertFieldExists(obj, "type");
    assertFieldExists(obj, "id");

    String type = assertString(obj, "type");
    String idRaw = assertString(obj, "id");

    if (!VALID_TYPES.contains(type)) {
      throw new JsonParseException("Tipo de Storage inválido: '" + type + "' → " + obj);
    }

    UUID id;
    try {
      id = UUID.fromString(idRaw);
    } catch (Exception e) {
      throw new JsonParseException("UUID inválido: '" + idRaw + "' → " + obj, e);
    }

    // ===============================
    // VALIDACIÓN NIVEL 2 (CAMPOS SOBRANTES)
    // ===============================

    validateNoUnknownFields(obj, type);

    // ===============================
    // VALIDACIÓN NIVEL 3 (POR TIPO)
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
            "Error deserializando Pokemon → " + pokemonObj, e
          );
        }

        if (pokemon == null) {
          throw new JsonParseException("Pokemon deserializado es null → " + obj);
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
            "Error deserializando ItemStack → " + itemStackObj, e
          );
        }

        if (stack == null || stack.isEmpty()) {
          throw new JsonParseException(
            "ItemStack vacío o null → " + obj
          );
        }

        yield new StorageItemStack(id, stack);
      }

      case "reward" -> {
        assertFieldExists(obj, "reward");
        JsonObject rewardObj = assertObject(obj, "reward");

        ItemChance reward;
        try {
          reward = ItemChanceAdapter.INSTANCE.deserialize(
            rewardObj,
            null,
            context
          );
        } catch (Exception e) {
          throw new JsonParseException(
            "Error deserializando Reward → " + rewardObj, e
          );
        }

        if (reward == null) {
          throw new JsonParseException("Reward es null → " + obj);
        }

        yield new StorageRewards(id, reward);
      }

      default -> throw new AssertionError("Nunca debería llegar aquí");
    };
  }

  // =====================================================
  // =================== SERIALIZE =======================
  // =====================================================

  @Override
  public JsonElement serialize(Storage src, Type typeOfSrc, JsonSerializationContext context) {

    if (src == null) {
      throw new IllegalStateException("Intento de serializar Storage null");
    }

    if (src.getId() == null) {
      throw new IllegalStateException("Storage sin ID: " + src.getClass().getName());
    }

    JsonObject obj = new JsonObject();
    obj.addProperty("id", src.getId().toString());

    switch (src) {

      case StoragePokemon p -> {
        if (p.getPokemon() == null) {
          throw new IllegalStateException("StoragePokemon sin Pokemon");
        }
        obj.addProperty("type", "pokemon");
        obj.add("pokemon",
          PokemonAdapter.INSTANCE.serialize(
            p.getPokemon(),
            Pokemon.class,
            null
          ));
      }

      case StorageItemStack s -> {
        if (s.getItemStack() == null || s.getItemStack().isEmpty()) {
          throw new IllegalStateException("StorageItemStack vacío");
        }
        obj.addProperty("type", "itemStack");
        obj.add("itemStack",
          ItemStackAdapter.INSTANCE.serialize(
            s.getItemStack(),
            ItemStack.class,
            null
          ));
      }

      case StorageRewards r -> {
        if (r.getReward() == null) {
          throw new IllegalStateException("StorageRewards sin reward");
        }
        obj.addProperty("type", "reward");
        obj.add("reward",
          ItemChanceAdapter.INSTANCE.serialize(
            r.getReward(),
            null,
            null
          ));
      }

      default -> throw new IllegalStateException(
        "Storage no soportado: " + src.getClass().getName()
      );
    }

    return obj;
  }

  // =====================================================
  // =================== HELPERS =========================
  // =====================================================

  private static void assertFieldExists(JsonObject obj, String field) {
    if (!obj.has(field)) {
      throw new JsonParseException("Falta campo obligatorio '" + field + "' → " + obj);
    }
    if (obj.get(field).isJsonNull()) {
      throw new JsonParseException("Campo '" + field + "' es null → " + obj);
    }
  }

  private static String assertString(JsonObject obj, String field) {
    JsonElement e = obj.get(field);
    if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) {
      throw new JsonParseException(
        "Campo '" + field + "' no es String → " + obj
      );
    }
    return e.getAsString();
  }

  private static JsonObject assertObject(JsonObject obj, String field) {
    JsonElement e = obj.get(field);
    if (!e.isJsonObject()) {
      throw new JsonParseException(
        "Campo '" + field + "' no es JsonObject → " + obj
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
          "Campo desconocido '" + entry.getKey() + "' → " + obj
        );
      }
    }
  }
}
