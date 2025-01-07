package com.kingpixel.cobbleutils.features.shops;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.features.shops.models.Product;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.EconomyUtil;
import com.kingpixel.cobbleutils.util.LuckPermsUtil;
import com.kingpixel.cobbleutils.util.PlayerUtils;
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
  // Actual: Currency -> ShopId -> Products Better ? ShopId -> Currency -> products
  private static final Map<String, Map<String, Set<Product>>> products = new ConcurrentHashMap<>();

  private ShopSell() {
    // Utility class
  }

  /**
   * Adds products from the shop to the global product list, updating existing products.
   *
   * @param shop The shop whose products are to be added.
   */
  public static void addProduct(Shop shop, ShopConfigMenu shopConfigMenu) {
    if (!shop.isActive()) {
      products.get(shop.getCurrency()).remove(shop.getId());
      return;
    }
    Set<Product> productSet = new HashSet<>();
    BigDecimal highPrice = BigDecimal.valueOf(999999999);
    if (shopConfigMenu.getPricesHigh() != null) {
      shopConfigMenu.getPricesHigh().getOrDefault(shop.getId(), BigDecimal.valueOf(999999999));
    }
    shop.getProducts().forEach(product -> {
      if (product.getSell().compareTo(highPrice) > 0) {
        CobbleUtils.LOGGER.fatal("Product " + product.getProduct() + " in shop " + shop.getId() + " has a sell price higher than the maximum price.");
        return;
      }
      String productName = product.getProduct();
      boolean isValidType = !(productName.startsWith("pokemon:") ||
        productName.startsWith("command:") ||
        productName.startsWith("money:"));
      boolean hasPositiveSellPrice = product.getSell().compareTo(BigDecimal.ZERO) > 0;

      if (isValidType && hasPositiveSellPrice) {
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
              if (inventoryItem.isEmpty()) continue;
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
    return ItemStack.areItemsAndComponentsEqual(inventoryItem, productStack);
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
