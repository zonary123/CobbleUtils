package com.kingpixel.cobbleutils.features.shops;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.features.shops.models.Product;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.EconomyUtil;
import com.kingpixel.cobbleutils.util.LuckPermsUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Carlos Varas Alonso - 25/08/2024 23:32
 */
@Getter
@Setter
@ToString
public class ShopSell {
  public static Map<String, Set<Product>> products = new HashMap<>();

  public ShopSell() {
  }

  public ShopSell(Map<String, Set<Product>> products) {
    ShopSell.products.clear();
    ShopSell.products.putAll(products);
  }

  /**
   * Adds products from the shop to the global product list, updating existing products.
   *
   * @param shop The shop whose products are to be added.
   */
  public static void addProduct(Shop shop) {
    String currency = shop.getCurrency();
    Set<Product> productSet = products.computeIfAbsent(currency, k -> new HashSet<>());

    for (Product product : shop.getProducts()) {
      if (product.getItemchance().getItem().startsWith("pokemon:") || product.getSell().compareTo(BigDecimal.ZERO) <= 0)
        continue;

      ItemStack productStack = product.getItemchance().getItemStack();
      productSet.removeIf(p -> ItemStack.areEqual(p.getItemchance().getItemStack(), productStack));
      productSet.add(product);
    }
  }

  /**
   * Sells all products from the player's inventory and updates the player's balance.
   *
   * @param player         The player who is selling the products.
   * @param shopConfigMenu
   */
  public static void sellProducts(ServerPlayerEntity player, ShopConfigMenu shopConfigMenu) {
    PlayerInventory inventory = player.getInventory();
    Map<String, BigDecimal> currencyTotals = new HashMap<>();

    products.forEach((currency, productSet) -> {
      BigDecimal currencyTotal = BigDecimal.ZERO;
      int decimals = EconomyUtil.getDecimals(currency);

      for (Product product : productSet) {
        if (!LuckPermsUtil.checkPermission(player, product.getPermission())) continue;

        BigDecimal sellPrice = product.getSell().setScale(decimals, RoundingMode.HALF_UP);
        if (sellPrice.compareTo(BigDecimal.ZERO) <= 0) continue;

        ItemStack productStack = product.getItemchance().getItemStack();

        for (ItemStack itemStack : inventory.main) {
          if (itemStack.isEmpty() || !ItemStack.areEqual(itemStack, productStack)) continue;

          int amount = itemStack.getCount();
          BigDecimal price = sellPrice.multiply(BigDecimal.valueOf(amount));
          currencyTotal = currencyTotal.add(price);

          // Add transaction
          ShopTransactions.addTransaction(
            player.getUuid(), // Assuming you have a method to get the player's UUID
            currency,
            ShopTransactions.ShopAction.SELL, // Assuming there's an action type for selling
            product,
            BigDecimal.valueOf(amount),
            price
          );

          // Remove items from the inventory
          itemStack.decrement(amount);
        }
      }

      if (currencyTotal.compareTo(BigDecimal.ZERO) > 0) {
        currencyTotals.put(currency, currencyTotal.setScale(decimals, RoundingMode.HALF_UP));
      }
    });

    if (!currencyTotals.isEmpty()) {
      StringBuilder message = new StringBuilder(CobbleUtils.shopLang.getMessageSell());
      StringBuilder currencyMessage = new StringBuilder();

      currencyTotals.forEach((currency, total) -> {
        currencyMessage.append(String.format("\n &6%s &a%s,", EconomyUtil.formatCurrency(total, currency, player.getUuid()), currency));
        EconomyUtil.addMoney(player, currency, total);
      });

      message = new StringBuilder(message.toString()
        .replace("%currencys%", currencyMessage.toString().replaceAll(",\n$", ""))
        .replace("%prefix%", CobbleUtils.shopLang.getPrefix()));
      player.sendMessage(AdventureTranslator.toNative(message.toString()));
      ShopTransactions.updateTransaction(player.getUuid(), shopConfigMenu);
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
      player.sendMessage(
        AdventureTranslator.toNative(
          CobbleUtils.shopLang.getMessageSellHandNoItem()
            .replace("%prefix%", CobbleUtils.shopLang.getPrefix())
        )
      );
      return;
    }

    boolean sold = false;
    String currencyUsed = null;
    BigDecimal totalEarned = BigDecimal.ZERO;
    Product productSold = null;
    for (Map.Entry<String, Set<Product>> entry : products.entrySet()) {
      String currency = entry.getKey();
      Set<Product> productSet = entry.getValue();
      int decimals = EconomyUtil.getDecimals(currency);

      for (Product product : productSet) {
        if (!LuckPermsUtil.checkPermission(player, product.getPermission())) continue;

        BigDecimal sellPrice = product.getSell().setScale(decimals, RoundingMode.HALF_UP);
        if (sellPrice.compareTo(BigDecimal.ZERO) <= 0) continue;

        ItemStack productStack = product.getItemchance().getItemStack();
        if (ItemStack.areEqual(mainHandStack, productStack)) {
          int amount = mainHandStack.getCount();
          BigDecimal price = sellPrice.multiply(BigDecimal.valueOf(amount));
          EconomyUtil.addMoney(player, currency, price);
          mainHandStack.decrement(amount);
          totalEarned = totalEarned.add(price);
          currencyUsed = currency;
          sold = true;
          productSold = product;
          break;
        }
      }
      if (sold) break;
    }

    if (sold) {
      ShopTransactions.addTransaction(
        player.getUuid(),
        currencyUsed,
        ShopTransactions.ShopAction.SELL,
        productSold,
        BigDecimal.valueOf(mainHandStack.getCount()),
        totalEarned
      );
      player.sendMessage(AdventureTranslator.toNative(
        CobbleUtils.shopLang.getMessageSellHand()
          .replace("%prefix%", CobbleUtils.shopLang.getPrefix())
          .replace("%balance%", EconomyUtil.formatCurrency(totalEarned, currencyUsed, player.getUuid()))
          .replace("%currency%", currencyUsed)));
      ShopTransactions.updateTransaction(player.getUuid(), shopConfigMenu);
    } else {
      player.sendMessage(
        AdventureTranslator.toNative(
          CobbleUtils.shopLang.getMessageSellHandNoItemPrice()
            .replace("%prefix%", CobbleUtils.shopLang.getPrefix())
        )
      );
    }
  }
}
