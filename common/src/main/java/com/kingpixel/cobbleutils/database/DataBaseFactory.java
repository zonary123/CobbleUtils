package com.kingpixel.cobbleutils.database;

import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.database.blocks.DataBaseBlock;
import com.kingpixel.cobbleutils.database.blocks.DataBaseBlockSQLite;

/**
 * @author Carlos Varas Alonso - 23/08/2025 7:35
 */
public class DataBaseFactory {
  public static DataBaseBlock INSTANCE;

  public static void init(DataBaseConfig config) {
    initDataBaseBlock(config);
  }

  private static void initDataBaseBlock(DataBaseConfig config) {
    if (INSTANCE != null) INSTANCE.disconnect();
    INSTANCE = switch (config.getType()) {
      case JSON -> null;
      case MONGODB -> null;
      case MYSQL -> null;
      case SQLITE -> new DataBaseBlockSQLite(config);
    };
    if (INSTANCE != null)
      INSTANCE.connect();
  }
}
