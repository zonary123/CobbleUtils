package com.kingpixel.cobbleutils.database.blocks;

/**
 * @author Carlos Varas Alonso - 23/08/2025 7:37
 */

import ca.landonjw.gooeylibs2.api.tasks.Task;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import lombok.Data;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.*;

public class DataBaseBlockSQLite extends DataBaseBlock {
  private Connection connection;
  private final ConcurrentHashMap<BlockPos, BlockData> blockCache = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
    new ThreadFactoryBuilder()
      .setNameFormat("CobbleUtils-DB-DelayScheduler-%d")
      .build()
  );

  public static Task task;

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

        // PRAGMA optimizados
        try (var stmt = connection.createStatement()) {
          stmt.execute("PRAGMA journal_mode = WAL;");
          stmt.execute("PRAGMA synchronous = NORMAL;");
          stmt.execute("PRAGMA temp_store = MEMORY;");
          stmt.execute("PRAGMA cache_size = -32000;"); // ~32MB cache
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
        connection.close();
        CobbleUtils.LOGGER.info(CobbleUtils.MOD_ID, "Disconnected from the database of blocks");
      }
    } catch (SQLException e) {
      CobbleUtils.LOGGER.error(CobbleUtils.MOD_ID, "Failed to close database connection" + e);
    }
    if (task != null) {
      task.setExpired();
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

    try (var statement = connection.createStatement()) {
      statement.execute(createWorldsTable);
      statement.execute(createChunksTable);
      statement.execute(createBlocksTable);
      statement.execute(createIndexes);
    } catch (SQLException e) {
      CobbleUtils.LOGGER.error(CobbleUtils.MOD_ID, "Failed to create tables or indexes" + e);
    }
  }

  private CompletableFuture<Integer> getOrInsertAsync(String table, String column, String value) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return getOrInsert(table, column, value);
      } catch (SQLException e) {
        CobbleUtils.LOGGER.error(CobbleUtils.MOD_ID, "Failed to insert or retrieve ID from " + table + "  " + e);
        throw new RuntimeException(e);
      }
    }, CobbleUtils.EXECUTOR_COBBLEUTILS);
  }

  private int getOrInsert(String table, String column, String value) throws SQLException {
    String insertQuery = "INSERT OR IGNORE INTO " + table + " (" + column + ") VALUES (?);";
    String selectQuery = "SELECT id FROM " + table + " WHERE " + column + " = ?;";

    try (var insertStmt = connection.prepareStatement(insertQuery);
         var selectStmt = connection.prepareStatement(selectQuery)) {

      insertStmt.setString(1, value);
      insertStmt.executeUpdate();

      selectStmt.setString(1, value);
      var resultSet = selectStmt.executeQuery();
      if (resultSet.next()) {
        return resultSet.getInt("id");
      }
    }
    throw new SQLException("Failed to insert or retrieve ID from " + table);
  }

  private CompletableFuture<Integer> getOrInsertChunkAsync(int worldId, int chunkX, int chunkZ) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return getOrInsertChunk(worldId, chunkX, chunkZ);
      } catch (SQLException e) {
        CobbleUtils.LOGGER.error(CobbleUtils.MOD_ID, "Failed to insert or retrieve chunk ID" + e);
        throw new RuntimeException(e);
      }
    }, CobbleUtils.EXECUTOR_COBBLEUTILS);
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
      var resultSet = selectStmt.executeQuery();
      if (resultSet.next()) {
        return resultSet.getInt("id");
      }
    }
    throw new SQLException("Failed to insert or retrieve chunk ID");
  }

  @Override
  public void placeBlock(World world, BlockPos pos, BlockState state, ServerPlayerEntity player) {
    final String worldId = world.getRegistryKey().getValue().toString();
    final int chunkX = pos.getX() >> 4;
    final int chunkZ = pos.getZ() >> 4;
    final String playerUuid = player.getUuidAsString();

    getOrInsertAsync("worlds", "world_id", worldId)
      .thenCompose(worldDbId -> getOrInsertChunkAsync(worldDbId, chunkX, chunkZ))
      .thenAccept(chunkDbId -> {
        BlockData blockData = new BlockData(chunkDbId, pos.getX(), pos.getY(), pos.getZ(), playerUuid);

        // Ejecutar el cache con delay de 2 segundos
        CompletableFuture.delayedExecutor(50, TimeUnit.MILLISECONDS, scheduler)
          .execute(() -> {
            blockCache.put(pos, blockData);

            if (CobbleUtils.config.isDebug()) {
              CobbleUtils.LOGGER.info(CobbleUtils.MOD_ID,
                "Block cached with delay at " + pos + " in world " + worldId);
            }
          });
      })
      .exceptionally(e -> {
        CobbleUtils.LOGGER.error(CobbleUtils.MOD_ID,
          "Failed to cache block at " + pos + " in world " + worldId + "  " + e);
        return null;
      });
  }


  @Override
  public void removeBlock(World world, BlockPos pos, BlockState state, ServerPlayerEntity player) {
    // Introducir retraso de 50ms antes de borrar el bloque
    CompletableFuture.delayedExecutor(50, TimeUnit.MILLISECONDS, CobbleUtils.EXECUTOR_COBBLEUTILS)
      .execute(() -> {
        if (blockCache.remove(pos) == null) {
          removeBlockFromDatabaseAsync(world, pos);
        }
        if (CobbleUtils.config.isDebug()) {
          CobbleUtils.LOGGER.info(CobbleUtils.MOD_ID, "Block removed (with delay) from cache and/or database: " + pos);
        }
      });
  }


  private void removeBlockFromDatabaseAsync(World world, BlockPos pos) {
    CompletableFuture.runAsync(() -> removeBlockFromDatabase(world, pos), CobbleUtils.EXECUTOR_COBBLEUTILS);
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

    try (var preparedStatement = connection.prepareStatement(query)) {
      preparedStatement.setString(1, worldId);
      preparedStatement.setInt(2, chunkX);
      preparedStatement.setInt(3, chunkZ);
      preparedStatement.setInt(4, pos.getX());
      preparedStatement.setInt(5, pos.getY());
      preparedStatement.setInt(6, pos.getZ());

      preparedStatement.executeUpdate();
    } catch (SQLException e) {
      CobbleUtils.LOGGER.error(CobbleUtils.MOD_ID, "Failed to remove block from database" + e);
    }
  }

  @Override
  public boolean isBlockPlaceByPlayer(World world, BlockPos pos) {
    boolean cacheContains = blockCache.containsKey(pos);
    if (CobbleUtils.config.isDebug()) {
      CobbleUtils.LOGGER.info(CobbleUtils.MOD_ID, "isBlockPlaceByPlayer check at " + pos + ": " + cacheContains);
    }
    if (cacheContains) {
      return true;
    } else {
      boolean isInDatabase = isBlockInDatabase(world, pos);
      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER.info(CobbleUtils.MOD_ID, "Database check for block at " + pos + ": " + isInDatabase);
      }
      return isInDatabase;
    }
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

    try (var preparedStatement = connection.prepareStatement(query)) {
      preparedStatement.setString(1, worldId);
      preparedStatement.setInt(2, chunkX);
      preparedStatement.setInt(3, chunkZ);
      preparedStatement.setInt(4, pos.getX());
      preparedStatement.setInt(5, pos.getY());
      preparedStatement.setInt(6, pos.getZ());

      try (var resultSet = preparedStatement.executeQuery()) {
        return resultSet.next();
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

    String deleteChunks = """
          DELETE FROM chunks
          WHERE world_id = (SELECT id FROM worlds WHERE world_id = ?);
      """;

    String deleteWorld = """
          DELETE FROM worlds WHERE world_id = ?;
      """;

    try (var deleteBlocksStmt = connection.prepareStatement(deleteBlocks);
         var deleteChunksStmt = connection.prepareStatement(deleteChunks);
         var deleteWorldStmt = connection.prepareStatement(deleteWorld)) {

      // 1. Borrar bloques
      deleteBlocksStmt.setString(1, worldId);
      deleteBlocksStmt.executeUpdate();

      // 2. Borrar chunks
      deleteChunksStmt.setString(1, worldId);
      deleteChunksStmt.executeUpdate();

      // 3. Borrar el mundo
      deleteWorldStmt.setString(1, worldId);
      deleteWorldStmt.executeUpdate();

      CobbleUtils.LOGGER.info(CobbleUtils.MOD_ID, "Deleted all data for world: " + worldId);

    } catch (SQLException e) {
      CobbleUtils.LOGGER.error(CobbleUtils.MOD_ID, "Failed to delete world data " + e);
    }

    // También limpiar la cache por si acaso
    blockCache.entrySet().removeIf(entry -> {
      BlockPos pos = entry.getKey();
      String cacheWorldId = world.getRegistryKey().getValue().toString();
      return cacheWorldId.equals(worldId);
    });
  }


  private void startBatchUpdateTask() {
    if (task != null) {
      task.setExpired();
    }
    long interval = 20 * 60 * 5; // 5 minutes in ticks (20 ticks per second)
    task = Task.builder()
      .execute(this::insertBlocks)
      .infinite()
      .interval(interval)
      .build();
  }

  @Override
  public void insertBlocks() {
    CompletableFuture.runAsync(() -> {
      try {
        if (!blockCache.isEmpty()) {
          try (var preparedStatement = connection.prepareStatement(
            "INSERT INTO blocks (chunk_id, block_x, block_y, block_z, player_uuid) VALUES (?, ?, ?, ?, ?)")) {
            for (BlockData blockData : blockCache.values()) {
              preparedStatement.setInt(1, blockData.chunkId);
              preparedStatement.setInt(2, blockData.blockX);
              preparedStatement.setInt(3, blockData.blockY);
              preparedStatement.setInt(4, blockData.blockZ);
              preparedStatement.setString(5, blockData.playerUuid);
              preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
            blockCache.clear();
            CobbleUtils.LOGGER.info(CobbleUtils.MOD_ID, "Batch update completed.");
          }
        }
      } catch (SQLException e) {
        CobbleUtils.LOGGER.error(CobbleUtils.MOD_ID, "Failed to perform batch update" + e);
      }
    }, CobbleUtils.EXECUTOR_COBBLEUTILS);
  }

  @Data
  private static class BlockData {
    final int chunkId;
    final int blockX;
    final int blockY;
    final int blockZ;
    final String playerUuid;

    BlockData(int chunkId, int blockX, int blockY, int blockZ, String playerUuid) {
      this.chunkId = chunkId;
      this.blockX = blockX;
      this.blockY = blockY;
      this.blockZ = blockZ;
      this.playerUuid = playerUuid;
    }
  }
}