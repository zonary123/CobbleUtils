package com.kingpixel.cobbleutils.config;

import com.cobblemon.mod.common.api.storage.NoPokemonStoreException;
import com.google.gson.Gson;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Getter;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 25/06/2024 22:19
 */
@Getter
public class PoolMoney {
  private Map<String, List<ItemChance>> randomMoney;


  public PoolMoney() {
    randomMoney = Map.of(
      "default", List.of(new ItemChance("money:tokens:100", 50), new ItemChance("money:100", 50))
    );
  }

  public void init() {
    CompletableFuture<Boolean> futureRead = Utils.readFileAsync(CobbleUtils.PATH_RANDOM, "money.json",
      el -> {
        Gson gson = Utils.newGson();
        PoolMoney config = gson.fromJson(el, PoolMoney.class);
        randomMoney = config.getRandomMoney();
        String data = gson.toJson(this);
        CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(CobbleUtils.PATH_RANDOM, "money.json",
          data);
        if (!futureWrite.join()) {
          CobbleUtils.LOGGER.fatal("Could not write money.json file for" + CobbleUtils.MOD_NAME + " .");
        }
      });

    if (!futureRead.join()) {
      CobbleUtils.LOGGER.info("No money.json file found for" + CobbleUtils.MOD_NAME + ". Attempting to generate one.");
      Gson gson = Utils.newGson();
      String data = gson.toJson(this);
      CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(CobbleUtils.PATH_RANDOM, "money.json",
        data);

      if (!futureWrite.join()) {
        CobbleUtils.LOGGER.fatal("Could not write money.json file for " + CobbleUtils.MOD_NAME + ".");
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
  public boolean getRandomMoney(ServerPlayerEntity player, String category) {
    List<ItemChance> moneys = randomMoney.get(category);
    if (moneys == null || moneys.isEmpty()) {
      return false;
    }

    double totalWeight = moneys.stream().mapToDouble(ItemChance::getChance).sum();
    double randomValue = Utils.RANDOM.nextDouble(totalWeight) + 1;

    double currentWeight = 0;
    for (ItemChance itemChance : moneys) {
      currentWeight += itemChance.getChance();
      if (randomValue <= currentWeight) {
        try {
          return ItemChance.giveReward(player, itemChance);
        } catch (NoPokemonStoreException e) {
          e.printStackTrace();
        }
      }
    }
    return false;
  }


}
