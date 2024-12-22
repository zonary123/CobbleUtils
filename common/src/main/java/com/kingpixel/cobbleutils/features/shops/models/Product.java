package com.kingpixel.cobbleutils.features.shops.models;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import lombok.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemStack;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 16/09/2024 18:43
 */
@Getter
@Setter
@EqualsAndHashCode
@Data
@ToString
public class Product {
  // Optional
  private Boolean notCanBuyWithPermission;
  private String permission;
  private String color;
  private String display;
  private String displayname;
  private List<String> lore;
  private Long CustomModelData;
  private Integer discount;
  // Always have date
  private String product;
  private BigDecimal buy;
  private BigDecimal sell;

  public Product() {
    this.notCanBuyWithPermission = null;
    this.display = null;
    this.color = null;
    this.displayname = null;
    this.lore = null;
    this.CustomModelData = null;
    this.permission = null;
    this.discount = null;
    this.product = "minecraft:stone";
    this.buy = BigDecimal.valueOf(500000);
    this.sell = BigDecimal.ZERO;
  }

  public Product(boolean optional) {
    if (optional) {
      this.notCanBuyWithPermission = true;
      this.display = "minecraft:dirt";
      this.color = "<#e7af76>";
      this.displayname = "Custom Dirt";
      this.lore = List.of("This is a custom dirt", "You can use it to build");
      this.CustomModelData = 1L;
      this.permission = "cobbleutils.dirt";
      this.discount = 10;
    } else {
      this.notCanBuyWithPermission = null;
      this.display = null;
      this.color = null;
      this.displayname = null;
      this.lore = null;
      this.CustomModelData = null;
      this.permission = null;
      this.discount = null;
    }
    this.product = "minecraft:stone";
    this.buy = BigDecimal.valueOf(500000);
    this.sell = BigDecimal.ZERO;
  }

  public Product(ItemStack defaultStack) {
    this.notCanBuyWithPermission = null;
    this.display = null;
    this.color = null;
    this.displayname = null;
    this.lore = null;
    this.CustomModelData = null;
    this.permission = null;
    this.discount = null;
    this.product = defaultStack.getItem().getTranslationKey()
      .replace("item.", "")
      .replace("block.", "")
      .replace(".", ":");
    this.buy = BigDecimal.valueOf(500000);
    this.sell = BigDecimal.ZERO;
  }


  public ItemChance getItemchance() {
    return new ItemChance(product, 100);
  }

  public ItemStack getItemStack(int amount) {
    ItemStack itemStack = getItemchance().getItemStack();

    if (getDisplay() != null && !getDisplay().isEmpty()) {
      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER.info("Display: " + getDisplay());
      }
      itemStack = new ItemChance(getDisplay(), 100).getItemStack();
    }

    itemStack.setCount(amount);

    if (getDisplayname() != null && !getDisplayname().isEmpty()) {
      itemStack.set(DataComponentTypes.ITEM_NAME, AdventureTranslator.toNative(getDisplayname()));
    }

    if (getLore() != null && !getLore().isEmpty()) {

    }

    if (getCustomModelData() != null && getCustomModelData() > 0 && itemStack.get(DataComponentTypes.CUSTOM_MODEL_DATA) != null) {
      itemStack.set(DataComponentTypes.CUSTOM_MODEL_DATA,
        new CustomModelDataComponent(Math.toIntExact(getCustomModelData())));
    }

    return itemStack;
  }

  public ItemStack getItemStack() {
    return getItemStack(1);
  }

  public ItemStack getItemStack(int amount, boolean setAmount) {
    ItemStack itemStack = getItemchance().getItemStack();

    if (getDisplay() != null && !getDisplay().isEmpty()) {
      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER.info("Display: " + getDisplay());
      }
      itemStack = new ItemChance(getDisplay(), 100).getItemStack();
    }

    if (setAmount) {
      itemStack.setCount(amount);
    }

    if (getDisplayname() != null && !getDisplayname().isEmpty()) {
      itemStack.set(DataComponentTypes.ITEM_NAME, AdventureTranslator.toNative(getDisplayname()));
    }

    if (getLore() != null && !getLore().isEmpty()) {

    }

    if (getCustomModelData() != null && getCustomModelData() > 0 && itemStack.get(DataComponentTypes.CUSTOM_MODEL_DATA) != null) {
      itemStack.set(DataComponentTypes.CUSTOM_MODEL_DATA,
        new CustomModelDataComponent(Math.toIntExact(getCustomModelData())));
    }

    return itemStack;
  }
}
