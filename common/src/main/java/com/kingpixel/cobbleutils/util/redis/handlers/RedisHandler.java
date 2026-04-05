package com.kingpixel.cobbleutils.util.redis.handlers;

import com.google.gson.JsonObject;

/**
 * Contract for handling incoming Redis Pub/Sub messages.
 * <p>
 * Each handler is associated with a unique channel identifier.
 * When a message arrives on that channel, {@link #handle(JsonObject)} is invoked
 * with the parsed JSON payload.
 *
 * <h3>Example</h3>
 * <pre>{@code
 * public class MyHandler implements RedisHandler {
 *   @Override
 *   public String getIdentifier() {
 *     return "mymod:events";
 *   }
 *
 *   @Override
 *   public void handle(JsonObject json) {
 *     String type = json.get("type").getAsString();
 *     // process message...
 *   }
 * }
 * }</pre>
 *
 * @see com.kingpixel.cobbleutils.util.redis.RedisManager#registerHandler(RedisHandler)
 */
public interface RedisHandler {

  /**
   * Returns the unique channel identifier for this handler.
   * The full Redis channel is constructed as: {@code <config.channel>:<identifier>}.
   *
   * @return A non-null, unique string identifying the channel this handler listens to.
   */
  String getIdentifier();

  /**
   * Called when a message is received on this handler's channel.
   *
   * @param json The parsed JSON payload from the Redis message.
   */
  void handle(JsonObject json);
}