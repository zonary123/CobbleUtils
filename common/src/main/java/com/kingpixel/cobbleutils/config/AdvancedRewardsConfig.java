package com.kingpixel.cobbleutils.config;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.model.rewards.AdvancedReward;
import com.kingpixel.cobbleutils.api.RewardsAPI;
import com.kingpixel.cobbleutils.util.UtilsFile;
import lombok.Data;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 02/09/2025 12:16
 */
@Data
public class AdvancedRewardsConfig {
  private static final String PATH = CobbleUtils.PATH + "/advancedRewards/";

  public void init() {
    Path folder = CobbleUtils.getPathMod().resolve("advancedRewards");
    if (folder.toFile().exists()) {
      List<Path> files = UtilsFile.getAllJsonFiles(folder);
      for (Path f : files) {
        try {
          AdvancedReward advancedReward = UtilsFile.read(f, AdvancedReward.class);
          if (advancedReward != null) {
            String id = f.toFile().getName().replace(".json", "");
            RewardsAPI.registerAdvancedReward(id, advancedReward);
            UtilsFile.write(f, advancedReward);
          }
        } catch (Exception e) {
          CobbleUtils.LOGGER.error("Error loading reward file: " + f + " - " + e.getMessage());
        }
      }
    } else {
      folder.toFile().mkdirs();
      try {
        createDefaultFiles();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void createDefaultFiles() throws IOException {
    createFile("easy_reward", new AdvancedReward());
  }

  private void createFile(String fileName, AdvancedReward advancedReward) throws IOException {
    String id = fileName.replace(".json", "");
    RewardsAPI.registerAdvancedReward(id, advancedReward);
    Path filePath = CobbleUtils.getPathMod().resolve("advancedRewards/" + fileName + ".json");
    UtilsFile.write(filePath, advancedReward);
  }

}
