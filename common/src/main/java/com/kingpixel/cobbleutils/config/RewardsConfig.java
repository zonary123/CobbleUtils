package com.kingpixel.cobbleutils.config;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemChance;
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
public class RewardsConfig {
  private static final Path FOLDER = Path.of(CobbleUtils.PATH).resolve("rewards");
  private final Map<String, ItemChance> rewards = new HashMap<>();

  public void init() {
    rewards.clear();
    if (Files.exists(FOLDER)) {
      List<Path> files = UtilsFile.getAllJsonFiles(FOLDER);
      for (Path f : files) {
        try {
          ItemChance itemChance = UtilsFile.read(f, ItemChance.class);
          if (itemChance == null) continue;
          String id = f.getFileName().toString().replace(".json", "");
          UtilsFile.write(f, itemChance);
          if (rewards.containsKey(id)) {
            CobbleUtils.LOGGER_RAW.error("Duplicate reward id found: " + id + " in file: " + f);
          } else {
            rewards.put(id, itemChance);
          }
        } catch (Exception e) {
          CobbleUtils.LOGGER_RAW.error("Error loading reward file: " + f + " - " + e.getMessage());
        }
      }
    } else {
      try {
        Files.createDirectories(FOLDER);
      } catch (IOException e) {
        CobbleUtils.LOGGER_RAW.error("Error creating rewards folder: " + e.getMessage());
      }
      createDefaultFiles();
    }
    CobbleUtils.LOGGER_RAW.info("Loaded " + rewards.size() + " reward files.");
  }

  private void createDefaultFiles() {
    createFile("default", new ItemChance());
    createFile("pokeball", new ItemChance());
    createFile("berries", new ItemChance());
    createFile("battle", new ItemChance());
    createFile("evolution", new ItemChance());
  }

  private void createFile(String fileName, ItemChance itemChance) {
    String id = fileName.replace(".json", "");
    rewards.put(id, itemChance);
    Path file = FOLDER.resolve(fileName + ".json");
    try {
      UtilsFile.write(file, itemChance);
    } catch (IOException e) {
      CobbleUtils.LOGGER_RAW.error("Error writing reward file: " + file + " - " + e.getMessage());
    }
  }
}
