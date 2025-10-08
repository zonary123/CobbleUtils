package com.kingpixel.cobbleutils.database.blocks;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.events.models.EventBlockBreak;
import com.kingpixel.cobbleutils.events.models.EventBlockPlaced;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.*;

@EqualsAndHashCode(callSuper = true) @Data
public class DataBaseBlockSQLite extends DataBaseBlock {

  private Connection connection;
  private final ConcurrentHashMap<BlockPos, BlockData> blockCache = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
    new ThreadFactoryBuilder().setNameFormat("CobbleUtils-DB-DelayScheduler-%d").build()
  );

  // Cachés de mundos y chunks
  private final ConcurrentHashMap<String, Integer> worldCache = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Integer> chunkCache = new ConcurrentHashMap<>();

  public DataBaseBlockSQLite(DataBaseConfig config) {
    connect();
    startBatchUpdateTask();
  }

  public void connect() {
    try {
      Class.forName("org.sqlite.JDBC");
      if (connection == null || connection.isClosed()) {
        connection = DriverManager.getConnection("jdbc:sqlite:./config/cobbleutils/blocks.db");
        CobbleUtils.LOGGER.info(CobbleUtils.MOD_ID, "Connected to the database of blocks");

        try (var stmt = connection.createStatement()) {
          stmt.execute("PRAGMA journal_mode = WAL;");
          stmt.execute("PRAGMA synchronous = NORMAL;");
          stmt.execute("PRAGMA temp_store = MEMORY;");
          stmt.execute("PRAGMA cache_size = -32000;");
        }

        createTablesAndIndexes();
      }
    } catch (SQLException e) {
      CobbleUtils.LOGGER.error(CobbleUtils.MOD_ID, "Failed to connect to database" + e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void disconnect() {
    try {
      if (connection != null && !connection.isClosed()) {
        insertBlocks();
        CobbleUtils.shutdownAndAwait(scheduler);
        connection.close();
        CobbleUtils.LOGGER.info(CobbleUtils.MOD_ID, "Disconnected from the database of blocks");
      }
    } catch (SQLException e) {
      CobbleUtils.LOGGER.error(CobbleUtils.MOD_ID, "Failed to close database connection" + e);
    }
  }

  @SuppressWarnings("All")
  private void createTablesAndIndexes() {
    String createWorldsTable = """
      CREATE TABLE IF NOT EXISTS worlds (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          world_id TEXT NOT NULL UNIQUE
      );
      """;

    String createChunksTable = """
      CREATE TABLE IF NOT EXISTS chunks (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          world_id INTEGER NOT NULL,
          chunk_x INTEGER NOT NULL,
          chunk_z INTEGER NOT NULL,
          FOREIGN KEY (world_id) REFERENCES worlds(id),
          UNIQUE (world_id, chunk_x, chunk_z)
      );
      """;

    String createBlocksTable = """
      CREATE TABLE IF NOT EXISTS blocks (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          chunk_id INTEGER NOT NULL,
          block_x INTEGER NOT NULL,
          block_y INTEGER NOT NULL,
          block_z INTEGER NOT NULL,
          player_uuid TEXT NOT NULL,
          FOREIGN KEY (chunk_id) REFERENCES chunks(id)
      );
      """;

    String createIndexes = """
      CREATE INDEX if NOT EXISTS idx_world_id ON worlds (world_id);
      CREATE INDEX if NOT EXISTS idx_chunk ON chunks (world_id, chunk_x, chunk_z);
      CREATE INDEX if NOT EXISTS idx_block ON blocks (chunk_id, block_x, block_y, block_z);
      """;

    try (var stmt = connection.createStatement()) {
      stmt.execute(createWorldsTable);
      stmt.execute(createChunksTable);
      stmt.execute(createBlocksTable);
      stmt.execute(createIndexes);
    } catch (SQLException e) {
      CobbleUtils.LOGGER.error(CobbleUtils.MOD_ID, "Failed to create tables or indexes" + e);
    }
  }

  // -------------------------
  // CACHE OPTIMIZADA
  // -------------------------

  private CompletableFuture<Integer> getOrInsertWorldAsync(String worldId) {
    Integer cached = worldCache.get(worldId);
    if (cached != null) return CompletableFuture.completedFuture(cached);

    return CompletableFuture.supplyAsync(() -> {
      try {
        int id = getOrInsert("worlds", "world_id", worldId);
        worldCache.put(worldId, id);
        return id;
      } catch (SQLException e) {
        CobbleUtils.LOGGER.error(CobbleUtils.MOD_ID, "Failed to insert or retrieve world ID " + e);
        throw new RuntimeException(e);
      }
    }, DataBaseBlock.DB_THREAD_FACTORY);
  }

  private CompletableFuture<Integer> getOrInsertChunkAsync(int worldId, int chunkX, int chunkZ) {
    String key = worldId + ":" + chunkX + ":" + chunkZ;
    Integer cached = chunkCache.get(key);
    if (cached != null) return CompletableFuture.completedFuture(cached);

    return CompletableFuture.supplyAsync(() -> {
      try {
        int id = getOrInsertChunk(worldId, chunkX, chunkZ);
        chunkCache.put(key, id);
        return id;
      } catch (SQLException e) {
        CobbleUtils.LOGGER.error(CobbleUtils.MOD_ID, "Failed to insert or retrieve chunk ID " + e);
        throw new RuntimeException(e);
      }
    }, DataBaseBlock.DB_THREAD_FACTORY);
  }

  private int getOrInsert(String table, String column, String value) throws SQLException {
    String insertQuery = "INSERT OR IGNORE INTO " + table + " (" + column + ") VALUES (?);";
    String selectQuery = "SELECT id FROM " + table + " WHERE " + column + " = ?;";

    try (var insertStmt = connection.prepareStatement(insertQuery);
         var selectStmt = connection.prepareStatement(selectQuery)) {
      insertStmt.setString(1, value);
      insertStmt.executeUpdate();

      selectStmt.setString(1, value);
      var rs = selectStmt.executeQuery();
      if (rs.next()) return rs.getInt("id");
    }
    throw new SQLException("Failed to insert or retrieve ID from " + table);
  }

  private int getOrInsertChunk(int worldId, int chunkX, int chunkZ) throws SQLException {
    String insertQuery = "INSERT OR IGNORE INTO chunks (world_id, chunk_x, chunk_z) VALUES (?, ?, ?);";
    String selectQuery = "SELECT id FROM chunks WHERE world_id = ? AND chunk_x = ? AND chunk_z = ?;";

    try (var insertStmt = connection.prepareStatement(insertQuery);
         var selectStmt = connection.prepareStatement(selectQuery)) {
      insertStmt.setInt(1, worldId);
      insertStmt.setInt(2, chunkX);
      insertStmt.setInt(3, chunkZ);
      insertStmt.executeUpdate();

      selectStmt.setInt(1, worldId);
      selectStmt.setInt(2, chunkX);
      selectStmt.setInt(3, chunkZ);
      var rs = selectStmt.executeQuery();
      if (rs.next()) return rs.getInt("id");
    }
    throw new SQLException("Failed to insert or retrieve chunk ID");
  }

  // -------------------------
  // BLOQUES
  // -------------------------

  @Override
  public void placeBlock(World world, BlockPos pos, BlockState state, ServerPlayerEntity player) {
    final String worldId = world.getRegistryKey().getValue().toString();
    final int chunkX = pos.getX() >> 4;
    final int chunkZ = pos.getZ() >> 4;
    final String playerUuid = (player != null) ? player.getUuidAsString() : "NONE";

    getOrInsertWorldAsync(worldId)
      .thenCompose(worldDbId -> getOrInsertChunkAsync(worldDbId, chunkX, chunkZ))
      .thenAccept(chunkDbId -> {
        BlockData blockData = new BlockData(chunkDbId, pos.getX(), pos.getY(), pos.getZ(), playerUuid);
        blockCache.put(pos, blockData);

        CobbleUtilsEvents.BLOCK_PLACED_EVENT.emit(
          new EventBlockPlaced(world, pos, state, player, blockData, player != null)
        );

        if (CobbleUtils.config.isDebug())
          CobbleUtils.LOGGER.info(CobbleUtils.MOD_ID, "Block cached & event emitted at " + pos);
      })
      .exceptionally(e -> {
        CobbleUtils.LOGGER.error(CobbleUtils.MOD_ID,
          "Failed to cache block at " + pos + " " + e);
        return null;
      });
  }

  @Override
  public void removeBlock(World world, BlockPos pos, BlockState state, ServerPlayerEntity player) {
    boolean wasPlayerPlaced = isBlockPlaceByPlayer(world, pos);
    if (blockCache.remove(pos) == null) removeBlockFromDatabaseAsync(world, pos);

    CobbleUtilsEvents.BLOCK_BREAK_EVENT.emit(
      new EventBlockBreak(world, pos, state, player, wasPlayerPlaced)
    );

    if (CobbleUtils.config.isDebug())
      CobbleUtils.LOGGER.info(CobbleUtils.MOD_ID, "Block removed & event emitted: " + pos);
  }

  private void removeBlockFromDatabaseAsync(World world, BlockPos pos) {
    CompletableFuture.runAsync(() -> removeBlockFromDatabase(world, pos), DataBaseBlock.DB_THREAD_FACTORY);
  }

  private void removeBlockFromDatabase(World world, BlockPos pos) {
    String worldId = world.getRegistryKey().getValue().toString();
    int chunkX = pos.getX() >> 4;
    int chunkZ = pos.getZ() >> 4;

    String query = """
      DELETE FROM blocks
      WHERE chunk_id = (SELECT id FROM chunks WHERE world_id = (SELECT id FROM worlds WHERE world_id = ?) AND chunk_x = ? AND chunk_z = ?)
        AND block_x = ? AND block_y = ? AND block_z = ?;
      """;

    try (var stmt = connection.prepareStatement(query)) {
      stmt.setString(1, worldId);
      stmt.setInt(2, chunkX);
      stmt.setInt(3, chunkZ);
      stmt.setInt(4, pos.getX());
      stmt.setInt(5, pos.getY());
      stmt.setInt(6, pos.getZ());
      stmt.executeUpdate();
    } catch (SQLException e) {
      CobbleUtils.LOGGER.error(CobbleUtils.MOD_ID, "Failed to remove block from database" + e);
    }
  }

  @Override
  public boolean isBlockPlaceByPlayer(World world, BlockPos pos) {
    if (blockCache.containsKey(pos)) return true;
    return isBlockInDatabase(world, pos);
  }

  private boolean isBlockInDatabase(World world, BlockPos pos) {
    String worldId = world.getRegistryKey().getValue().toString();
    int chunkX = pos.getX() >> 4;
    int chunkZ = pos.getZ() >> 4;

    String query = """
      SELECT 1 FROM blocks
      JOIN chunks ON blocks.chunk_id = chunks.id
      JOIN worlds ON chunks.world_id = worlds.id
      WHERE worlds.world_id = ? AND chunks.chunk_x = ? AND chunks.chunk_z = ?
        AND blocks.block_x = ? AND blocks.block_y = ? AND blocks.block_z = ?;
      """;

    try (var stmt = connection.prepareStatement(query)) {
      stmt.setString(1, worldId);
      stmt.setInt(2, chunkX);
      stmt.setInt(3, chunkZ);
      stmt.setInt(4, pos.getX());
      stmt.setInt(5, pos.getY());
      stmt.setInt(6, pos.getZ());
      try (var rs = stmt.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      CobbleUtils.LOGGER.error(CobbleUtils.MOD_ID, "Failed to check block in database" + e);
    }
    return false;
  }

  public void deleteWorld(World world) {
    String worldId = world.getRegistryKey().getValue().toString();

    String deleteBlocks = """
      DELETE FROM blocks
      WHERE chunk_id IN (
          SELECT id FROM chunks WHERE world_id = (SELECT id FROM worlds WHERE world_id = ?)
      );
      """;

    String deleteChunks = "DELETE FROM chunks WHERE world_id = (SELECT id FROM worlds WHERE world_id = ?);";
    String deleteWorld = "DELETE FROM worlds WHERE world_id = ?;";

    try (var stmtBlocks = connection.prepareStatement(deleteBlocks);
         var stmtChunks = connection.prepareStatement(deleteChunks);
         var stmtWorld = connection.prepareStatement(deleteWorld)) {

      stmtBlocks.setString(1, worldId);
      stmtBlocks.executeUpdate();

      stmtChunks.setString(1, worldId);
      stmtChunks.executeUpdate();

      stmtWorld.setString(1, worldId);
      stmtWorld.executeUpdate();

      CobbleUtils.LOGGER.info(CobbleUtils.MOD_ID, "Deleted all data for world: " + worldId);

      worldCache.remove(worldId);
      chunkCache.entrySet().removeIf(e -> e.getKey().startsWith(worldId + ":"));

    } catch (SQLException e) {
      CobbleUtils.LOGGER.error(CobbleUtils.MOD_ID, "Failed to delete world data " + e);
    }

    blockCache.entrySet().removeIf(entry -> {
      String cacheWorldId = world.getRegistryKey().getValue().toString();
      return cacheWorldId.equals(worldId);
    });
  }

  private void startBatchUpdateTask() {
    // Use scheduler to run insertBlocks every minute
    scheduler.scheduleAtFixedRate(this::insertBlocks, 1, 1, TimeUnit.MINUTES);
  }

  @Override
  public void insertBlocks() {
    CompletableFuture.runAsync(() -> {
      try {
        if (!blockCache.isEmpty()) {
          try (var stmt = connection.prepareStatement(
            "INSERT INTO blocks (chunk_id, block_x, block_y, block_z, player_uuid) VALUES (?, ?, ?, ?, ?)")) {
            for (BlockData data : blockCache.values()) {
              stmt.setInt(1, data.chunkId);
              stmt.setInt(2, data.blockX);
              stmt.setInt(3, data.blockY);
              stmt.setInt(4, data.blockZ);
              stmt.setString(5, data.playerUuid);
              stmt.addBatch();
            }
            stmt.executeBatch();
            blockCache.clear();
            CobbleUtils.LOGGER.info(CobbleUtils.MOD_ID, "Batch update completed.");
          }
        }
      } catch (SQLException e) {
        CobbleUtils.LOGGER.error(CobbleUtils.MOD_ID, "Failed to perform batch update" + e);
      }
    }, DataBaseBlock.DB_THREAD_FACTORY);
  }
}
