package com.kingpixel.cobbleutils.database;

import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.database.blocks.DataBaseBlock;
import com.kingpixel.cobbleutils.database.blocks.DataBaseBlockSQLite;
import com.kingpixel.cobbleutils.database.users.DataBaseUsers;
import com.kingpixel.cobbleutils.database.users.DataBaseUsersJson;
import com.kingpixel.cobbleutils.database.users.DataBaseUsersMongoDB;

/**
 * @author Carlos Varas Alonso - 23/08/2025 7:35
 */
public class DataBaseFactory {
  public static DataBaseBlock dataBaseBlock;
  public static DataBaseUsers dataBaseUsers;

  public static void init(DataBaseConfig config) {
    initDataBaseBlock(config);
    initDataBaseUsers(config);
  }

  private static void initDataBaseBlock(DataBaseConfig config) {
    if (dataBaseBlock != null) dataBaseBlock.disconnect();
    dataBaseBlock = switch (config.getType()) {
      case SQLITE -> new DataBaseBlockSQLite(config);
      default -> new DataBaseBlockSQLite(config);
    };
    if (dataBaseBlock != null)
      dataBaseBlock.connect();
  }

  public static void initDataBaseUsers(DataBaseConfig config) {
    if (dataBaseUsers != null) dataBaseUsers.disconnect();
    dataBaseUsers = switch (config.getType()) {
      case MONGODB -> new DataBaseUsersMongoDB();
      default -> new DataBaseUsersJson();
    };
    if (dataBaseUsers != null) dataBaseUsers.connect(config);
  }

  public static void close() {
    if (dataBaseBlock != null) dataBaseBlock.disconnect();
    if (dataBaseUsers != null) dataBaseUsers.disconnect();

  }
}
