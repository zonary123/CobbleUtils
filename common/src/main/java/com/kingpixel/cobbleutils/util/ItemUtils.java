package com.kingpixel.cobbleutils.util;

import com.google.gson.JsonObject;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.RegistryOps;

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
    ItemStack itemStack = Utils.parseItemId(item);
    return getTranslatedName(itemStack);
  }

  public static String getNameItem(ItemStack itemStack) {
    return getTranslatedName(itemStack);
  }

  public static String getTranslatedName(ItemStack itemStack) {
    if (itemStack.getItem() == Items.AIR) return CobbleUtils.language.getUnknown();
    if (itemStack.get(DataComponentTypes.CUSTOM_NAME) != null) return itemStack.getName().getString();
    return "<lang:" + itemStack.getItem().getTranslationKey() + ">";
  }
}
