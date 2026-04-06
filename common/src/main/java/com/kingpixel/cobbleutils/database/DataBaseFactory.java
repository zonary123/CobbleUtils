package com.kingpixel.cobbleutils.database;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.database.users.DataBaseUsers;
import com.kingpixel.cobbleutils.database.users.DataBaseUsersJson;
import com.kingpixel.cobbleutils.database.users.DataBaseUsersMongoDB;
import com.kingpixel.cobbleutils.database.users.DataBaseUsersSQL;
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
      case MYSQL, SQLITE, MARIADB, H2 -> new DataBaseUsersSQL();
      default -> new DataBaseUsersJson();
    };

    dataBaseUsers.connect(config);
    CobbleUtils.LOGGER_RAW.info("Database initialized: " + config.getType());
  }

}
