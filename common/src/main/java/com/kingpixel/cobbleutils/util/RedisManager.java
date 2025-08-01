package com.kingpixel.cobbleutils.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kingpixel.cobbleutils.CobbleUtils;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class RedisManager {

    private static JedisPool jedisPool;
    private static JedisPubSub jedisPubSub;
    private static Thread subscriptionThread;
    private static final AtomicBoolean isConnected = new AtomicBoolean(false);
    private static final AtomicBoolean isSubscriberHealthy = new AtomicBoolean(false);
    private static final AtomicBoolean shouldReconnect = new AtomicBoolean(true);
    private static ScheduledExecutorService reconnectExecutor;
    private static final AtomicLong lastMessageReceived = new AtomicLong(System.currentTimeMillis());
    private static final AtomicLong messagesReceived = new AtomicLong(0);
    private static final AtomicLong messagesSent = new AtomicLong(0);

    private static final int CONNECTION_TIMEOUT = 10000;
    private static final int SO_TIMEOUT = 10000;
    private static final int SUBSCRIBER_TIMEOUT = 30000; // 30 segundos para subscriber
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final int RECONNECT_DELAY_SECONDS = 10;
    private static final int HEALTH_CHECK_INTERVAL = 15; // segundos

    public static void init() {
        if (!CobbleUtils.config.isRedisMessaging()) return;

        try {
            initializePool();
            subscribe();
            startHealthCheck();
            CobbleUtils.LOGGER.info("Redis connection initialized successfully.");
        } catch (Exception e) {
            CobbleUtils.LOGGER.error("Failed to initialize Redis connection: " + e.getMessage());
            e.printStackTrace();
        }
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
            isConnected.set(true);
            CobbleUtils.LOGGER.info("Redis pool initialized and tested successfully.");
        }
    }

    private static void startHealthCheck() {
        if (reconnectExecutor == null || reconnectExecutor.isShutdown()) {
            reconnectExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "Redis-HealthCheck-Thread");
                thread.setDaemon(true);
                return thread;
            });
        }

        reconnectExecutor.scheduleWithFixedDelay(() -> {
            if (!shouldReconnect.get()) return;

            checkPublisherHealth();
            checkSubscriberHealth();

        }, HEALTH_CHECK_INTERVAL, HEALTH_CHECK_INTERVAL, TimeUnit.SECONDS);
    }

    private static void checkPublisherHealth() {
        try {
            if (jedisPool != null && !jedisPool.isClosed()) {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.ping();
                    if (!isConnected.get()) {
                        isConnected.set(true);
                        CobbleUtils.LOGGER.info("Redis publisher connection restored.");
                    }
                }
            } else {
                throw new JedisConnectionException("Pool is closed");
            }
        } catch (Exception e) {
            if (isConnected.get()) {
                isConnected.set(false);
                CobbleUtils.LOGGER.warn("Redis publisher connection lost: " + e.getMessage());
            }
            attemptPublisherReconnection();
        }
    }

    private static void checkSubscriberHealth() {
        long timeSinceLastMessage = System.currentTimeMillis() - lastMessageReceived.get();
        boolean subscriberSeemsAlive = subscriptionThread != null &&
                subscriptionThread.isAlive() &&
                jedisPubSub != null &&
                jedisPubSub.isSubscribed();

        if (timeSinceLastMessage > 120000 && subscriberSeemsAlive) {
            CobbleUtils.LOGGER.warn("Subscriber appears stuck (no messages for " + (timeSinceLastMessage/1000) + " seconds). Forcing reconnection.");
            attemptSubscriberReconnection();
        } else if (!subscriberSeemsAlive) {
            if (isSubscriberHealthy.get()) {
                isSubscriberHealthy.set(false);
                CobbleUtils.LOGGER.warn("Redis subscriber connection lost. Attempting reconnection...");
            }
            attemptSubscriberReconnection();
        } else if (!isSubscriberHealthy.get() && subscriberSeemsAlive) {
            isSubscriberHealthy.set(true);
            CobbleUtils.LOGGER.info("Redis subscriber connection restored.");
        }
    }

    private static void attemptPublisherReconnection() {
        if (!shouldReconnect.get()) return;

        CobbleUtils.LOGGER.info("Attempting Redis publisher reconnection...");

        try {
            if (jedisPool != null && !jedisPool.isClosed()) {
                jedisPool.close();
            }

            Thread.sleep(3000);
            initializePool();

            CobbleUtils.LOGGER.info("Redis publisher reconnection successful.");

        } catch (Exception e) {
            CobbleUtils.LOGGER.error("Failed to reconnect Redis publisher: " + e.getMessage());
        }
    }

    private static void attemptSubscriberReconnection() {
        if (!shouldReconnect.get()) return;

        CobbleUtils.LOGGER.info("Attempting Redis subscriber reconnection...");

        try {
            if (jedisPubSub != null && jedisPubSub.isSubscribed()) {
                try {
                    jedisPubSub.unsubscribe();
                } catch (Exception ignored) {}
            }

            if (subscriptionThread != null && subscriptionThread.isAlive()) {
                subscriptionThread.interrupt();
                try {
                    subscriptionThread.join(5000);
                } catch (InterruptedException ignored) {}
            }

            // Recrear subscriber
            Thread.sleep(2000);
            subscribe();

            CobbleUtils.LOGGER.info("Redis subscriber reconnection completed.");

        } catch (Exception e) {
            CobbleUtils.LOGGER.error("Failed to reconnect Redis subscriber: " + e.getMessage());
        }
    }

    public static void close() {
        shouldReconnect.set(false);

        if (reconnectExecutor != null && !reconnectExecutor.isShutdown()) {
            reconnectExecutor.shutdown();
            try {
                if (!reconnectExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    reconnectExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                reconnectExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (jedisPubSub != null && jedisPubSub.isSubscribed()) {
            try {
                jedisPubSub.unsubscribe();
            } catch (Exception e) {
                CobbleUtils.LOGGER.warn("Error unsubscribing from Redis: " + e.getMessage());
            }
        }

        if (subscriptionThread != null && subscriptionThread.isAlive()) {
            subscriptionThread.interrupt();
            try {
                subscriptionThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (jedisPool != null && !jedisPool.isClosed()) {
            try {
                jedisPool.close();
                CobbleUtils.LOGGER.info("Redis connection closed.");
            } catch (Exception e) {
                CobbleUtils.LOGGER.error("Error closing Redis connection: " + e.getMessage());
            }
        }

        isConnected.set(false);
        isSubscriberHealthy.set(false);
    }

    private static void subscribe() {
        jedisPubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                try {
                    lastMessageReceived.set(System.currentTimeMillis());
                    messagesReceived.incrementAndGet();

                    if (CobbleUtils.config.isDebug()) {
                        CobbleUtils.LOGGER.info("Received Redis message on channel " + channel + ": " + message);
                    }

                    handleIncomingMessage(message);

                    if (!isSubscriberHealthy.get()) {
                        isSubscriberHealthy.set(true);
                        CobbleUtils.LOGGER.info("Redis subscriber is receiving messages again.");
                    }
                } catch (Exception e) {
                    CobbleUtils.LOGGER.error("Error handling Redis message: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            @Override
            public void onSubscribe(String channel, int subscribedChannels) {
                CobbleUtils.LOGGER.info("Successfully subscribed to Redis channel: " + channel);
                isSubscriberHealthy.set(true);
                lastMessageReceived.set(System.currentTimeMillis()); // Reset timer on successful subscription
            }

            @Override
            public void onUnsubscribe(String channel, int subscribedChannels) {
                CobbleUtils.LOGGER.info("Unsubscribed from Redis channel: " + channel);
                isSubscriberHealthy.set(false);
            }
        };

        subscriptionThread = new Thread(() -> {
            int attempts = 0;
            while (shouldReconnect.get() && attempts < MAX_RECONNECT_ATTEMPTS) {
                try {
                    Jedis subscriberJedis = new Jedis(
                            CobbleUtils.config.getRedis().getHost(),
                            CobbleUtils.config.getRedis().getPort(),
                            CONNECTION_TIMEOUT,
                            SUBSCRIBER_TIMEOUT
                    );

                    if (!CobbleUtils.config.getRedis().getPassword().isEmpty()) {
                        subscriberJedis.auth(CobbleUtils.config.getRedis().getPassword());
                    }

                    String channel = CobbleUtils.config.getRedis().getChannel();
                    CobbleUtils.LOGGER.info("Subscribing to Redis channel: " + channel + " (attempt " + (attempts + 1) + ")");

                    subscriberJedis.subscribe(jedisPubSub, channel);

                    subscriberJedis.close();
                    break;

                } catch (JedisException e) {
                    attempts++;
                    isSubscriberHealthy.set(false);
                    CobbleUtils.LOGGER.error("Redis subscription failed (attempt " + attempts + "): " + e.getMessage());

                    if (attempts < MAX_RECONNECT_ATTEMPTS && shouldReconnect.get()) {
                        try {
                            Thread.sleep(RECONNECT_DELAY_SECONDS * 1000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } catch (Exception e) {
                    CobbleUtils.LOGGER.error("Unexpected error in Redis subscription: " + e.getMessage());
                    e.printStackTrace();
                    break;
                }
            }

            if (attempts >= MAX_RECONNECT_ATTEMPTS) {
                CobbleUtils.LOGGER.error("Failed to subscribe to Redis after " + MAX_RECONNECT_ATTEMPTS + " attempts");
                isSubscriberHealthy.set(false);
            }

            CobbleUtils.LOGGER.warn("Redis subscription thread ending");
        }, "Redis-Subscription-Thread");

        subscriptionThread.setDaemon(true);
        subscriptionThread.start();
    }

    private static void handleIncomingMessage(String message) {
        try {
            if (CobbleUtils.server == null) {
                CobbleUtils.LOGGER.warn("Server is null, cannot handle Redis message");
                return;
            }

            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            String type = json.get("type").getAsString();
            String content = json.get("content").getAsString();

            String prefix = json.has("prefix") ? json.get("prefix").getAsString() : "";
            if (!prefix.isEmpty()) {
                content = content.replace("%prefix%", prefix);
            }

            Text formattedMessage = AdventureTranslator.toNative(content);

            switch (type) {
                case "broadcast":
                    CobbleUtils.server.getPlayerManager().broadcast(formattedMessage, false);
                    if (CobbleUtils.config.isDebug()) {
                        CobbleUtils.LOGGER.info("Broadcasted message to all players");
                    }
                    break;

                case "player":
                    if (!json.has("uuid")) {
                        CobbleUtils.LOGGER.warn("Player message missing UUID");
                        return;
                    }
                    UUID playerUUID = UUID.fromString(json.get("uuid").getAsString());
                    ServerPlayerEntity player = CobbleUtils.server.getPlayerManager().getPlayer(playerUUID);
                    if (player != null) {
                        player.sendMessage(formattedMessage);
                        if (CobbleUtils.config.isDebug()) {
                            CobbleUtils.LOGGER.info("Sent message to player: " + player.getGameProfile().getName());
                        }
                    } else {
                        CobbleUtils.LOGGER.warn("Player not found for UUID: " + playerUUID);
                    }
                    break;

                case "actionbar":
                    CobbleUtils.server.getPlayerManager().getPlayerList().forEach(p -> {
                        p.sendMessage(formattedMessage, true);
                    });
                    if (CobbleUtils.config.isDebug()) {
                        CobbleUtils.LOGGER.info("Sent actionbar message to all players");
                    }
                    break;

                case "actionbar_player":
                    if (!json.has("uuid")) {
                        CobbleUtils.LOGGER.warn("Actionbar player message missing UUID");
                        return;
                    }
                    UUID actionbarPlayerUUID = UUID.fromString(json.get("uuid").getAsString());
                    ServerPlayerEntity actionbarPlayer = CobbleUtils.server.getPlayerManager().getPlayer(actionbarPlayerUUID);
                    if (actionbarPlayer != null) {
                        actionbarPlayer.sendMessage(formattedMessage, true);
                        if (CobbleUtils.config.isDebug()) {
                            CobbleUtils.LOGGER.info("Sent actionbar message to player: " + actionbarPlayer.getGameProfile().getName());
                        }
                    } else {
                        CobbleUtils.LOGGER.warn("Player not found for actionbar UUID: " + actionbarPlayerUUID);
                    }
                    break;

                default:
                    CobbleUtils.LOGGER.warn("Unknown message type: " + type);
                    break;
            }
        } catch (Exception e) {
            CobbleUtils.LOGGER.error("Failed to handle incoming Redis message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Métodos públicos para enviar mensajes
    public static void sendMessage(String message) {
        publish("broadcast", message, null, "");
    }

    public static void sendMessage(UUID playerUUID, String message) {
        publish("player", message, playerUUID, "");
    }

    public static void sendMessage(String message, String prefix) {
        publish("broadcast", message, null, prefix);
    }

    public static void sendMessage(UUID playerUUID, String message, String prefix) {
        publish("player", message, playerUUID, prefix);
    }

    public static void sendActionBarMessage(String message, String prefix) {
        publish("actionbar", message, null, prefix);
    }

    public static void sendActionBarMessage(UUID playerUUID, String message, String prefix) {
        publish("actionbar_player", message, playerUUID, prefix);
    }

    private static void publish(String type, String message, UUID uuid, String prefix) {
        if (!isConnected.get()) {
            CobbleUtils.LOGGER.warn("Redis publisher is not connected, cannot send message. Message: " + message);
            return;
        }

        if (jedisPool == null || jedisPool.isClosed()) {
            CobbleUtils.LOGGER.warn("Redis pool is not available, cannot send message");
            isConnected.set(false);
            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            JsonObject json = new JsonObject();
            json.addProperty("type", type);
            json.addProperty("content", message);

            if (uuid != null) {
                json.addProperty("uuid", uuid.toString());
            }

            if (prefix != null && !prefix.isEmpty()) {
                json.addProperty("prefix", prefix);
            }

            String channel = CobbleUtils.config.getRedis().getChannel();
            String jsonMessage = json.toString();

            long result = jedis.publish(channel, jsonMessage);
            messagesSent.incrementAndGet();

            if (CobbleUtils.config.isDebug()) {
                CobbleUtils.LOGGER.info("Published message to " + result + " subscribers on channel: " + channel);
            }

        } catch (JedisException e) {
            CobbleUtils.LOGGER.error("Failed to publish Redis message (Redis error): " + e.getMessage());
            isConnected.set(false);
        } catch (Exception e) {
            CobbleUtils.LOGGER.error("Failed to publish Redis message (Unexpected error): " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean isHealthy() {
        return isConnected.get() && isSubscriberHealthy.get() && jedisPool != null && !jedisPool.isClosed();
    }

    public static String getPoolStats() {
        if (jedisPool == null) return "Pool not initialized";

        return String.format("Active: %d, Idle: %d, Waiters: %d",
                jedisPool.getNumActive(),
                jedisPool.getNumIdle(),
                jedisPool.getNumWaiters());
    }

    public static String getDetailedStats() {
        long timeSinceLastMessage = System.currentTimeMillis() - lastMessageReceived.get();
        return String.format(
                "Publisher: %s, Subscriber: %s, Pool: %s, Messages Sent: %d, Received: %d, Last Received: %ds ago",
                isConnected.get() ? "OK" : "FAIL",
                isSubscriberHealthy.get() ? "OK" : "FAIL",
                getPoolStats(),
                messagesSent.get(),
                messagesReceived.get(),
                timeSinceLastMessage / 1000
        );
    }
}