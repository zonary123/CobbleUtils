package com.kingpixel.cobbleutils.database;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.database.repository.UserRepository;
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
  /**
   * Active user repository. Type is the clean {@link UserRepository} interface —
   * callers should never need to cast this to a concrete implementation.
   *
   * @deprecated Use {@link #users()} for API access. Direct field access is kept
   *             for backward compatibility with internal code.
   */
  public static UserRepository dataBaseUsers;

  /**
   * Returns the active {@link UserRepository}.
   * Prefer this over accessing {@link #dataBaseUsers} directly.
   */
  public static UserRepository users() {
    return dataBaseUsers;
  }

  public static void init(DataBaseConfig config) {
    initDataBaseUsers(config);
  }

  public static void initDataBaseUsers(DataBaseConfig config) {
    if (dataBaseUsers instanceof DataBaseUsers db) db.disconnect();

    DataBaseUsers impl = switch (config.getType()) {
      case MONGODB -> new DataBaseUsersMongoDB();
      case MYSQL, SQLITE, MARIADB, H2 -> new DataBaseUsersSQL();
      default -> new DataBaseUsersJson();
    };

    impl.connect(config);
    dataBaseUsers = impl;
    CobbleUtils.LOGGER_RAW.info("Database initialized: " + config.getType());
  }
}
