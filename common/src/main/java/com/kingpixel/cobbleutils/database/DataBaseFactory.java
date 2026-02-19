package com.kingpixel.cobbleutils.database;

import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.database.users.DataBaseUsers;
import com.kingpixel.cobbleutils.database.users.DataBaseUsersJson;
import com.kingpixel.cobbleutils.database.users.DataBaseUsersMongoDB;
import lombok.Data;

/**
 * @author Carlos Varas Alonso - 23/08/2025 7:35
 */
@Data
public class DataBaseFactory {
  public static DataBaseUsers dataBaseUsers;

  public static void init(DataBaseConfig config) {
    initDataBaseUsers(config);
  }

  public static void initDataBaseUsers(DataBaseConfig config) {
    if (dataBaseUsers != null) dataBaseUsers.disconnect();
    dataBaseUsers = switch (config.getType()) {
      case MONGODB -> new DataBaseUsersMongoDB();
      default -> new DataBaseUsersJson();
    };
    dataBaseUsers.connect(config);
  }

  public static void close() {
    if (dataBaseUsers != null) dataBaseUsers.disconnect();
  }
}
