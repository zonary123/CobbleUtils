package com.kingpixel.cobbleutils.config;

import com.google.gson.Gson;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Getter;
import lombok.ToString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 13/06/2024 12:05
 */
@Getter
@ToString
public class PoolItems {
  private Map<String, List<ItemChance>> randomitems;


  public PoolItems() {
    randomitems = new HashMap<>();
    randomitems.put("default", ItemChance.defaultItemChances());
    randomitems.put("pokeballs", List.of(new ItemChance("cobblemon:poke_ball", 100)));
    randomitems.put("fossil", List.of(new ItemChance("cobblemon:helix_fossil", 100), new ItemChance("cobblemon:dome_fossil", 100)));
  }

  public void init() {
    randomitems.clear();
    CompletableFuture<Boolean> futureRead = Utils.readFileAsync(CobbleUtils.PATH_RANDOM, "items.json",
      el -> {
        Gson gson = Utils.newGson();
        PoolItems config = gson.fromJson(el, PoolItems.class);
        randomitems = config.getRandomitems();
        String data = gson.toJson(this);
        CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(CobbleUtils.PATH_RANDOM, "items.json",
          data);
        if (!futureWrite.join()) {
          CobbleUtils.LOGGER.fatal("Could not write items.json file for" + CobbleUtils.MOD_NAME + " .");
        }
      });

    if (!futureRead.join()) {
      CobbleUtils.LOGGER.info("No items.json file found for" + CobbleUtils.MOD_NAME + ". Attempting to generate one.");
      Gson gson = Utils.newGson();
      String data = gson.toJson(this);
      CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(CobbleUtils.PATH_RANDOM, "items.json",
        data);

      if (!futureWrite.join()) {
        CobbleUtils.LOGGER.fatal("Could not write items.json file for " + CobbleUtils.MOD_NAME + ".");
      }
    }

  }

  /**
   * Método para obtener un ítem aleatorio basado en las probabilidades configuradas.
   *
   * @param category La categoría de ítems de la que seleccionar.
   *
   * @return El ítem seleccionado según las probabilidades.
   */
  public ItemChance getRandomItem(String category) {
    List<ItemChance> items = randomitems.get(category);
    if (items == null || items.isEmpty()) return new ItemChance("", 100);


    int totalWeight = items.stream().mapToInt(ItemChance::getChance).sum();
    int randomValue = Utils.RANDOM.nextInt(totalWeight) + 1;

    int currentWeight = 0;
    for (ItemChance itemChance : items) {
      currentWeight += itemChance.getChance();
      if (randomValue <= currentWeight) {
        return itemChance;
      }
    }
    return items.get(Utils.RANDOM.nextInt(items.size()));
  }

}