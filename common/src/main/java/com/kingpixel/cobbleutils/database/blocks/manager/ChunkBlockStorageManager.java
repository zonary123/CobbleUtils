package com.kingpixel.cobbleutils.database.blocks.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.database.blocks.model.ChunkBlockData;
import com.kingpixel.cobbleutils.util.Utils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.io.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Handles chunk-based storage of player-placed blocks.
 * Uses Caffeine cache + async disk IO.
 */
public class ChunkBlockStorageManager {

  private static File storageDir;

  /**
   * Cache key format: worldName_chunkX_chunkZ
   */
  private static final Cache<String, ChunkBlockData> CHUNK_CACHE =
    Caffeine.newBuilder()
      .expireAfterAccess(5, TimeUnit.MINUTES)
      .maximumSize(10_000)
      .removalListener((String key, ChunkBlockData value, RemovalCause cause) -> {
        if (key == null || value == null) return;

        debug("Chunk evicted from cache: " + key + " | cause=" + cause);

        if (value.isDirty()) {
          debug("Evicted chunk is dirty, saving to disk: " + key);
          saveChunkByKeyAsync(key, value);
        }
      })
      .build();

  /**
   * Cache sanitized world names (weak keys to avoid memory leaks).
   */
  private static final Cache<World, String> WORLD_NAME_CACHE = Caffeine.newBuilder()
    .weakKeys()
    .build();

  /**
   * Initializes storage directory.
   */
  public static void init(MinecraftServer server) {
    storageDir = new File(
      server.getSavePath(WorldSavePath.ROOT).toFile(),
      "cobbleutils_blocks"
    );

    if (!storageDir.exists()) {
      storageDir.mkdirs();
    }

    debug("Storage initialized at: " + storageDir.getAbsolutePath());
  }

  // ===========================
  // BLOCK OPERATIONS
  // ===========================

  public static void markPlaced(World world, Chunk chunk, BlockPos pos, BlockState state) {
    BlockPos target = pos;

    // Handle falling blocks (sand, gravel, etc.)
    if (state.getBlock() instanceof FallingBlock) {
      int steps = 0;
      while (world.getBlockState(target.down()).isReplaceable() && steps++ < 256) {
        target = target.down();
      }
    }

    getChunkData(world, chunk).add(target.asLong());

    debug("Block placed at " + target +
      " | chunk=(" + chunk.getPos().x + "," + chunk.getPos().z + ")" +
      " | world=" + getSanitizedWorldName(world));
  }

  public static boolean removePlaced(World world, Chunk chunk, BlockPos pos, BlockState state) {
    ChunkBlockData data = getChunkData(world, chunk);
    boolean removed = data.remove(pos.asLong());

    debug("Block removed at " + pos +
      " | chunk=(" + chunk.getPos().x + "," + chunk.getPos().z + ")" +
      " | world=" + getSanitizedWorldName(world) +
      " | success=" + removed);

    // Handle falling blocks stacked above
    BlockPos current = pos.up();
    int steps = 0;

    while (steps++ < 256) {
      BlockState above = world.getBlockState(current);
      if (!(above.getBlock() instanceof FallingBlock)) break;

      if (data.contains(current.asLong())) {
        data.remove(current.asLong());
        data.add(current.down().asLong());

        debug("Falling block adjusted from " + current + " to " + current.down());
      }

      current = current.up();
    }

    return removed;
  }

  public static boolean isPlacedByPlayer(World world, Chunk chunk, BlockPos pos) {
    boolean result = getChunkData(world, chunk).contains(pos.asLong());

    debug("Check placed-by-player at " + pos +
      " | chunk=(" + chunk.getPos().x + "," + chunk.getPos().z + ")" +
      " | result=" + result);

    return result;
  }

  // ===========================
  // CACHE ACCESS
  // ===========================

  private static ChunkBlockData getChunkData(World world, Chunk chunk) {
    String key = getKey(world, chunk);

    return CHUNK_CACHE.get(key, k -> {
      debug("Cache miss for chunk: " + k);
      ChunkBlockData data = new ChunkBlockData();
      loadChunkAsync(world, chunk, data);
      return data;
    });
  }

