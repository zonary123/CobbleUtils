package com.kingpixel.cobbleutils.adapter;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.*;
import com.kingpixel.cobbleutils.database.users.models.Storage;
import com.kingpixel.cobbleutils.database.users.models.StorageItemStack;
import com.kingpixel.cobbleutils.database.users.models.StoragePokemon;
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
    String type = jsonObject.get("type").getAsString();
    UUID id = UUID.fromString(jsonObject.get("id").getAsString());
    return switch (type) {
      case "pokemon" -> {
        var pokemon = PokemonAdapter.INSTANCE.deserialize(jsonObject.get("pokemon").getAsJsonObject(),
          Pokemon.class, null);
        yield new StoragePokemon(id, pokemon);
      }
      case "itemStack" -> {
        ItemStack itemStack = ItemStackAdapter.INSTANCE.deserialize(jsonObject.get("itemStack").getAsJsonObject(), ItemStack.class, null);
        yield new StorageItemStack(id, itemStack);
      }
      default -> throw new IllegalStateException("Unexpected value: " + type);
    };
  }

  @Override public JsonElement serialize(Storage src, Type typeOfSrc, JsonSerializationContext context) {
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
      default -> throw new IllegalStateException("Unexpected value: " + src.getClass().getName());
    }
    return jsonObject;
  }
}
