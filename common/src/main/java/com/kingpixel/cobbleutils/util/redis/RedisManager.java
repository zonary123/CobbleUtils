package com.kingpixel.cobbleutils.util.redis;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.redis.handlers.RedisHandler;
import lombok.Data;
import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Data
public class RedisManager {

  @Getter
  private static JedisPool jedisPool;
  private static JedisPubSub subscriber;
  private static Thread subscriberThread;

  @Getter
  private static final AtomicBoolean connected = new AtomicBoolean(false);

  @Getter
  private static final AtomicBoolean subscriberHealthy = new AtomicBoolean(false);

  private static final AtomicLong lastMessage = new AtomicLong(System.currentTimeMillis());

  private static final Map<String, RedisHandler> handlers = new ConcurrentHashMap<>();

  private static ScheduledExecutorService healthExecutor;

  private static final int CONNECTION_TIMEOUT = 10000;
  private static final int SO_TIMEOUT = 10000;

  public static void init() {
    if (!CobbleUtils.config.isRedisMessaging()) return;
    RedisRegistryHandlers.registerHandlers();
    initializePool();
    startSubscriber();
    startHealthCheck();
  }

  private static void initializePool() {
    JedisPoolConfig poolConfig = new JedisPoolConfig();

    poolConfig.setMaxTotal(30);
    poolConfig.setMaxIdle(15);
    poolConfig.setMinIdle(5);
    poolConfig.setTestOnBorrow(true);
    poolConfig.setTestOnReturn(true);
    poolConfig.setTestWhileIdle(true);
    poolConfig.setMinEvictableIdleTimeMillis(Duration.ofMinutes(2).toMillis());
    poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
    poolConfig.setNumTestsPerEvictionRun(5);
    poolConfig.setBlockWhenExhausted(true);
    poolConfig.setMaxWaitMillis(5000);

    jedisPool = new JedisPool(
      poolConfig,
      CobbleUtils.config.getRedis().getHost(),
      CobbleUtils.config.getRedis().getPort(),
      CONNECTION_TIMEOUT,
      SO_TIMEOUT,
      CobbleUtils.config.getRedis().getPassword().isEmpty() ? null : CobbleUtils.config.getRedis().getPassword(),
      0,
      null
    );

    try (Jedis jedis = jedisPool.getResource()) {
      jedis.ping();
      connected.set(true);
    }
  }

  public static void registerHandler(RedisHandler handler) {
    String prefix = CobbleUtils.config.getRedis().getChannel();
    String fullChannel = prefix + ":" + handler.getIdentifier();
    handlers.put(fullChannel, handler);
  }

  private static void startSubscriber() {
    subscriber = new JedisPubSub() {

      @Override
      public void onMessage(String channel, String message) {
        lastMessage.set(System.currentTimeMillis());

        RedisHandler handler = handlers.get(channel);
        if (handler != null) {
          JsonObject json = JsonParser.parseString(message).getAsJsonObject();
          handler.handle(json);
        }
      }

      @Override
      public void onSubscribe(String channel, int subscribedChannels) {
        subscriberHealthy.set(true);
      }

      @Override
      public void onUnsubscribe(String channel, int subscribedChannels) {
        subscriberHealthy.set(false);
      }
    };

    subscriberThread = new Thread(() -> {
      try (Jedis jedis = createSubscriberJedis()) {

        String prefix = CobbleUtils.config.getRedis().getChannel();

        String[] channels = handlers.keySet()
          .stream()
          .filter(key -> key.startsWith(prefix + ":"))
          .toArray(String[]::new);

        if (channels.length > 0) {
          jedis.subscribe(subscriber, channels);
        }

      } catch (Exception ignored) {
      }
    });

    subscriberThread.setDaemon(true);
    subscriberThread.start();
  }

  private static Jedis createSubscriberJedis() {
    Jedis jedis = new Jedis(
      CobbleUtils.config.getRedis().getHost(),
      CobbleUtils.config.getRedis().getPort(),
      CONNECTION_TIMEOUT,
      CONNECTION_TIMEOUT
    );

    if (!CobbleUtils.config.getRedis().getPassword().isEmpty()) {
      jedis.auth(CobbleUtils.config.getRedis().getPassword());
    }

    return jedis;
  }

  private static void startHealthCheck() {
    healthExecutor = Executors.newSingleThreadScheduledExecutor();
    healthExecutor.scheduleWithFixedDelay(() -> {
      if (jedisPool == null || jedisPool.isClosed()) return;

      try (Jedis jedis = jedisPool.getResource()) {
        jedis.ping();
        connected.set(true);
      } catch (Exception e) {
        connected.set(false);
      }

    }, 15, 15, TimeUnit.SECONDS);
  }

  public static void publish(String key, JsonObject json) {
    if (!connected.get()) return;
    if (jedisPool == null || jedisPool.isClosed()) return;

    String prefix = CobbleUtils.config.getRedis().getChannel();
    String fullChannel = prefix + ":" + key;

    try (Jedis jedis = jedisPool.getResource()) {
      jedis.publish(fullChannel, json.toString());
    } catch (Exception ignored) {
      connected.set(false);
    }
  }

  public static void close() {
    if (subscriber != null) {
      try {
        subscriber.unsubscribe();
      } catch (Exception ignored) {
      }
    }

    if (subscriberThread != null) {
      subscriberThread.interrupt();
    }

    if (healthExecutor != null) {
      healthExecutor.shutdownNow();
    }

    if (jedisPool != null) {
      jedisPool.close();
    }

    connected.set(false);
    subscriberHealthy.set(false);
  }
}