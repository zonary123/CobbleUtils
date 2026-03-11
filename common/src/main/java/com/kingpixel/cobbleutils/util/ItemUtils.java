package com.kingpixel.cobbleutils.util;

import com.google.gson.JsonObject;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.model.ItemModel;
import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryOps;
import net.minecraft.util.Identifier;

/**
 * @author Carlos Varas Alonso - 04/07/2024 4:05
 */
public class ItemUtils {
  private static RegistryOps<NbtElement> nbtOps;

  private static RegistryOps<NbtElement> getNbt() {
    if (nbtOps == null)
      if (CobbleUtils.server != null) nbtOps = CobbleUtils.server.getRegistryManager().getOps(NbtOps.INSTANCE);
    return nbtOps;
  }

  public static ItemStack parseItemId(String id) {
    return parseItemId(id, 1);
  }

  public static ItemStack parseItemId(String id, int amount) {
    return new ItemStack(Registries.ITEM.get(Identifier.of(id)), amount);
  }

  public static ItemStack parseItemId(String item, int amount, long customModelData) {
    ItemStack itemStack = parseItemId(item, amount);
    if (customModelData != 0)
      itemStack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent((int) customModelData));
    return itemStack;
  }

  public static boolean equals(ItemStack itemStack, ItemStack otherStack) {
    return ItemStack.areItemsAndComponentsEqual(itemStack, otherStack);
  }

  public static String itemstackToString(ItemStack itemStack) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("itemId", itemStack.getRegistryEntry().getIdAsString());
    jsonObject.addProperty("amount", itemStack.getCount());
    if (itemStack.get(DataComponentTypes.CUSTOM_NAME) != null) {
      jsonObject.addProperty("displayName", itemStack.getName().getString());
    }
    if (itemStack.get(DataComponentTypes.CUSTOM_DATA) != null) {
      NbtCompound nbtCompound = itemStack.get(DataComponentTypes.CUSTOM_DATA).copyNbt();
      if (nbtCompound != null) {
        jsonObject.addProperty("nbt", nbtCompound.toString());
      }
    }
    return jsonObject.toString();
  }

  public static ItemStack applyNbt(String item, ItemStack itemStack, String nbt, int amount) {
    try {
      if (nbt != null && !nbt.isEmpty()) {
        boolean isLegacy = nbt.startsWith("{");

        if (isLegacy) {
          var parseNbt = StringNbtReader.parse(nbt);
          var legacyNbt = new NbtCompound();
          legacyNbt.putString("id", itemStack.getRegistryEntry().getIdAsString());
          legacyNbt.putInt("Count", amount);
          legacyNbt.put("tag", parseNbt);


          var updatedNbt = CobbleUtils.server.getDataFixer().update(
            TypeReferences.ITEM_STACK,
            new Dynamic<>(getNbt(), legacyNbt),
            3700,
            SharedConstants.getGameVersion().getSaveVersion().getId()
          ).getValue();

          itemStack = ItemStack.CODEC.parse(getNbt(), updatedNbt).result().orElse(ItemStack.EMPTY);
        } else {
          String result = item + nbt;
          if (CobbleUtils.config.isDebug()) {
            CobbleUtils.LOGGER.info("Item: " + item);
            CobbleUtils.LOGGER.info("NBT: " + nbt);
            CobbleUtils.LOGGER.info("Result: " + result);
          }
          itemStack = new ItemStackArgumentType(CobbleUtils.commandRegistryAccess)
            .parse(new StringReader(result))
            .createStack(amount, false);
        }

      }
    } catch (Exception e) {
      CobbleUtils.LOGGER.error("Error al aplicar NBT a un item: " + e.getMessage());
      return itemStack;
    }
    return itemStack;
  }

  public static String getNameItem(String item) {
    ItemStack itemStack = ItemUtils.parseItemId(item);
    return getTranslatedName(itemStack);
  }

  public static String getNameItem(ItemStack itemStack) {
    return getTranslatedName(itemStack);
  }

  public static String getTranslatedName(ItemStack itemStack) {
    if (itemStack.isEmpty()) return CobbleUtils.language.getUnknown();
    if (itemStack.get(DataComponentTypes.CUSTOM_NAME) != null) return itemStack.getName().getString();
    return "<lang:" + itemStack.getItem().getTranslationKey() + ">";
  }


  public static ItemStack parseItemModel(ItemModel itemModel, int amount) {
    String item = itemModel.getItem();
    String nbt = itemModel.getNbt();

    // Split item string to handle NBT if present
    String[] nbtSplit = item.split("#");
    if (nbtSplit.length > 1) {
      item = nbtSplit[0];
      nbt = nbtSplit[1];
    }

    // Handle custom item format
    if (item.startsWith("item:")) {
      item = item.replace("item:", "");
      String[] split = item.split(":");
      item = split[1] + ":" + split[2];
      amount = Integer.parseInt(split[0]);
    }

    // Parse the item ID and create the ItemStack
    ItemStack itemStack = parseItemId(item, amount);

    // Apply additional NBT and properties to the ItemStack
    itemStack = addThingsItemStack(itemStack, itemModel, nbt);

    return itemStack;
  }


  public static ItemStack addThingsItemStack(ItemStack itemStack, ItemModel itemModel, String nbt) {
    if (nbt != null && !nbt.isEmpty()) {
      String item = itemModel.getItem();
      String supportNbt = "";
      String[] split = item.split("#");
      if (split.length > 1) {
        item = split[0];
        supportNbt = split[1];
      }
      String[] splitItem = item.split(":");
      if (splitItem.length > 2) {
        item = splitItem[2] + ":" + splitItem[3];
      } else {
        item = splitItem[0] + ":" + splitItem[1];
      }
      itemStack = ItemUtils.applyNbt(item, itemStack, itemModel.getNbt() == null || itemModel.getNbt().isEmpty() ?
          supportNbt
          : nbt,
        itemStack.getCount());
    }

    itemStack.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNativeWithOutPrefix(
      itemModel.getDisplayname() != null ? itemModel.getDisplayname() : "Please set a displayname for this item"));

    if (itemModel.getCustomModelData() != 0)
      itemStack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent((int) itemModel.getCustomModelData()));

    if (itemModel.getLore() != null && !itemModel.getLore().isEmpty()) {
      itemStack.set(DataComponentTypes.LORE,
        new LoreComponent(AdventureTranslator.toNativeL(itemModel.getLore())));
    }
    return itemStack;
  }
}
