package com.kingpixel.cobbleutils.util.redis;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.redis.handlers.RedisHandler;
import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Instance-based Redis connection manager.
 * <p>
 * Each {@code RedisManager} owns its own {@link JedisPool}, subscriber thread,
 * and health-check executor. Multiple mods sharing the same host:port should
 * obtain their manager via {@link RedisService#getOrCreateManager(RedisConfig)}
 * to avoid duplicate connections.
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>Create: {@code new RedisManager(config)}</li>
 *   <li>Initialize pool and health-check: {@link #init()}</li>
 *   <li>Register handlers (can be done before or after init): {@link #registerHandler(RedisHandler)}</li>
 *   <li>Publish messages: {@link #publish(String, JsonObject)}</li>
 *   <li>Shutdown: {@link #close()}</li>
 * </ol>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * RedisConfig cfg = new RedisConfig("localhost", 6379, "", "mymod");
 * RedisManager mgr = new RedisManager(cfg);
 * mgr.init();
 * mgr.registerHandler(new MyHandler());
 * mgr.publish("my-channel", myJson);
 * // on shutdown:
 * mgr.close();
 * }</pre>
 *
 * @see RedisConfig
 * @see RedisService
 * @see RedisHandler
 */
public class RedisManager {

  private final RedisConfig redisConfig;
  @Getter
  private volatile JedisPool jedisPool;
  private volatile JedisPubSub subscriber;
  private volatile Thread subscriberThread;

  /**
   * Whether this manager has an active connection to the Redis server.
   */
  @Getter
  private final AtomicBoolean connected = new AtomicBoolean(false);
  private final Map<String, RedisHandler> handlers = new ConcurrentHashMap<>();
  private ScheduledExecutorService healthExecutor;

  private static final int TIMEOUT = 5_000;
  private static final int HEALTH_CHECK_INTERVAL = 30;
  private static final int RECONNECT_DELAY = 5_000;

  /**
   * Creates a new manager bound to the given configuration.
   * Call {@link #init()} afterwards to establish the connection pool.
   *
   * @param redisConfig Connection parameters (host, port, password, channel, etc.).
   */
  public RedisManager(RedisConfig redisConfig) {
    this.redisConfig = redisConfig;
  }

  /**
   * Initializes the Jedis connection pool and starts a periodic health-check.
   * This method must be called once before any publish/subscribe operations.
   */
  public void init() {
    initializePool();
    startHealthCheck();
  }

  /**
   * Configures and creates the {@link JedisPool} using settings from
   * {@link RedisConfig}. Performs an initial {@code PING} to verify connectivity.
   */
  private void initializePool() {
    int maxConn = redisConfig.getMaxConnections();
    JedisPoolConfig poolConfig = new JedisPoolConfig();
    poolConfig.setMaxTotal(maxConn);
    poolConfig.setMaxIdle(Math.max(maxConn / 2, 1));
    poolConfig.setMinIdle(Math.min(2, maxConn));
    poolConfig.setTestOnBorrow(true);
    poolConfig.setBlockWhenExhausted(true);

    String password = redisConfig.getPassword();
    this.jedisPool = new JedisPool(
      poolConfig,
      redisConfig.getHost(),
      redisConfig.getPort(),
      TIMEOUT,
      (password == null || password.isEmpty()) ? null : password
    );

    try (Jedis jedis = jedisPool.getResource()) {
      jedis.ping();
      connected.set(true);
    } catch (Exception e) {
      connected.set(false);
      CobbleUtils.LOGGER_RAW.error("Could not connect to Redis at {}:{}", redisConfig.getHost(), redisConfig.getPort());
      throw new RuntimeException(e);
    }
  }

  /**
   * Registers a {@link RedisHandler} for a specific channel.
   * <p>
   * The full channel name is built as: {@code <config.channel>:<handler.identifier>}.
   * <p>
   * If the subscriber thread is already active, the new channel is subscribed
   * in-flight without restarting the thread. Otherwise, a new subscriber thread
   * is started automatically.
   *
   * @param handler The handler to register. Must provide a unique identifier.
   */
  public void registerHandler(RedisHandler handler) {
    String fullChannel = redisConfig.getChannel() + ":" + handler.getIdentifier();
    handlers.put(fullChannel, handler);

    JedisPubSub sub = this.subscriber;
    if (sub != null && sub.isSubscribed()) {
      CompletableFuture.runAsync(() -> sub.subscribe(fullChannel));
    } else {
      startSubscriber();
    }
  }

  /**
   * Starts the subscriber thread that listens for incoming Redis Pub/Sub messages.
   * <p>
   * The thread automatically reconnects with a 5-second delay if the connection
   * drops. It subscribes to all channels registered in {@link #handlers}.
   * The thread is marked as daemon so it won't prevent JVM shutdown.
   */
  private synchronized void startSubscriber() {
    if (subscriberThread != null && subscriberThread.isAlive()) return;

    subscriber = new JedisPubSub() {
      @Override
      public void onMessage(String channel, String message) {
        RedisHandler handler = handlers.get(channel);
        if (handler == null) return;
        try {
          handler.handle(JsonParser.parseString(message).getAsJsonObject());
        } catch (Exception e) {
          CobbleUtils.LOGGER_RAW.error("Error in RedisHandler for channel {}", channel, e);
        }
      }
    };

    subscriberThread = new Thread(() -> {
      while (!Thread.currentThread().isInterrupted() && !jedisPool.isClosed()) {
        try (Jedis jedis = jedisPool.getResource()) {
          String[] channels = handlers.keySet().toArray(new String[0]);
          if (channels.length > 0) {
            jedis.subscribe(subscriber, channels);
          } else {
            Thread.sleep(2000);
          }
        } catch (Exception e) {
          if (Thread.currentThread().isInterrupted()) break;
          CobbleUtils.LOGGER_RAW.warn("Redis subscriber connection lost. Retrying in 5s...");
          reconnectSubscriberWithBackoff();
        }
      }
    }, "Redis-Subscriber-" + redisConfig.getHost());

    subscriberThread.setDaemon(true);
    subscriberThread.start();
  }

  private void reconnectSubscriberWithBackoff() {
    try {
      Thread.sleep(RECONNECT_DELAY);
    } catch (InterruptedException interruptedException) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Starts a periodic health-check that pings the Redis server every
   * {@value #HEALTH_CHECK_INTERVAL} seconds and updates the {@link #connected}
   * flag accordingly. Uses a daemon thread to avoid blocking JVM shutdown.
   */
  private void startHealthCheck() {
    healthExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "Redis-HealthCheck-" + redisConfig.getHost());
      t.setDaemon(true);
      return t;
    });
    healthExecutor.scheduleWithFixedDelay(() -> {
      if (jedisPool == null || jedisPool.isClosed()) return;
      try (Jedis jedis = jedisPool.getResource()) {
        connected.set("PONG".equalsIgnoreCase(jedis.ping()));
      } catch (Exception e) {
        connected.set(false);
      }
    }, HEALTH_CHECK_INTERVAL, HEALTH_CHECK_INTERVAL, TimeUnit.SECONDS);
  }

  /**
   * Publishes a JSON message to a Redis Pub/Sub channel.
   * <p>
   * The full channel name is: {@code <config.channel>:<key>}.
   * If the connection is down, the message is silently dropped.
   *
   * @param key  The channel suffix (e.g. "cobbleutils:messages").
   * @param json The JSON payload to publish.
   */
  public void publish(String key, JsonObject json) {
    if (!connected.get()) return;
    execute(jedis -> jedis.publish(redisConfig.getChannel() + ":" + key, json.toString()));
  }

  /**
   * Persists a JSON state in Redis under the key {@code state:<key>}.
   * Useful for sharing state across servers (e.g. spawn data, player locations).
   *
   * @param key  The state identifier.
   * @param json The JSON payload to store.
   */
  public void saveState(String key, JsonObject json) {
    if (!connected.get()) return;
    execute(jedis -> jedis.set("state:" + key, json.toString()));
  }

  /**
   * Persists a JSON state in Redis under the key {@code state:<key>} with an expiration time.
   *
   * @param key     The state identifier.
   * @param json    The JSON payload to store.
   * @param seconds Expiration time in seconds.
   */
  public void saveState(String key, JsonObject json, int seconds) {
    if (!connected.get()) return;
    execute(jedis -> jedis.setex("state:" + key, seconds, json.toString()));
  }

  /**
   * Deletes a previously saved JSON state from Redis.
   *
   * @param key The state identifier.
   */
  public void deleteState(String key) {
    if (!connected.get()) return;
    execute(jedis -> jedis.del("state:" + key));
  }

  /**
   * Retrieves a previously saved JSON state from Redis.
   *
   * @param key The state identifier.
   * @return The stored {@link JsonObject}, or {@code null} if not found or disconnected.
   */
  public JsonObject getState(String key) {
    if (!connected.get()) return null;
    String data = execute(jedis -> jedis.get("state:" + key));
    if (data == null) return null;
    try {
      return JsonParser.parseString(data).getAsJsonObject();
    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.error("Failed to parse Redis state for key: {}", key, e);
      return null;
    }
  }

  /**
   * Executes a task using a pooled Jedis connection.
   * Automatically returns the connection to the pool after use.
   * Sets {@link #connected} to {@code false} on failure.
   *
   * @param task The task to execute with a Jedis instance.
   * @param <T>  The return type of the task.
   * @return The task result, or {@code null} if the pool is closed or an error occurs.
   */
  private <T> T execute(RedisTask<T> task) {
    JedisPool pool = this.jedisPool;
    if (pool == null || pool.isClosed()) return null;
    try (Jedis jedis = pool.getResource()) {
      return task.run(jedis);
    } catch (Exception e) {
      connected.set(false);
      CobbleUtils.LOGGER_RAW.error("Redis execute error", e);
      return null;
    }
  }

  /**
   * Functional interface for executing operations on a pooled {@link Jedis} connection.
   *
   * @param <T> The return type.
   */
  @FunctionalInterface
  interface RedisTask<T> {
    T run(Jedis jedis);
  }

  /**
   * Gracefully shuts down this manager by:
   * <ol>
   *   <li>Unsubscribing the Pub/Sub listener</li>
   *   <li>Interrupting the subscriber thread</li>
   *   <li>Shutting down the health-check executor</li>
   *   <li>Closing the Jedis connection pool</li>
   * </ol>
   */
  public void close() {
    connected.set(false);

    JedisPubSub sub = this.subscriber;
    if (sub != null && sub.isSubscribed()) {
      try {
        sub.unsubscribe();
      } catch (Exception ignored) {
      }
    }

    Thread subThread = this.subscriberThread;
    if (subThread != null) {
      subThread.interrupt();
      try {
        subThread.join(3000);
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      }
    }

    if (healthExecutor != null) {
      healthExecutor.shutdownNow();
      try {
        healthExecutor.awaitTermination(3, TimeUnit.SECONDS);
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      }
    }

    JedisPool pool = this.jedisPool;
    if (pool != null && !pool.isClosed()) {
      pool.close();
    }
  }
}
