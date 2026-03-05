package com.kingpixel.cobbleutils.config;

import lombok.Data;

@Data
public class RedisConfig {
  private String host;
  private int port;
  private String password;
  private String channel;

  public RedisConfig() {
    this.host = "localhost";
    this.port = 6379;
    this.password = "";
    this.channel = "channel1";
  }
}
