package com.kingpixel.cobbleutils.Model;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

/**
 * @author Carlos Varas Alonso - 27/07/2024 13:13
 */
@Data
@ToString
@Builder
public class DataBaseConfig {
  @Builder.Default
  private DataBaseType type = DataBaseType.JSON;
  @Builder.Default
  private String database = "";
  @Builder.Default
  private String url = "";
  @Builder.Default
  private String user = "admin";
  @Builder.Default
  private String password = "admin";

  public DataBaseConfig() {
    this.type = DataBaseType.JSON;
    this.database = "CobbleUtils";
    this.url = "";
    this.user = "admin";
    this.password = "admin";
  }

  public DataBaseConfig(String database) {
    super();
    this.database = database;
  }

  public DataBaseConfig(DataBaseType type, String database, String url, String user, String password) {
    this.type = type;
    this.database = database;
    this.url = url;
    this.user = user;
    this.password = password;
  }
}
