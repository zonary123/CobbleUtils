package com.kingpixel.cobbleutils.util.mongodb;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.util.async.AsyncContext;
import com.kingpixel.cobbleutils.util.async.UtilsAsync;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import org.bson.Document;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Instance-based MongoDB connection manager.
 * <p>
 * Wraps a single {@link MongoClient} that can be shared across multiple mods
 * connecting to the same MongoDB server. Use
 * {@link MongoDBService#getOrCreateManager(DataBaseConfig)}
 * to obtain an instance and avoid duplicate connections.
 *
 * <h3>Lifecycle</h3>
 * <ol>
 * <li>Create: {@code new MongoDBManager(config)}</li>
 * <li>Initialize: {@link #init()}</li>
 * <li>Use: {@link #getDatabase(String)}, {@link #getCollection(String)}</li>
 * <li>Shutdown: {@link #close()}</li>
 * </ol>
 *
 * <h3>Example</h3>
 * 
 * <pre>{@code
 * DataBaseConfig cfg = new DataBaseConfig(DataBaseType.MONGODB, "mydb", "mongodb://localhost:27017", "", "");
 * MongoDBManager mgr = MongoDBService.getOrCreateManager(cfg);
 * MongoCollection<Document> col = mgr.getCollection("players");
 * // on shutdown:
 * MongoDBService.shutdown();
 * }</pre>
 *
 * @see MongoDBService
 * @see DataBaseConfig
 */
public class MongoDBManager {

  private static final String PING_DB = "admin";
  private static final String MONGO_ASYNC_CONTEXT_ID = "cobbleutils-mongodb";
  private static final String MONGO_ASYNC_THREAD_NAME = "MongoDB-IO";

  private final DataBaseConfig config;
  private final Object lifecycleLock = new Object();

  private final AtomicReference<MongoClient> mongoClient = new AtomicReference<>();

  private final AtomicReference<MongoDatabase> defaultDatabase = new AtomicReference<>();

  /**
   * Whether this manager has an active connection to MongoDB.
   */
  @Getter
  private volatile boolean connected;

  /**
   * Creates a new manager bound to the given configuration.
   * Call {@link #init()} afterwards to establish the connection.
   *
   * @param config Connection parameters (url, database).
   */
  public MongoDBManager(DataBaseConfig config) {
    this.config = config;
  }

  /**
   * Initializes the {@link MongoClient} and verifies connectivity
   * by issuing a {@code ping} command against the {@code admin} database.
   */
  public void init() {
    synchronized (lifecycleLock) {
      if (this.connected && this.mongoClient.get() != null) {
        return;
      }
      if (isAlive()) {
        return;
      }

      MongoClient newClient = null;
      try {
        String url = Objects.requireNonNull(config.getUrl(), "MongoDB URL cannot be null");
        MongoClientSettings settings = MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString(url))
            .applicationName("CobbleUtils-MongoDBManager")
            .applyToConnectionPoolSettings(pool -> {
              pool.maxSize(50);
              pool.minSize(0);
              pool.maxWaitTime(5, TimeUnit.SECONDS);
            })
            .applyToClusterSettings(cluster -> cluster.serverSelectionTimeout(5, TimeUnit.SECONDS))
            .build();

        newClient = MongoClients.create(settings);
        String databaseName = resolveDatabaseName(config);
        MongoDatabase newDefaultDatabase = newClient.getDatabase(databaseName);

        // Healthcheck before publishing client/database references.
        newClient.getDatabase(PING_DB).runCommand(new Document("ping", 1));

        this.mongoClient.set(newClient);
        this.defaultDatabase.set(newDefaultDatabase);
        this.connected = true;
        if (CobbleUtils.LOGGER_RAW.isInfoEnabled()) {
          CobbleUtils.LOGGER_RAW.info("[MongoDB] Connected successfully to {} / {}",
              sanitizeForLog(config.getUrl()),
              databaseName);
        }
      } catch (Exception e) {
        this.connected = false;
        if (newClient != null) {
          try {
            newClient.close();
          } catch (Exception closeException) {
            CobbleUtils.LOGGER_RAW.warn("[MongoDB] Failed to cleanup client after init failure", closeException);
          }
        }
        throw new IllegalStateException("Could not connect to MongoDB", e);
      }
    }
  }

  /**
   * Returns a {@link MongoDatabase} for the given name using the shared client.
   *
   * @param databaseName The database name.
   * @return The {@link MongoDatabase} instance.
   * @throws IllegalStateException if the manager has not been initialized.
   */
  public MongoDatabase getDatabase(String databaseName) {
    MongoClient client = this.mongoClient.get();
    if (client == null)
      throw new IllegalStateException("MongoDBManager not initialized. Call init() first.");
    return client.getDatabase(databaseName);
  }

  /**
   * Returns a {@link MongoCollection} from the default database.
   *
   * @param collectionName The collection name.
   * @return The {@link MongoCollection} instance.
   * @throws IllegalStateException if the manager has not been initialized.
   */
  public MongoCollection<Document> getCollection(String collectionName) {
    MongoDatabase db = this.defaultDatabase.get();
    if (db == null)
      throw new IllegalStateException("MongoDBManager not initialized. Call init() first.");
    return db.getCollection(collectionName);
  }

  /**
   * Returns a {@link MongoCollection} from a specific database.
   *
   * @param databaseName   The database name.
   * @param collectionName The collection name.
   * @return The {@link MongoCollection} instance.
   */
  public MongoCollection<Document> getCollection(String databaseName, String collectionName) {
    return getDatabase(databaseName).getCollection(collectionName);
  }

  /**
   * Checks if the connection is alive by pinging the server.
   *
   * @return {@code true} if the connection is active.
   */
  public boolean isAlive() {
    MongoClient client = this.mongoClient.get();
    if (client == null) {
      this.connected = false;
      return false;
    }

    try {
      client.getDatabase(PING_DB).runCommand(new Document("ping", 1));
      this.connected = true;
      return true;
    } catch (Exception e) {
      this.connected = false;
      CobbleUtils.LOGGER_RAW.warn("[MongoDB] Healthcheck failed for {}", sanitizeForLog(config.getUrl()), e);
      return false;
    }
  }

  /**
   * Runs a blocking Mongo operation asynchronously using a shared IO context.
   */
  public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
    Objects.requireNonNull(supplier, "supplier cannot be null");
    return getMongoAsyncContext().supply(supplier);
  }

  /**
   * Runs a blocking Mongo operation asynchronously with custom timeout.
   */
  public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier, long timeout, TimeUnit unit) {
    Objects.requireNonNull(supplier, "supplier cannot be null");
    Objects.requireNonNull(unit, "unit cannot be null");
    return getMongoAsyncContext().supply(supplier, timeout, unit);
  }

  /**
   * Runs a void Mongo task asynchronously using the shared IO context.
   */
  public CompletableFuture<Void> runAsync(Runnable runnable) {
    Objects.requireNonNull(runnable, "runnable cannot be null");
    return getMongoAsyncContext().runAsync(runnable);
  }

  /**
   * Executes a function against the default database asynchronously.
   */
  public <T> CompletableFuture<T> withDatabaseAsync(Function<MongoDatabase, T> action) {
    Objects.requireNonNull(action, "action cannot be null");
    return supplyAsync(() -> action.apply(requireDefaultDatabase()));
  }

  /**
   * Executes a consumer against the default database asynchronously.
   */
  public CompletableFuture<Void> withDatabaseAsync(Consumer<MongoDatabase> action) {
    Objects.requireNonNull(action, "action cannot be null");
    return runAsync(() -> action.accept(requireDefaultDatabase()));
  }

  /**
   * Executes a function against a collection of the default database
   * asynchronously.
   */
  public <T> CompletableFuture<T> withCollectionAsync(String collectionName,
      Function<MongoCollection<Document>, T> action) {
    Objects.requireNonNull(collectionName, "collectionName cannot be null");
    Objects.requireNonNull(action, "action cannot be null");
    return supplyAsync(() -> action.apply(getCollection(collectionName)));
  }

  /**
   * Executes a consumer against a collection of the default database
   * asynchronously.
   */
  public CompletableFuture<Void> withCollectionAsync(String collectionName,
      Consumer<MongoCollection<Document>> action) {
    Objects.requireNonNull(collectionName, "collectionName cannot be null");
    Objects.requireNonNull(action, "action cannot be null");
    return runAsync(() -> action.accept(getCollection(collectionName)));
  }

  /**
   * Gracefully closes the {@link MongoClient} and releases all resources.
   */
  public void close() {
    synchronized (lifecycleLock) {
      this.connected = false;
      MongoClient client = this.mongoClient.get();
      if (client != null) {
        try {
          client.close();
        } catch (Exception e) {
          CobbleUtils.LOGGER_RAW.error("[MongoDB] Error closing MongoDB connection for {}",
              sanitizeForLog(config.getUrl()), e);
        }
      }
      this.mongoClient.set(null);
      this.defaultDatabase.set(null);
    }
  }

  private static String resolveDatabaseName(DataBaseConfig config) {
    String configured = config.getDatabase();
    if (configured != null && !configured.isBlank())
      return configured;

    try {
      String fromUrl = new ConnectionString(config.getUrl()).getDatabase();
      if (fromUrl != null && !fromUrl.isBlank())
        return fromUrl;
    } catch (Exception ignored) {
    }
    return PING_DB;
  }

  private static String sanitizeForLog(String url) {
    if (url == null)
      return "unknown";
    return url.replaceAll("://[^@]*+@", "://***@");
  }

  private static AsyncContext getMongoAsyncContext() {
    return UtilsAsync.createContext(MONGO_ASYNC_CONTEXT_ID, MONGO_ASYNC_THREAD_NAME, 2, 4);
  }

  private MongoDatabase requireDefaultDatabase() {
    MongoDatabase db = this.defaultDatabase.get();
    if (db == null)
      throw new IllegalStateException("MongoDBManager not initialized. Call init() first.");
    return db;
  }
}
