package com.kingpixel.cobbleutils.config;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.model.rewards.Reward;
import com.kingpixel.cobbleutils.api.RewardsAPI;
import com.kingpixel.cobbleutils.util.UtilsFile;
import lombok.Data;

import java.nio.file.Path;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 02/09/2025 12:16
 */
@Data
public class RewardsConfig {

  public void init() {
    Path folder = CobbleUtils.getPathMod().resolve("rewards");
    if (folder.toFile().exists()) {

      List<Path> files = UtilsFile.getAllJsonFiles(folder);

      for (Path f : files) {
        try {
          Reward reward = UtilsFile.read(f, Reward.class);
          if (reward == null) {
            CobbleUtils.LOGGER.error("Reward file is empty or invalid: " + f);
            continue;
          }
          reward.setId(f.toFile().getName().replace(".json", ""));
          RewardsAPI.registerReward(reward);
          UtilsFile.write(f, reward);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

    } else {
      folder.toFile().mkdirs();
      createDefaultFiles();
    }

  }


  private void createDefaultFiles() {
    createFile("default", new Reward());
    createFile("pokeball", new Reward());
    createFile("berries", new Reward());
    createFile("battle", new Reward());
    createFile("evolution", new Reward());
  }

  private void createFile(String fileName, Reward reward) {
    Path path = CobbleUtils.getPathMod().resolve("rewards").resolve(fileName + ".json");
    if (!path.toFile().exists()) {
      try {
        UtilsFile.write(path, reward);
      } catch (Exception e) {
        CobbleUtils.LOGGER.error("Error creating default reward file: " + path + " - " + e.getMessage());
      }
    }
  }
}
