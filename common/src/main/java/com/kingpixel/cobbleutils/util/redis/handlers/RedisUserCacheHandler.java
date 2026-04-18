package com.kingpixel.cobbleutils.util.redis.handlers;

import com.google.gson.JsonObject;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.database.users.DataBaseUsers;

import java.util.UUID;

/**
 * Cross-server UserModel cache invalidation via Redis Pub/Sub.
 */
public class RedisUserCacheHandler implements RedisHandler {

  public static final String IDENTIFIER = "user-cache";

  @Override
  public String getIdentifier() {
    return IDENTIFIER;
  }

  @Override
  public void handle(JsonObject json) {
    try {
      String action = json.has("action") ? json.get("action").getAsString() : null;
      String origin = json.has("origin") ? json.get("origin").getAsString() : null;

      if (origin != null && origin.equals(DataBaseUsers.INSTANCE_ID)) {
        return;
      }

      if ("invalidate".equals(action) && json.has("uuid")) {
        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        DataBaseUsers.USERS.invalidate(uuid);
        if (CobbleUtils.config.isDebug()) {
          CobbleUtils.LOGGER_RAW.info("Cross-server cache invalidation for user: {}", uuid);
        }
      }
    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.error("Error handling user cache invalidation message", e);
    }
  }
}

