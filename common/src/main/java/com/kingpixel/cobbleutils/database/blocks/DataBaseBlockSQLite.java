package com.kingpixel.cobbleutils.database.blocks;

/**
 * Optimized SQLite block database for CobbleUtils.
 * <p>
 * Mejoras:
 * - Uso de PRAGMA optimizados (WAL + NORMAL + cache en memoria).
 * - Uso de transacciones en batch para más velocidad.
 * - Flush de cache más frecuente (cada 30 segundos).
 * - Ejecutor de un solo hilo para acceso seguro al Connection.
 *
 * @author Carlos Varas Alonso
 * @optimized by ChatGPT - 04/09/2025
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
  private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor(
    new ThreadFactoryBuilder()
      .setNameFormat("CobbleUtils-DB-Blocks-%d")
      .build()
  );

  public static Task task;

  public DataBaseBlockSQLite(DataBaseConfig config) {
    connect();
    startBatchUpdateTask();
  }

  /**
   * Conectar a la base de datos y aplicar PRAGMAs de rendimiento.
   */
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
      CobbleUtils.LOGGER.error(CobbleUtils.MOD_ID, "Failed to connect to database " + e);
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
      CobbleUtils.LOGGER.error(CobbleUtils.MOD_ID, "Failed to close database connection " + e);
    }
    if (task != null) {
      task.setExpired();
    }
    dbExecutor.shutdown();
  }

  @Override public void deleteWorld(World world) {
    String worldId = world.getRegistryKey().getValue().toString();
    dbExecutor.execute(() -> {
      String deleteBlocks = """
            DELETE FROM blocks
            WHERE chunk_id IN (
                SELECT id FROM chunks
                WHERE world_id = (SELECT id FROM worlds WHERE world_id = ?)
            );
        """;

      String deleteChunks = """
            DELETE FROM chunks
            WHERE world_id = (SELECT id FROM worlds WHERE world_id = ?);
        """;

      String deleteWorld = "DELETE FROM worlds WHERE world_id = ?;";

      try (var blockStmt = connection.prepareStatement(deleteBlocks);
           var chunkStmt = connection.prepareStatement(deleteChunks);
           var worldStmt = connection.prepareStatement(deleteWorld)) {

        blockStmt.setString(1, worldId);
        blockStmt.executeUpdate();

        chunkStmt.setString(1, worldId);
        chunkStmt.executeUpdate();

        worldStmt.setString(1, worldId);
        worldStmt.executeUpdate();

        CobbleUtils.LOGGER.info(CobbleUtils.MOD_ID, "Deleted all data for world: " + worldId);
      } catch (SQLException e) {
        CobbleUtils.LOGGER.error(CobbleUtils.MOD_ID, "Failed to delete world data " + e);
      }
    });
  }

  /**
   * Crear tablas e índices si no existen.
   */
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
      CobbleUtils.LOGGER.error(CobbleUtils.MOD_ID, "Failed to create tables or indexes " + e);
    }
  }

  /**
   * Guardar un bloque en cache (se inserta en DB más tarde en batch).
   */
  @Override
  public void placeBlock(World world, BlockPos pos, BlockState state, ServerPlayerEntity player) {
    String worldId = world.getRegistryKey().getValue().toString();
    int chunkX = pos.getX() >> 4;
    int chunkZ = pos.getZ() >> 4;

    CompletableFuture.runAsync(() -> {
      try {
        int worldDbId = getOrInsert("worlds", "world_id", worldId);
        int chunkDbId = getOrInsertChunk(worldDbId, chunkX, chunkZ);

        BlockData blockData = new BlockData(chunkDbId, pos.getX(), pos.getY(), pos.getZ(), player.getUuidAsString());
        blockCache.put(pos, blockData);

        if (CobbleUtils.config.isDebug()) {
          CobbleUtils.LOGGER.info(CobbleUtils.MOD_ID, "Block placed and cached: " + pos);
        }
      } catch (SQLException e) {
        CobbleUtils.LOGGER.error(CobbleUtils.MOD_ID, "Failed to cache block " + e);
      }
    }, dbExecutor);
  }

  // NUEVO: cola de bloques a eliminar en batch
  private final ConcurrentLinkedQueue<DeleteEntry> deleteQueue = new ConcurrentLinkedQueue<DeleteEntry>();

  @Override
  public void removeBlock(World world, BlockPos pos, BlockState state, ServerPlayerEntity player) {
    CompletableFuture.delayedExecutor(50, TimeUnit.MILLISECONDS, dbExecutor)
      .execute(() -> {
        if (blockCache.remove(pos) == null) {
          deleteQueue.add(new DeleteEntry(pos, player));
        }
        if (CobbleUtils.config.isDebug()) {
          CobbleUtils.LOGGER.info(CobbleUtils.MOD_ID, "Block queued for removal: " + pos);
        }
      });
  }


  // Clase auxiliar para almacenar bloque + jugador
  private static class DeleteEntry {
    final BlockPos pos;
    final ServerPlayerEntity player;

    DeleteEntry(BlockPos pos, ServerPlayerEntity player) {
      this.pos = pos;
      this.player = player;
    }
  }

  // Flush deletes usando info del player
  private void flushDeletes() {
    if (deleteQueue.isEmpty()) return;

    CompletableFuture.runAsync(() -> {
      String query = """
        DELETE FROM blocks
        WHERE chunk_id = (
          SELECT id FROM chunks
          WHERE world_id = (SELECT id FROM worlds WHERE world_id = ?)
          AND chunk_x = ? AND chunk_z = ?
        )
        AND block_x = ? AND block_y = ? AND block_z = ?;
        """;

      try (var preparedStatement = connection.prepareStatement(query)) {
        DeleteEntry entry;
        while ((entry = deleteQueue.poll()) != null) {
          BlockPos pos = entry.pos;
          ServerPlayerEntity player = entry.player;

          // Obtenemos worldId desde el player
          String worldId = player.getWorld().getRegistryKey().getValue().toString();
          int chunkX = pos.getX() >> 4;
          int chunkZ = pos.getZ() >> 4;

          preparedStatement.setString(1, worldId);
          preparedStatement.setInt(2, chunkX);
          preparedStatement.setInt(3, chunkZ);
          preparedStatement.setInt(4, pos.getX());
          preparedStatement.setInt(5, pos.getY());
          preparedStatement.setInt(6, pos.getZ());

          preparedStatement.addBatch();
        }
        preparedStatement.executeBatch();
        CobbleUtils.LOGGER.info(CobbleUtils.MOD_ID, "Batch delete completed.");
      } catch (SQLException e) {
        CobbleUtils.LOGGER.error(CobbleUtils.MOD_ID, "Failed to perform batch delete");
      }
    }, dbExecutor);
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
      CobbleUtils.LOGGER.error(CobbleUtils.MOD_ID, "Failed to remove block from database " + e);
    }
  }

  /**
   * Comprobación rápida (primero cache, luego DB).
   */
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
      CobbleUtils.LOGGER.error(CobbleUtils.MOD_ID, "Failed to check block in database " + e);
    }
    return false;
  }

  /**
   * Batch insert: se ejecuta cada X tiempo y usa transacción para mayor velocidad.
   */
  @Override
  public void insertBlocks() {
    dbExecutor.execute(() -> {
      try {
        if (!blockCache.isEmpty()) {
          connection.setAutoCommit(false);
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
          }
          connection.commit();
          blockCache.clear();
          CobbleUtils.LOGGER.info(CobbleUtils.MOD_ID, "Batch update completed.");
        }
      } catch (SQLException e) {
        CobbleUtils.LOGGER.error(CobbleUtils.MOD_ID, "Failed to perform batch update " + e);
        try {
          connection.rollback();
        } catch (SQLException ignored) {
        }
      } finally {
        try {
          connection.setAutoCommit(true);
        } catch (SQLException ignored) {
        }
      }
    });
  }

  /**
   * Ejecutar batch insert cada 30 segundos (antes eran 5 minutos).
   */
  private void startBatchUpdateTask() {
    if (task != null) {
      task.setExpired();
    }
    long interval = 20 * 30;
    task = Task.builder()
      .execute(() -> {
        insertBlocks();
        flushDeletes();
      })
      .infinite()
      .interval(interval)
      .build();
  }

  // Métodos auxiliares (getOrInsert)
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

  // Clase interna para cachear datos de bloques
  @Data
  private static class BlockData {
    final int chunkId;
    final int blockX;
    final int blockY;
    final int blockZ;
    final String playerUuid;
  }
}
