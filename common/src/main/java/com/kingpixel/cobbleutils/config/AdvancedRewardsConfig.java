package com.kingpixel.cobbleutils.config;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.AdvancedItemChance;
import com.kingpixel.cobbleutils.util.UtilsFile;
import lombok.Data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Carlos Varas Alonso - 02/09/2025 12:16
 */
@Data
public class AdvancedRewardsConfig {
  private static final Path FOLDER = Path.of(CobbleUtils.PATH).resolve("advancedRewards");
  private final Map<String, AdvancedItemChance> TEMPLATE_REWARDS = new HashMap<>();

  public void init() {
    TEMPLATE_REWARDS.clear();
    if (Files.exists(FOLDER)) {
      List<Path> files = UtilsFile.getAllJsonFiles(FOLDER);
      for (Path f : files) {
        try {
          AdvancedItemChance advancedItemChance = UtilsFile.read(f, AdvancedItemChance.class);
          if (advancedItemChance == null) continue;
          String id = f.getFileName().toString().replace(".json", "");
          UtilsFile.write(f, advancedItemChance);
          if (TEMPLATE_REWARDS.containsKey(id)) {
            CobbleUtils.LOGGER_RAW.error("Duplicate reward id found: " + id + " in file: " + f);
          } else {
            TEMPLATE_REWARDS.put(id, advancedItemChance);
          }
        } catch (Exception e) {
          CobbleUtils.LOGGER_RAW.error("Error loading reward file: " + f + " - " + e.getMessage());
        }
      }
    } else {
      try {
        Files.createDirectories(FOLDER);
      } catch (IOException e) {
        CobbleUtils.LOGGER_RAW.error("Error creating advancedRewards folder: " + e.getMessage());
      }
      createDefaultFiles();
    }
    CobbleUtils.LOGGER_RAW.info("Loaded " + TEMPLATE_REWARDS.size() + " reward files.");
  }

  private void createDefaultFiles() {
    createFile("easy_reward", new AdvancedItemChance());
  }

  private void createFile(String fileName, AdvancedItemChance advancedItemChance) {
    String id = fileName.replace(".json", "");
    TEMPLATE_REWARDS.put(id, advancedItemChance);
    Path file = FOLDER.resolve(fileName + ".json");
    try {
      UtilsFile.write(file, advancedItemChance);
    } catch (IOException e) {
      CobbleUtils.LOGGER_RAW.error("Error writing advanced reward file: " + file + " - " + e.getMessage());
    }
  }

}
