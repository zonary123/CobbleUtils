package com.kingpixel.cobbleutils.database;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.features.breeding.models.PlotBreeding;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 24/07/2024 21:03
 */
public class DatabaseClientFactory {
  public static DatabaseClient databaseClient;

  public static DatabaseClient createDatabaseClient(DataBaseConfig database) {
    if (databaseClient != null) {
      databaseClient.disconnect();
    }
    switch (database.getType()) {
      case MONGODB -> databaseClient = new MongoDBClient(database);
      case JSON -> databaseClient = new JSONClient(database);
      default -> databaseClient = new JSONClient(database);
    }
    databaseClient.connect();
    return databaseClient;
  }

  // Daycare
  public static void CheckDaycarePlots(ServerPlayerEntity player) {
    boolean update = false;
    List<PlotBreeding> plots = databaseClient.getPlots(player);
    int size = CobbleUtils.breedconfig.getPlotSlots().size();
    List<PlotBreeding> removedPlots = new ArrayList<>();

    if (plots == null || plots.isEmpty()) {
      plots = new ArrayList<>();
      for (int i = 0; i < size; i++) {
        plots.add(new PlotBreeding());
      }
      update = true;
    }
    if (plots.size() < size) {
      for (int i = plots.size(); i < size; i++) {
        plots.add(new PlotBreeding());
      }
      update = true;
    }

    if (plots.size() > size) {
      removedPlots = new ArrayList<>(plots.subList(size, plots.size()));
      plots = plots.subList(0, size);
      update = true;
    }


    for (PlotBreeding plot : plots) {
      if (plot.checking(player)) {
        update = true;
      }
    }

    if (update) {
      databaseClient.savePlots(player, plots);
    }


    // Return Pokémon from removed plots
    try {
      PCStore pcStore = Cobblemon.INSTANCE.getStorage().getPC(player);
      for (PlotBreeding removedPlot : removedPlots) {
        if (removedPlot.getMale() != null) {
          pcStore.add(removedPlot.obtainMale());
        }
        if (removedPlot.getFemale() != null) {
          pcStore.add(removedPlot.obtainFemale());
        }
        removedPlot.getEggs().forEach(egg -> pcStore.add(Pokemon.Companion.loadFromJSON(DynamicRegistryManager.EMPTY,
          egg)));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

