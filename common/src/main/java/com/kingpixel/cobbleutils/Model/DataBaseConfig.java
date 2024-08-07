package com.kingpixel.cobbleutils.Model;

import lombok.Data;
import lombok.Getter;

/**
 * @author Carlos Varas Alonso - 27/07/2024 13:13
 */
@Getter
@Data
public class DataBaseConfig {
  private DataBaseType type;
  private String database;
  private String url;
  private String user;
  private String password;

  public DataBaseConfig() {
    this.type = DataBaseType.JSON;
    this.database = "CobbleUtils";
    this.url = "";
    this.user = "admin";
    this.password = "admin";
  }

  public DataBaseConfig(DataBaseType type, String database, String url, String user, String password) {
    this.type = type;
    this.database = database;
    this.url = url;
    this.user = user;
    this.password = password;
  }
}
