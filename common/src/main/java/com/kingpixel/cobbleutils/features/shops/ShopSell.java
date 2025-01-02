package com.kingpixel.cobbleutils.features.shops;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.features.shops.models.Product;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.EconomyUtil;
import com.kingpixel.cobbleutils.util.LuckPermsUtil;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles selling products in the shop.
 */
public class ShopSell {
  private static final Map<String, Map<String, Set<Product>>> products = new ConcurrentHashMap<>();

  private ShopSell() {
    // Utility class
  }

  /**
   * Adds products from the shop to the global product list, updating existing products.
   *
   * @param shop The shop whose products are to be added.
   */
  public static void addProduct(Shop shop) {
    Set<Product> productSet = new HashSet<>();
    shop.getProducts().forEach(product -> {
      String s = product.getProduct();
      if (!s.startsWith("pokemon:")
        && !s.startsWith("money:")
        && !s.startsWith("command:")) {
        productSet.add(product);
      }
    });
    if (!productSet.isEmpty()) {
      products.computeIfAbsent(shop.getCurrency(), k -> new ConcurrentHashMap<>())
        .put(shop.getId(), productSet);
    }
  }

  /**
   * Sells all products from the player's inventory and updates the player's balance.
   *
   * @param player         The player who is selling the products.
   * @param shopConfigMenu
   */
  public static void sellProducts(ServerPlayerEntity player, ShopConfigMenu shopConfigMenu) {
    if (CobbleUtils.config.isDebug()) {
      Set<Product> products = new HashSet<>();
      for (Map<String, Set<Product>> value : ShopSell.products.values()) {
        for (Set<Product> productSet : value.values()) {
          productSet.forEach(product -> {
            String s = product.getProduct();
            if (!s.startsWith("pokemon:")
              && !s.startsWith("money:")
              && !s.startsWith("command:")) {
              products.add(product);
            }
          });
        }
      }
      products.forEach(product -> {
        CobbleUtils.LOGGER.info("Product: " + product);
        ItemStack itemStack = product.getItemchance().getItemStack();
        CobbleUtils.LOGGER.info("ItemStack: " + itemStack);
        CobbleUtils.LOGGER.info("CustomModelData: " + itemStack.get(DataComponentTypes.CUSTOM_MODEL_DATA));
      });
    }
    try {
      PlayerInventory inventory = player.getInventory();
      Map<String, BigDecimal> currencyTotals = new HashMap<>();

      products.forEach((currency, shopMap) -> {
        BigDecimal currencyTotal = BigDecimal.ZERO;
        int decimals = EconomyUtil.getDecimals(currency);

        for (Set<Product> productSet : shopMap.values()) {
          for (Product product : productSet) {
            if (!isProductSellable(player, product)) continue;

            BigDecimal sellPrice = product.getSell().setScale(decimals, RoundingMode.HALF_UP);
            ItemStack productStack = product.getItemchance().getItemStack();

            for (ItemStack inventoryItem : inventory.main) {
              if (!isMatchingItem(inventoryItem, productStack)) continue;

              int amount = inventoryItem.getCount();
              currencyTotal = currencyTotal.add(sellPrice.multiply(BigDecimal.valueOf(amount)));
              inventoryItem.decrement(amount);
            }
          }
        }

        if (currencyTotal.compareTo(BigDecimal.ZERO) > 0) {
          currencyTotals.put(currency, currencyTotal.setScale(decimals, RoundingMode.HALF_UP));
        }
      });

      distributeEarnings(player, currencyTotals);
    } catch (Exception e) {
      CobbleUtils.LOGGER.error("Error selling products: " + e);
    }
  }

