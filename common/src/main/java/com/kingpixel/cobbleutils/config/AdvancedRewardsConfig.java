package com.kingpixel.cobbleutils.config;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.AdvancedItemChance;
import com.kingpixel.cobbleutils.Model.Animations.core.Animations;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.util.UtilsFile;
import lombok.Data;
import lombok.Getter;

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
  private static final Path FOLDER = CobbleUtils.getPathMod().resolve("advancedRewards");
  @Getter private static final Map<String, AdvancedItemChance> TEMPLATE_REWARDS = new HashMap<>();

  public void init() {
    TEMPLATE_REWARDS.clear();

    setupFoldersAndDefaults();
    loadAndSanitizeRewards();

    CobbleUtils.LOGGER_RAW.info("Loaded and formatted " + TEMPLATE_REWARDS.size() + " reward files.");
  }

  private void setupFoldersAndDefaults() {
    try {
      if (!Files.exists(FOLDER)) {
        Files.createDirectories(FOLDER);
        createFile("easy_reward", new AdvancedItemChance());
      }

      Path animationsFolder = FOLDER.resolve("animations");
      Files.createDirectories(animationsFolder);

      for (Animations value : Animations.values()) {
        Path f = animationsFolder.resolve(value.name() + ".json");
        AdvancedItemChance animation = new AdvancedItemChance();
        animation.setAnimation(value);
        animation.setGiveAll(true);
        animation.getLootTable().get("").add(new ItemChance("pokemon:pikachu", 1));
        animation.getLootTable().get("").add(new ItemChance("item:1:minecraft:diamond", 1));
        animation.getLootTable().get("").add(new ItemChance("item:1:minecraft:iron", 1));
        UtilsFile.write(f, animation);
      }
    } catch (IOException e) {
      CobbleUtils.LOGGER_RAW.error("Error setting up folders or defaults: " + e.getMessage());
    }
  }

  private void loadAndSanitizeRewards() {
    if (!Files.exists(FOLDER)) return;

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
        CobbleUtils.LOGGER_RAW.error("Error loading/formatting reward file: " + f + " - " + e.getMessage());
      }
    }
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

  public static AdvancedItemChance getAdvancedReward(String id) {
    return TEMPLATE_REWARDS.get(id);
  }
}