  private static String getKey(World world, Chunk chunk) {
    return getSanitizedWorldName(world) + "_" +
      chunk.getPos().x + "_" +
      chunk.getPos().z;
  }

  private static String getSanitizedWorldName(World world) {
    return WORLD_NAME_CACHE.get(world,
      w -> w.getRegistryKey().getValue().toString().replaceAll("[^a-zA-Z0-9-_]", "_")
    );
  }

  // ===========================
  // ASYNC LOAD & SAVE
  // ===========================

  private static void loadChunkAsync(World world, Chunk chunk, ChunkBlockData target) {
    Utils.IO_EXECUTOR.execute(() -> {
      String key = getKey(world, chunk);
      debug("Loading chunk async from disk: " + key);

      ChunkBlockData loaded = loadChunkSync(world, chunk);
      target.mergeFrom(loaded);

      debug("Chunk loaded: " + key +
        " | blocks=" + loaded.getBlocks().size());
    });
  }

  private static ChunkBlockData loadChunkSync(World world, Chunk chunk) {
    File worldDir = new File(storageDir, getSanitizedWorldName(world));
    if (!worldDir.exists()) worldDir.mkdirs();

    File file = new File(
      worldDir,
      "chunk_" + chunk.getPos().x + "_" + chunk.getPos().z + ".dat"
    );

    ChunkBlockData data = new ChunkBlockData();

    if (!file.exists()) {
      debug("Chunk file not found, starting empty: " + file.getName());
      return data;
    }

    try (DataInputStream in = new DataInputStream(
      new GZIPInputStream(new FileInputStream(file))
    )) {
      int size = in.readInt();
      for (int i = 0; i < size; i++) {
        data.add(in.readLong());
      }
      data.clearDirty();

    } catch (IOException e) {
      CobbleUtils.LOGGER.warn("Failed to load chunk file: " + file.getName());
      e.printStackTrace();
    }

    return data;
  }

  private static void saveChunkByKeyAsync(String key, ChunkBlockData data) {
    CobbleUtils.runAsync(() -> saveChunkByKeySync(key, data), Utils.IO_EXECUTOR);
  }

  private static void saveChunkByKeySync(String key, ChunkBlockData data) {
    if (!data.isDirty()) return;

    debug("Saving chunk to disk: " + key +
      " | blocks=" + data.getBlocks().size());

    try {
      int last = key.lastIndexOf('_');
      int mid = key.lastIndexOf('_', last - 1);

      String world = key.substring(0, mid);
      int x = Integer.parseInt(key.substring(mid + 1, last));
      int z = Integer.parseInt(key.substring(last + 1));

      File worldDir = new File(storageDir, world);
      if (!worldDir.exists()) worldDir.mkdirs();

      File file = new File(worldDir, "chunk_" + x + "_" + z + ".dat");

      try (DataOutputStream out = new DataOutputStream(
        new GZIPOutputStream(new FileOutputStream(file))
      )) {
        LongOpenHashSet blocks = data.getBlocks();
        out.writeInt(blocks.size());
        for (long b : blocks) {
          out.writeLong(b);
        }
      }

      data.clearDirty();
      debug("Chunk saved successfully: " + key);

    } catch (Exception e) {
      CobbleUtils.LOGGER.warn("Failed to save chunk: " + key);
      e.printStackTrace();
    }
  }

  // ===========================
  // DEBUG
  // ===========================

  /**
   * Logs debug messages only if debug mode is enabled.
   */
  private static void debug(String message) {
    if (!CobbleUtils.config.isDebug()) return;
    CobbleUtils.LOGGER.info("[ChunkBlockStorage] " + message);
  }

  // ===========================
  // SHUTDOWN
  // ===========================

  public static void shutdown() {
    if (CobbleUtils.config.isDebug())
      CobbleUtils.LOGGER.info(
        "ChunkBlockStorageManager: Saving all cached chunks..."
      );
    CHUNK_CACHE.asMap().forEach(ChunkBlockStorageManager::saveChunkByKeySync);
  }
}
