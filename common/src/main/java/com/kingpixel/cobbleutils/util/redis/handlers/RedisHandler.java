package com.kingpixel.cobbleutils.util.redis.handlers;

import com.google.gson.JsonObject;

public interface RedisHandler {
  String getIdentifier();

  void handle(JsonObject json);
}