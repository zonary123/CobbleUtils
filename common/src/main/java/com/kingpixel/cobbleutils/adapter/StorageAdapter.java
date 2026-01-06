package com.kingpixel.cobbleutils.adapter;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.*;
import com.kingpixel.cobbleutils.database.users.models.Storage;
import com.kingpixel.cobbleutils.database.users.models.StorageItemStack;
import com.kingpixel.cobbleutils.database.users.models.StoragePokemon;
import com.kingpixel.cobbleutils.database.users.models.StorageRewards;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Type;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 06/10/2025 5:26
 */
public class StorageAdapter implements JsonSerializer<Storage>, JsonDeserializer<Storage> {
  public static final StorageAdapter INSTANCE = new StorageAdapter();

  @Override
  public Storage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    JsonObject jsonObject = json.getAsJsonObject();

    String type = jsonObject.has("type") ? jsonObject.get("type").getAsString() : null;
    if (type == null) {
      throw new JsonParseException("El campo 'type' no existe en el JSON: " + jsonObject);
    }

    UUID id = jsonObject.has("id") ? UUID.fromString(jsonObject.get("id").getAsString()) : null;
    if (id == null) {
      throw new JsonParseException("El campo 'id' no existe en el JSON: " + jsonObject);
    }

    return switch (type) {
      case "pokemon" -> {
        JsonElement pokemonElement = jsonObject.get("pokemon");
        if (pokemonElement == null || !pokemonElement.isJsonObject()) {
          throw new JsonParseException("Falta el campo 'pokemon' para StoragePokemon: " + jsonObject);
        }
        var pokemon = PokemonAdapter.INSTANCE.deserialize(pokemonElement.getAsJsonObject(),
          Pokemon.class, context);
        yield new StoragePokemon(id, pokemon);
      }
      case "itemStack" -> {
        JsonElement itemStackElement = jsonObject.get("itemStack");
        if (itemStackElement == null || !itemStackElement.isJsonObject()) {
          throw new JsonParseException("Falta el campo 'itemStack' para StorageItemStack: " + jsonObject);
        }
        ItemStack itemStack = ItemStackAdapter.INSTANCE.deserialize(itemStackElement.getAsJsonObject(), ItemStack.class, context);
        yield new StorageItemStack(id, itemStack);
      }
      case "reward" -> {
        JsonElement rewardElement = jsonObject.get("reward");
        if (rewardElement == null || !rewardElement.isJsonObject()) {
          throw new JsonParseException("Falta el campo 'reward' para StorageRewards: " + jsonObject);
        }
        var itemChance = ItemChanceAdapter.INSTANCE.deserialize(rewardElement.getAsJsonObject(), null, context);
        yield new StorageRewards(id, itemChance);
      }
      default -> throw new IllegalStateException("Tipo inesperado: " + type);
    };
  }


  @Override
  public JsonElement serialize(Storage src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject jsonObject = new JsonObject();
    UUID id = src.getId();
    switch (src) {
      case StoragePokemon storagePokemon -> {
        jsonObject.addProperty("id", id.toString());
        jsonObject.addProperty("type", "pokemon");
        jsonObject.add("pokemon", PokemonAdapter.INSTANCE.serialize(storagePokemon.getPokemon(), Pokemon.class, null));
      }
      case StorageItemStack storageItemStack -> {
        jsonObject.addProperty("id", id.toString());
        jsonObject.addProperty("type", "itemStack");
        jsonObject.add("itemStack", ItemStackAdapter.INSTANCE.serialize(storageItemStack.getItemStack(), ItemStack.class, null));
      }
      case StorageRewards storageRewards -> {
        jsonObject.addProperty("id", id.toString());
        jsonObject.addProperty("type", "reward");
        jsonObject.add("reward", ItemChanceAdapter.INSTANCE.serialize(storageRewards.getReward(), null, null));
      }
      default -> throw new IllegalStateException("Unexpected value: " + src.getClass().getName());
    }
    return jsonObject;
  }
}
