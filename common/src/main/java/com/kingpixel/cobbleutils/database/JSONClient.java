package com.kingpixel.cobbleutils.database;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.features.breeding.manager.ManagerPlotEggs;
import com.kingpixel.cobbleutils.features.breeding.models.PlotBreeding;
import com.kingpixel.cobbleutils.util.Utils;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 07/08/2024 9:41
 */
public class JSONClient implements DatabaseClient {
  private Map<UUID, List<PlotBreeding>> eggs = new HashMap<>();

  public JSONClient(DataBaseConfig dataBaseConfig) {
  }

  @Override public void connect() {

  }

  @Override public void disconnect() {

  }

  @Override public void save() {

  }

  // Daycare
  @Override public List<PlotBreeding> getPlots(ServerPlayerEntity player) {
    List<PlotBreeding> plots = eggs.get(player.getUuid());
    if (plots == null) {
      plots = readPlots(player);
      savePlots(player, plots);
    }
    return plots;
  }

  private List<PlotBreeding> readPlots(ServerPlayerEntity player) {
    CompletableFuture<Boolean> future = Utils.readFileAsync(CobbleUtils.PATH_BREED_DATA, player.getUuid() + ".json",
      call -> {
        List<PlotBreeding> plots;
        Gson gson = Utils.newWithoutSpacingGson();
        TypeToken<List<PlotBreeding>> typeToken = new TypeToken<>() {
        };
        plots = gson.fromJson(call, typeToken.getType());
        if (plots == null || plots.isEmpty()) {
          plots = ManagerPlotEggs.createPlots();
        }
        savePlots(player, plots);
      }).exceptionally(throwable -> {
      throwable.printStackTrace();
      return false;
    });
    if (future.join()) {
      return eggs.get(player.getUuid());
    }
    return new ArrayList<>();
  }

  @Override public void savePlots(ServerPlayerEntity player, List<PlotBreeding> plots) {
    eggs.put(player.getUuid(), plots);
    Utils.writeFileAsync(CobbleUtils.PATH_BREED_DATA, player.getUuid() + ".json", Utils.newWithoutSpacingGson().toJson(plots));
  }


  @Override public void checkDaycarePlots(ServerPlayerEntity player) {
    DatabaseClientFactory.CheckDaycarePlots(player);
  }

  @Override public void removeDataIfNecessary(ServerPlayerEntity player) {
    savePlots(player, getPlots(player));
    eggs.remove(player.getUuid());
  }


}
