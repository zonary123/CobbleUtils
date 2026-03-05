package com.kingpixel.cobbleutils.util.redis;

import com.kingpixel.cobbleutils.util.redis.handlers.RedisMessageHandler;
import com.kingpixel.cobbleutils.util.redis.handlers.RedisTeleportHandler;

public class RedisRegistryHandlers {
  public static void registerHandlers() {
    RedisManager.registerHandler(new RedisMessageHandler());
    RedisManager.registerHandler(new RedisTeleportHandler());
  }
}
