package com.kingpixel.cobbleutils.Model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kingpixel.cobbleutils.util.ItemUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * @author Carlos Varas Alonso - 29/06/2024 0:54
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class ItemObject {
  private String item;

  public ItemObject(String item) {
    this.item = item;
  }

  // Crea un ItemObject a partir de un ItemStack, guardando el NBT como String
  public static String fromItemStack(ItemStack itemStack) {
    var jsonObject = new JsonObject();
    jsonObject.addProperty("itemId", Registries.ITEM.getId(itemStack.getItem()).toString());
    jsonObject.addProperty("amount", itemStack.getCount());

    if (itemStack.get(DataComponentTypes.CUSTOM_NAME) != null) {
      jsonObject.addProperty("displayName", itemStack.getName().getString());
    }
    // Save NBT data
    if (itemStack.get(DataComponentTypes.CUSTOM_DATA) != null) {
      NbtComponent nbtComponent = itemStack.get(DataComponentTypes.CUSTOM_DATA);
      if (nbtComponent != null) {
        NbtCompound nbtCompound = nbtComponent.copyNbt();
        if (nbtCompound != null) {
          jsonObject.addProperty("nbt", NbtHelper.toNbtProviderString(nbtCompound));
        }
      }
    }

    return jsonObject.toString();
  }

  public ItemStack toItemStack() {
    return toItemStack(item);
  }

  // Crea un ItemStack a partir de un String
  public static ItemStack toItemStack(String itemString) {
    try {
      var jsonObject = JsonParser.parseString(itemString).getAsJsonObject();
      var itemId = jsonObject.get("itemId").getAsString();
      var amount = jsonObject.get("amount").getAsInt();
      var item = Registries.ITEM.get(Identifier.of(itemId));
      var itemStack = new ItemStack(item, amount);
      var jsonNbt = jsonObject.get("nbt");
      if (jsonNbt != null) {
        var nbt = jsonNbt.getAsString();
        if (nbt != null && !nbt.isEmpty()) {
          itemStack = ItemUtils.applyNbt(itemStack, nbt, itemStack.getCount());
        }
      }
      return itemStack;
    } catch (Exception e) {
      e.printStackTrace();
      return ItemStack.EMPTY;
    }
  }

  public static ItemObject createItemObject(ItemStack newItemStack) {
    return new ItemObject(fromItemStack(newItemStack));
  }
}