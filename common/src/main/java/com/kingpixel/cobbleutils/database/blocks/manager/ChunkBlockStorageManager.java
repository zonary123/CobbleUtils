package com.kingpixel.cobbleutils.database.blocks.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.database.blocks.model.ChunkBlockData;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.io.*;
import java.util.concurrent.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ChunkBlockStorageManager {

  // Executor nombrado para IO
  private static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
    Thread t = new Thread(r);
    t.setName("CobbleUtils-IO-Thread");
    t.setDaemon(true);
    return t;
  });

  private static File storageDir;

  // Cache chunks: key = world_sanitized + "_" + chunkX + "_" + chunkZ
  private static final Cache<String, ChunkBlockData> CHUNK_CACHE = Caffeine.newBuilder()
    .expireAfterAccess(1, TimeUnit.MINUTES)
    .removalListener((key, value, cause) -> {
      if (key != null && value != null) {
        saveChunkByKey((String) key, (ChunkBlockData) value);
      }
    })
    .build();

  // Cache de nombres de mundos saneados
  private static final Cache<World, String> WORLD_NAME_CACHE = Caffeine.newBuilder().build();

  // Inicializar carpeta principal
  public static void init(MinecraftServer server) {
    storageDir = new File(server.getSavePath(WorldSavePath.ROOT).toFile(), "cobbleutils_blocks");
    if (!storageDir.exists()) storageDir.mkdirs();
  }

  // ===========================
  // MÉTODOS PRINCIPALES
  // ===========================

  public static void markPlaced(World world, Chunk chunk, BlockPos pos) {
    getChunkData(world, chunk).add(pos.asLong());
  }

  public static boolean isPlacedByPlayer(World world, Chunk chunk, BlockPos pos) {
    return getChunkData(world, chunk).contains(pos.asLong());
  }

  public static boolean removePlaced(World world, Chunk chunk, BlockPos pos) {
    return getChunkData(world, chunk).remove(pos.asLong());
  }

  // ===========================
  // CACHE Y CARGA/SALVADO
  // ===========================

  private static ChunkBlockData getChunkData(World world, Chunk chunk) {
    String key = getKey(world, chunk);
    ChunkBlockData data = CHUNK_CACHE.getIfPresent(key);
    if (data == null) {
      try {
        data = loadChunk(world, chunk).get();
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
        data = new ChunkBlockData();
        CHUNK_CACHE.put(key, data);
      }
    }
    return data;
  }

  private static String getKey(World world, Chunk chunk) {
    return getSanitizedWorldName(world) + "_" + chunk.getPos().x + "_" + chunk.getPos().z;
  }

  private static String getSanitizedWorldName(World world) {
    return WORLD_NAME_CACHE.get(world, w -> sanitizeWorldName(w.getRegistryKey().getValue().toString()));
  }

  private static String sanitizeWorldName(String worldName) {
    // Reemplaza todo lo que no sea letra, número, guion o guion bajo
    return worldName.replaceAll("[^a-zA-Z0-9-_]", "_");
  }

  public static Future<ChunkBlockData> loadChunk(World world, Chunk chunk) {
    String key = getKey(world, chunk);
    return IO_EXECUTOR.submit(() -> {
      File worldDir = new File(storageDir, getSanitizedWorldName(world));
      if (!worldDir.exists()) worldDir.mkdirs();

      File chunkFile = new File(worldDir, "chunk_" + chunk.getPos().x + "_" + chunk.getPos().z + ".dat");
      ChunkBlockData chunkBlockData = new ChunkBlockData();

      if (chunkFile.exists()) {
        try (FileInputStream fis = new FileInputStream(chunkFile);
             GZIPInputStream gzip = new GZIPInputStream(fis);
             DataInputStream dis = new DataInputStream(gzip)) {

          int size = dis.readInt();
          for (int i = 0; i < size; i++) {
            long blockKey = dis.readLong();
            chunkBlockData.add(blockKey);
          }

        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      CHUNK_CACHE.put(key, chunkBlockData);
      return chunkBlockData;
    });
  }

  public static void saveChunk(World world, Chunk chunk) {
    String key = getKey(world, chunk);
    ChunkBlockData data = CHUNK_CACHE.getIfPresent(key);
    if (data != null) {
      saveChunkByKey(key, data);
    }
  }

  private static void saveChunkByKey(String key, ChunkBlockData data) {
    IO_EXECUTOR.execute(() -> {
      try {
        // La key = sanitizedWorld + "_" + chunkX + "_" + chunkZ
        int lastUnderscore = key.lastIndexOf('_');
        int secondLastUnderscore = key.lastIndexOf('_', lastUnderscore - 1);
        if (secondLastUnderscore == -1 || lastUnderscore == -1) return;

        String worldName = key.substring(0, secondLastUnderscore);
        int chunkX = Integer.parseInt(key.substring(secondLastUnderscore + 1, lastUnderscore));
        int chunkZ = Integer.parseInt(key.substring(lastUnderscore + 1));

        File worldDir = new File(storageDir, worldName);
        if (!worldDir.exists()) worldDir.mkdirs();

        File chunkFile = new File(worldDir, "chunk_" + chunkX + "_" + chunkZ + ".dat");

        try (FileOutputStream fos = new FileOutputStream(chunkFile);
             GZIPOutputStream gzip = new GZIPOutputStream(fos);
             DataOutputStream dos = new DataOutputStream(gzip)) {

          LongOpenHashSet blocks = data.getBlocks();
          dos.writeInt(blocks.size());
          for (long block : blocks) {
            dos.writeLong(block);
          }
        }

      } catch (IOException | NumberFormatException e) {
        e.printStackTrace();
      }
    });
  }

  // ===========================
  // SHUTDOWN
  // ===========================
  public static void shutdown() {
    CHUNK_CACHE.asMap().forEach(ChunkBlockStorageManager::saveChunkByKey);
    CobbleUtils.shutdownAndAwait(IO_EXECUTOR);
  }
}