  /**
   * Sells the product currently held in the player's main hand.
   *
   * @param player         The player who is selling the product.
   * @param shopConfigMenu
   */
  public static void sellProductHand(ServerPlayerEntity player, ShopConfigMenu shopConfigMenu) {
    PlayerInventory inventory = player.getInventory();
    ItemStack mainHandStack = inventory.getMainHandStack();

    if (mainHandStack.isEmpty()) {
      sendPlayerMessage(player, CobbleUtils.shopLang.getMessageSellHandNoItem());
      return;
    }

    for (Map.Entry<String, Map<String, Set<Product>>> currencyEntry : products.entrySet()) {
      String currency = currencyEntry.getKey();
      int decimals = EconomyUtil.getDecimals(currency);

      for (Set<Product> productSet : currencyEntry.getValue().values()) {
        for (Product product : productSet) {
          if (!isProductSellable(player, product)) continue;

          ItemStack productStack = product.getItemchance().getItemStack();
          if (!isMatchingItem(mainHandStack, productStack)) continue;

          BigDecimal sellPrice = product.getSell().setScale(decimals, RoundingMode.HALF_UP);
          BigDecimal totalEarned = sellPrice.multiply(BigDecimal.valueOf(mainHandStack.getCount()));

          EconomyUtil.addMoney(player, currency, totalEarned, false);
          mainHandStack.decrement(mainHandStack.getCount());

          sendSellHandSuccess(player, currency, totalEarned);
          return;
        }
      }
    }

    sendPlayerMessage(player, CobbleUtils.shopLang.getMessageSellHandNoItemPrice());
  }

  private static boolean isProductSellable(ServerPlayerEntity player, Product product) {
    return product.getPermission() == null || LuckPermsUtil.checkPermission(player, product.getPermission());
  }

  private static boolean isMatchingItem(ItemStack inventoryItem, ItemStack productStack) {
    boolean areItemsEqual = ItemStack.areItemsAndComponentsEqual(inventoryItem, productStack);
    boolean areCustomModelDataEqual = inventoryItem.get(DataComponentTypes.CUSTOM_MODEL_DATA) == productStack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
    if (CobbleUtils.config.isDebug()) {
      CobbleUtils.LOGGER.info("--------------------");
      CobbleUtils.LOGGER.info("InventoryItem: " + inventoryItem);
      CobbleUtils.LOGGER.info("ProductStack: " + productStack);
      CobbleUtils.LOGGER.info("areItemsEqual: " + areItemsEqual);
      CobbleUtils.LOGGER.info("areCustomModelDataEqual: " + areCustomModelDataEqual);
      CobbleUtils.LOGGER.info("--------------------");
    }
    return areItemsEqual && areCustomModelDataEqual;
  }

  private static void distributeEarnings(ServerPlayerEntity player, Map<String, BigDecimal> currencyTotals) {
    if (currencyTotals.isEmpty()) return;

    StringBuilder message = new StringBuilder(CobbleUtils.shopLang.getMessageSell());
    StringBuilder currencyMessage = new StringBuilder();

    currencyTotals.forEach((currency, total) -> {
      currencyMessage.append(String.format("\n &6%s &a%s,", EconomyUtil.formatCurrency(total, currency, player.getUuid()), currency));
      EconomyUtil.addMoney(player, currency, total, false);
    });

    message = new StringBuilder(message.toString()
      .replace("%currencys%", currencyMessage.toString().replaceAll(",\n$", ""))
      .replace("%prefix%", CobbleUtils.shopLang.getPrefix()));

    PlayerUtils.sendMessage(player, message.toString(), CobbleUtils.shopLang.getPrefix());
  }

  private static void sendPlayerMessage(ServerPlayerEntity player, String message) {
    player.sendMessage(AdventureTranslator.toNative(message.replace("%prefix%", CobbleUtils.shopLang.getPrefix())));
  }

  private static void sendSellHandSuccess(ServerPlayerEntity player, String currency, BigDecimal totalEarned) {
    PlayerUtils.sendMessage(player, CobbleUtils.shopLang.getMessageSellHand()
      .replace("%prefix%", CobbleUtils.shopLang.getPrefix())
      .replace("%balance%", EconomyUtil.formatCurrency(totalEarned, currency, player.getUuid()))
      .replace("%currency%", currency), CobbleUtils.shopLang.getPrefix());
  }
}
