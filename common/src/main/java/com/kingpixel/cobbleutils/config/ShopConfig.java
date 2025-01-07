package com.kingpixel.cobbleutils.config;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemModel;
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
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Arrays;
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
  @SerializedName("shop")
  private ShopConfigMenu shopConfigMenu;
  public static final Map<ShopConfigMenu.ShopMod, List<Shop>> shops = new ConcurrentHashMap<>();

  public ShopConfig() {
    shopConfigMenu = new ShopConfigMenu();
  }

  public void createConfigIfNotExists(String pathShop) {
    CompletableFuture<Boolean> futureRead = Utils.readFileAsync(pathShop, "shopconfig.json",
      el -> {
        Gson gson = Utils.newGson();
        ShopConfig config = gson.fromJson(el, ShopConfig.class);
        this.shopConfigMenu = config.getShopConfigMenu();
        CobbleUtils.LOGGER.info("shopconfig.json loaded successfully from " + pathShop);
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
    if (true) {
      // Default shops
      String default_path = path + "defaults/";
      CobbleUtils.LOGGER.info("No shops found. Creating default shops.");
      List<Shop> defaultShops = createDefaultShops();
      saveShopsToPath(mod_id, default_path, defaultShops);
    }
    // Load shops
    List<Shop> shopList = ShopConfigMenu.getShops(path);
    shopList.forEach(ShopConfig::checkShop);
    saveShopsToPath(mod_id, path, shopList);
    ShopConfigMenu.ShopMod shopMod = new ShopConfigMenu.ShopMod(mod_id, path);
    shops.put(shopMod, shopList);

    return shopList;
  }

  public static List<Shop> createDefaultShops() {
    List<Shop> shopArrayList = new ArrayList<>();
    shopArrayList.add(new Shop("permanent", "Permanent", new ShopTypePermanent(), (short) 6, List.of()));
    shopArrayList.add(new Shop("dynamic", "Dynamic", new ShopTypeDynamic(), (short) 6, List.of(
      "%cooldown%",
      "%amountProducts%"
    )));
    shopArrayList.add(new Shop("weekly", "Weekly", new ShopTypeWeekly(), (short) 6, List.of(
      "%days%"
    )));
    shopArrayList.add(new Shop("dynamicweekly", "DynamicWeekly", new ShopTypeDynamicWeekly(), (short) 6, List.of(
      "%cooldown%",
      "%amountProducts%",
      "%days%"
    )));


    for (int i = 0; i < shopArrayList.size(); i++) {
      shopArrayList.get(i).getDisplay().setSlot(i);
    }
    return shopArrayList;
  }

  public static void saveShopsToPath(String mod_id, String path, List<Shop> shopList) {
    Gson gson = Utils.newGson();
    File directory = Utils.getAbsolutePath(path);
    if (!directory.exists()) {
      directory.mkdirs();
    }

    if (shopList == null || shopList.isEmpty()) shopList = createDefaultShops();

    for (Shop shop : shopList) {
      String json = gson.toJson(shop);
      String fileName = shop.getId() + ".json";
      checkShop(shop);

      try {
        Utils.writeFileAsync(path, fileName, json).join();
      } catch (Exception e) {
        CobbleUtils.LOGGER.error("Failed to save default shop " + shop.getId());
      }
    }
  }

  public static void checkShop(Shop shop) {
    if (shop.getRows() < 1 || shop.getRows() > 6) shop.setRows((short) 6);
    int max_array = (shop.getRows() * 9) - 1;

    if (shop.getPrevious() == null) {
      ItemModel previous = CobbleUtils.language.getItemPrevious();
      previous.setSlot(max_array - 8);
      shop.setPrevious(previous);
    }
    if (shop.getClose() == null) {
      ItemModel close = CobbleUtils.language.getItemClose();
      close.setSlot(max_array - 4);
      shop.setClose(close);
    }
    if (shop.getNext() == null) {
      ItemModel next = CobbleUtils.language.getItemNext();
      next.setSlot(max_array);
      shop.setNext(next);
    }

    if (shop.getCloseCommand() == null) shop.setCloseCommand("");

    if (shop.getPrevious().getSlot() == null || shop.getPrevious().getSlot() > max_array)
      shop.getPrevious().setSlot(max_array - 8);
    if (shop.getClose().getSlot() == null || shop.getClose().getSlot() > max_array)
      shop.getClose().setSlot(max_array - 4);
    if (shop.getNext().getSlot() == null || shop.getNext().getSlot() > max_array)
      shop.getNext().setSlot(max_array);
    if (shop.getProducts() == null || shop.getProducts().isEmpty()) shop.setProducts(Shop.getDefaultProducts());

    switch (shop.getShopType().getTypeShop()) {
      case PERMANENT:
        break;
      case DYNAMIC:
        ShopTypeDynamic dynamic = (ShopTypeDynamic) shop.getShopType();
        if (dynamic.getAmountProducts() < 1) dynamic.setAmountProducts(6);
        if (dynamic.getMinutes() < 1) dynamic.setMinutes(60);
        break;
      case WEEKLY:
        ShopTypeWeekly weekly = (ShopTypeWeekly) shop.getShopType();
        if (weekly.getDayOfWeek() == null || weekly.getDayOfWeek().isEmpty())
          weekly.setDayOfWeek(Arrays.stream(DayOfWeek.values()).toList());
        break;
      case DYNAMIC_WEEKLY:
        ShopTypeDynamicWeekly dynamicWeekly = (ShopTypeDynamicWeekly) shop.getShopType();
        if (dynamicWeekly.getAmountProducts() < 1) dynamicWeekly.setAmountProducts(6);
        if (dynamicWeekly.getMinutes() < 1) dynamicWeekly.setMinutes(60);
        if (dynamicWeekly.getDayOfWeek() == null || dynamicWeekly.getDayOfWeek().isEmpty())
          dynamicWeekly.setDayOfWeek(Arrays.stream(DayOfWeek.values()).toList());
        break;
      default:
        break;
    }

  }

  public void init(String pathShop, String mod_id, String pathShops) {
    createConfigIfNotExists(pathShop);

    List<Shop> shopList = addShopsFromPath(mod_id, pathShops);
    shopList.forEach(ShopConfig::checkShop);
    loadTransactions(shopConfigMenu);
    ShopConfigMenu.addShops(mod_id, pathShops, shopList, shopConfigMenu);
  }
}
