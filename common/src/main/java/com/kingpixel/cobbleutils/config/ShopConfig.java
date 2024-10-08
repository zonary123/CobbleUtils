package com.kingpixel.cobbleutils.config;

import com.google.gson.Gson;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.features.shops.Shop;
import com.kingpixel.cobbleutils.features.shops.ShopConfigMenu;
import com.kingpixel.cobbleutils.features.shops.models.types.ShopTypeDynamic;
import com.kingpixel.cobbleutils.features.shops.models.types.ShopTypeDynamicWeekly;
import com.kingpixel.cobbleutils.features.shops.models.types.ShopTypePermanent;
import com.kingpixel.cobbleutils.features.shops.models.types.ShopTypeWeekly;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.kingpixel.cobbleutils.features.shops.ShopTransactions.loadTransactions;

/**
 * @author Carlos Varas Alonso - 13/08/2024 17:14
 */
@Getter
@Setter
@ToString
public class ShopConfig {
  private ShopConfigMenu shop;
  public static final Map<ShopConfigMenu.ShopMod, List<Shop>> shops = new ConcurrentHashMap<>();

  public ShopConfig() {
    shop = new ShopConfigMenu();
  }

  public void createConfigIfNotExists(String mod_id, String pathShops, String pathShop) {
    CompletableFuture<Boolean> futureRead = Utils.readFileAsync(pathShop, "shopconfig.json",
      el -> {
        Gson gson = Utils.newGson();
        ShopConfig config = gson.fromJson(el, ShopConfig.class);
        this.shop = config.getShop();
        CobbleUtils.LOGGER.info("shopconfig.json loaded successfully from " + pathShop);
        addShopsFromPath(mod_id, pathShops);
      });

    if (!futureRead.join()) {
      CobbleUtils.LOGGER.info("No shopconfig.json file found at " + pathShop + ". Creating a new one.");
      ShopConfig newConfig = new ShopConfig();
      Gson gson = Utils.newGson();
      String data = gson.toJson(newConfig);
      CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(pathShop, "shopconfig.json", data);
      if (!futureWrite.join()) {
        CobbleUtils.LOGGER.fatal("Could not write shopconfig.json file to " + pathShop);
      } else {
        CobbleUtils.LOGGER.info("shopconfig.json created successfully at " + pathShop);
      }
    }
  }

  public static List<Shop> addShopsFromPath(String mod_id, String path) {
    List<Shop> shopList = ShopConfigMenu.getShops(path);

    String default_path = path + "defaults/";
    // If no shops are found, create default shops
    CobbleUtils.LOGGER.info("No shops found. Creating default shops.");

    List<Shop> defaultShops = createDefaultShops();

    saveShopsToPath(mod_id, default_path, defaultShops);


    ShopConfigMenu.ShopMod shopMod = new ShopConfigMenu.ShopMod(mod_id, path);

    if (CobbleUtils.config.isDebug()) {
      CobbleUtils.LOGGER.info("Adding to map: " + shopMod);
    }

    shops.put(shopMod, shopList);

    saveShops();
    return shopList;
  }

  private static List<Shop> createDefaultShops() {
    List<Shop> shopArrayList = List.of(
      new Shop("permanent", "Permanent", new ShopTypePermanent(), (short) 6, List.of()),
      new Shop("dynamic", "Dynamic", new ShopTypeDynamic(), (short) 6, List.of()),
      new Shop("weekly", "Weekly", new ShopTypeWeekly(), (short) 6, List.of()),
      new Shop("dynamicweekly", "DynamicWeekly", new ShopTypeDynamicWeekly(), (short) 6, List.of())
    );
    for (int i = 0; i < shopArrayList.size(); i++) {
      shopArrayList.get(i).getDisplay().setSlot(i);
    }
    return shopArrayList;
  }

  private static void saveShopsToPath(String mod_id, String path, List<Shop> shopList) {
    Gson gson = Utils.newGson();
    File directory = Utils.getAbsolutePath(path);
    if (!directory.exists()) {
      directory.mkdirs();
    }

    for (Shop shop : shopList) {
      String json = gson.toJson(shop);
      String fileName = shop.getId() + ".json";

      try {
        Utils.writeFileAsync(path, fileName, json).join();
      } catch (Exception e) {
        CobbleUtils.LOGGER.error("Failed to save default shop " + shop.getId());
      }
    }
  }

  public static void saveShops() {
    Gson gson = Utils.newGson();

    for (Map.Entry<ShopConfigMenu.ShopMod, List<Shop>> entry : shops.entrySet()) {
      ShopConfigMenu.ShopMod shopMod = entry.getKey();
      List<Shop> shopList = entry.getValue();

      for (Shop shop : shopList) {
        String json = gson.toJson(shop);
        String fileName = shop.getId() + ".json";

        try {
          Utils.writeFileAsync(shopMod.getPath(), fileName, json).join();
        } catch (Exception e) {
          CobbleUtils.LOGGER.error("Failed to save shop " + shop.getId() + " for mod " + e);
        }
      }
    }
  }

  public void init(String pathShop, String mod_id, String pathShops) {
    createConfigIfNotExists(mod_id, pathShops, pathShop);

    List<Shop> shopList = addShopsFromPath(mod_id, pathShops);

    loadTransactions(shop);
    ShopConfigMenu.addShops(mod_id, pathShops, shopList);
  }
}
