package com.kingpixel.cobbleutils.config;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Data;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Carlos Varas Alonso - 02/09/2025 12:16
 */
@Data
public class RewardsConfig {
  private static final String PATH = CobbleUtils.PATH + "/rewards/";
  private final Map<String, ItemChance> rewards = new HashMap<>();

  public void init() {
    rewards.clear();
    File folder = Utils.getAbsolutePath(PATH);
    if (folder.exists()) {
      List<File> files = getAllJsonFilesRecursive(folder);
      for (File f : files) {
        try {
          ItemChance itemChance = Utils.newGson().fromJson(Utils.readFileSync(f), ItemChance.class);
          String id = f.getName().replace(".json", "");
          Utils.writeFileSync(f, Utils.newGson().toJson(itemChance, ItemChance.class));
          if (rewards.containsKey(id)) {
            CobbleUtils.LOGGER.error("Duplicate reward id found: " + id + " in file: " + f.getPath());
          } else {
            rewards.put(id, itemChance);
          }
        } catch (Exception e) {
          CobbleUtils.LOGGER.error("Error loading reward file: " + f.getPath() + " - " + e.getMessage());
        }
      }
    } else {
      folder.mkdirs();
      createDefaultFiles();
    }
    CobbleUtils.LOGGER.info("Loaded " + rewards.size() + " reward files.");
  }

  private List<File> getAllJsonFilesRecursive(File folder) {
    List<File> files = new ArrayList<>();
    File[] list = folder.listFiles();
    if (list != null) {
      for (File f : list) {
        if (f.isDirectory()) {
          files.addAll(getAllJsonFilesRecursive(f));
        } else if (f.isFile() && f.getName().endsWith(".json")) {
          files.add(f);
        }
      }
    }
    return files;
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
    File file = Utils.getAbsolutePath(PATH + fileName + ".json");
    Utils.writeFileSync(file, Utils.newGson().toJson(itemChance,
      ItemChance.class));
  }
}
