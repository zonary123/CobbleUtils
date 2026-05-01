package com.kingpixel.cobbleutils.util.mongodb;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Central service that manages shared MongoDBManager instances.
 * <p>
 * Guarantees a single MongoClient per unique logical configuration
 * while avoiding credential leaks in logs and identifiers.
 *
 * <h2>Design goals</h2>
 * <ul>
 *   <li>Connection reuse across plugins</li>
 *   <li>No duplicate MongoClient instances</li>
 *   <li>Thread-safe initialization</li>
 *   <li>Safe logging (no credential leaks)</li>
 * </ul>
 *
 * <h2>Thread safety</h2>
 * Fully safe for concurrent plugin loading environments.
 *
 * @see MongoDBManager
 * @see DataBaseConfig
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MongoDBService {
  private static final String UNKNOWN_HOST = "unknown-host";

  /**
   * Active MongoDB managers indexed by a safe fingerprint key.
   */
  private static final Map<String, MongoDBManager> MANAGERS = new ConcurrentHashMap<>();

  /**
   * Retrieves or creates a shared MongoDBManager instance.
   */
  public static MongoDBManager getOrCreateManager(DataBaseConfig config) {
    Objects.requireNonNull(config, "DataBaseConfig cannot be null");

    Fingerprint fingerprint = buildFingerprint(config);

    try {
      return MANAGERS.compute(fingerprint.cacheKey(), (key, existing) -> {
        if (existing != null) {
          if (existing.isAlive()) {
            return existing;
          }
          CobbleUtils.LOGGER_RAW.warn("[MongoDB] Replacing stale manager: {}", fingerprint.logLabel());
          closeQuietly(existing, fingerprint.logLabel());
        }

        CobbleUtils.LOGGER_RAW.info("[MongoDB] Creating connection pool: {}", fingerprint.logLabel());

        MongoDBManager manager = new MongoDBManager(config);
        manager.init();

        if (!manager.isConnected()) {
          closeQuietly(manager, fingerprint.logLabel());
          throw new IllegalStateException("MongoDB manager created but healthcheck failed: " + fingerprint.logLabel());
        }

        CobbleUtils.LOGGER_RAW.info("[MongoDB] Connected: {}", fingerprint.logLabel());
        return manager;
      });
    } catch (RuntimeException e) {
      CobbleUtils.LOGGER_RAW.error("[MongoDB] Failed to initialize manager: {}", fingerprint.logLabel(), e);
      throw e;
    }
  }

  private static void closeQuietly(MongoDBManager manager, String logLabel) {
    if (manager == null) return;
    try {
      manager.close();
    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.warn("[MongoDB] Error while closing manager {}", logLabel, e);
    }
  }

  private record Fingerprint(String cacheKey, String logLabel) {
  }

  /**
   * Builds a SAFE identifier for logs and caching.
   */
  private static Fingerprint buildFingerprint(DataBaseConfig config) {
    String hostLabel = extractHostLabel(config.getUrl());
    String database = normalizeDatabase(config);

    // Keep credentials private by hashing them into the cache key.
    String cacheMaterial = String.join("|",
      normalizeUrl(config.getUrl()),
      normalize(config.getDatabase()),
      normalize(config.getUser()),
      normalize(config.getPassword())
    );

    String hash = sha256Hex(cacheMaterial);
    String shortHash = hash.substring(0, 12);

    String cacheKey = hostLabel + "::" + database + "::" + hash;
    String logLabel = hostLabel + "::" + database + "::" + shortHash;

    return new Fingerprint(cacheKey, logLabel);
  }

  /**
   * Extracts host list safely from MongoDB URI without credentials.
   */
  private static String extractHostLabel(String url) {
    if (url == null || url.isBlank()) return UNKNOWN_HOST;

    try {
      ConnectionString connectionString = new ConnectionString(url);
      var hosts = connectionString.getHosts();
      if (!hosts.isEmpty()) {
        return hosts.stream()
          .map(String::toLowerCase)
          .sorted()
          .collect(Collectors.joining(","));
      }
    } catch (Exception ignored) {
      // Fallback parser below.
    }

    try {
      String cleaned = url.replaceFirst("^mongodb(\\+srv)?://", "");
      int atIndex = cleaned.lastIndexOf('@');
      if (atIndex >= 0 && atIndex + 1 < cleaned.length()) cleaned = cleaned.substring(atIndex + 1);
      int slashIndex = cleaned.indexOf('/');
      String hosts = slashIndex >= 0 ? cleaned.substring(0, slashIndex) : cleaned;
      return hosts.isBlank() ? UNKNOWN_HOST : hosts.toLowerCase();
    } catch (Exception ignored) {
      return UNKNOWN_HOST;
    }
  }

  private static String normalizeDatabase(DataBaseConfig config) {
    String database = normalize(config.getDatabase());
    if (!database.isBlank()) return database;

    try {
      if (config.getUrl() != null && !config.getUrl().isBlank()) {
        String fromUrl = new ConnectionString(config.getUrl()).getDatabase();
        if (fromUrl != null && !fromUrl.isBlank()) return fromUrl;
      }
    } catch (Exception ignored) {
      // keep fallback
    }
    return "unknown";
  }

  /**
   * Normalizes URL for hashing without exposing sensitive info.
   */
  private static String normalizeUrl(String url) {
    if (url == null) return "";
    return url
      .replaceAll("://[^@]*+@", "://***@") // hides credentials
      .replaceAll("\\d+", "x");      // optional noise reduction
  }

  private static String normalize(String value) {
    return Objects.requireNonNullElse(value, "").trim();
  }

  private static String sha256Hex(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      // Should never happen on the JVM; fallback keeps deterministic behavior.
      return Integer.toHexString(Arrays.hashCode(value.getBytes(StandardCharsets.UTF_8)));
    }
  }

  /**
   * Gracefully shuts down all MongoDB connections.
   */
  public static void shutdown() {

    if (MANAGERS.isEmpty()) return;

    CobbleUtils.LOGGER_RAW.info("[MongoDB] Shutting down {} connections...", MANAGERS.size());

    MANAGERS.forEach((key, manager) -> {
      try {
        manager.close();
        CobbleUtils.LOGGER_RAW.info("[MongoDB] Closed: {}", key);
      } catch (Exception e) {
        CobbleUtils.LOGGER_RAW.error("[MongoDB] Close error ({})", key, e);
      }
    });

    MANAGERS.clear();
  }

  /**
   * Returns number of active MongoDB connections.
   */
  public static int getActiveConnections() {
    return MANAGERS.size();
  }

  /**
   * Returns a collection from the shared manager for this config.
   * Prefer this helper in downstream mods to keep Mongo access centralized in CobbleUtils.
   */
  public static MongoCollection<Document> getCollection(DataBaseConfig config, String collectionName) {
    Objects.requireNonNull(collectionName, "collectionName cannot be null");
    return getOrCreateManager(config).getCollection(collectionName);
  }

  /**
   * Runs a function against a collection asynchronously through the shared manager.
   */
  public static <T> CompletableFuture<T> withCollectionAsync(
    DataBaseConfig config,
    String collectionName,
    Function<MongoCollection<Document>, T> action
  ) {
    Objects.requireNonNull(action, "action cannot be null");
    return getOrCreateManager(config).withCollectionAsync(collectionName, action);
  }

  /**
   * Runs a consumer against a collection asynchronously through the shared manager.
   */
  public static CompletableFuture<Void> withCollectionAsync(
    DataBaseConfig config,
    String collectionName,
    Consumer<MongoCollection<Document>> action
  ) {
    Objects.requireNonNull(action, "action cannot be null");
    return getOrCreateManager(config).withCollectionAsync(collectionName, action);
  }
}