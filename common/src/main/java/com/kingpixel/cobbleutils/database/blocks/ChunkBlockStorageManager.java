package com.kingpixel.cobbleutils.database.blocks;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.UtilsFile;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ChunkBlockStorageManager {

  private static File storageDir;

  private static final Cache<String, ChunkBlockData> CHUNK_CACHE = Caffeine.newBuilder()
    .expireAfterAccess(5, TimeUnit.MINUTES)
    .maximumSize(10_000)
    .removalListener((String key, ChunkBlockData value, RemovalCause cause) -> {
      if (key == null || value == null) return;
      if (CobbleUtils.server.isStopping() || CobbleUtils.server.isStopped()) return;
      if (value.isDirty()) saveChunkAsync(key, value);
    })
    .build();

  private static final Cache<World, String> WORLD_NAME_CACHE = Caffeine.newBuilder()
    .weakKeys()
    .build();

  public static void init(MinecraftServer server) {
    storageDir = new File(server.getSavePath(WorldSavePath.ROOT).toFile(), "cobbleutils_blocks");
    if (!storageDir.exists()) storageDir.mkdirs();
  }

  public static void markPlaced(World world, Chunk chunk, BlockPos pos, BlockState state) {
    BlockPos target = pos;
    if (state.getBlock() instanceof FallingBlock) {
      int steps = 0;
      while (world.getBlockState(target.down()).isReplaceable() && steps++ < 256) {
        target = target.down();
      }
    }
    getChunkData(world, chunk).add(target.asLong());
  }

  public static boolean removePlaced(World world, Chunk chunk, BlockPos pos, BlockState state) {
    ChunkBlockData data = getChunkData(world, chunk);
    boolean removed = data.remove(pos.asLong());

    BlockPos current = pos.up();
    int steps = 0;
    while (steps++ < 256) {
      BlockState above = world.getBlockState(current);
      if (!(above.getBlock() instanceof FallingBlock)) break;
      if (data.contains(current.asLong())) {
        data.remove(current.asLong());
        data.add(current.down().asLong());
      }
      current = current.up();
    }
    return removed;
  }

  public static boolean isPlacedByPlayer(World world, Chunk chunk, BlockPos pos) {
    return getChunkData(world, chunk).contains(pos.asLong());
  }

  /**
   * Obtiene el ChunkBlockData de manera sincronizada.
   * Si no existe, carga desde disco y lo cachea.
   */
  private static ChunkBlockData getChunkData(World world, Chunk chunk) {
    String key = getKey(world, chunk);

    ChunkBlockData cached = CHUNK_CACHE.getIfPresent(key);
    if (cached != null) return cached;

    cached = new ChunkBlockData();
    CHUNK_CACHE.put(key, cached);

    ChunkBlockData finalCached = cached;
    CompletableFuture.runAsync(() -> {
      ChunkBlockData loaded = loadChunkSync(world, chunk);
      finalCached.mergeFrom(loaded);
    }, UtilsFile.IO_CONTEXT.getExecutor());

    return cached;
  }

  private static String getKey(World world, Chunk chunk) {
    return getSanitizedWorldName(world) + "_" + chunk.getPos().x + "_" + chunk.getPos().z;
  }

  private static String getSanitizedWorldName(World world) {
    return WORLD_NAME_CACHE.get(world, w -> w.getRegistryKey().getValue().toString().replaceAll("[^a-zA-Z0-9-_]", "_"));
  }

  // =========================================
  // Async Load & Save con CompletableFuture
  // =========================================

  /**
   * Precarga async sin bloquear. El merge puede perder un bloque temporalmente.
   */
  public static CompletableFuture<ChunkBlockData> loadChunkAsync(World world, Chunk chunk) {
    String key = getKey(world, chunk);

    // obtenemos o creamos chunk vacío en cache
    ChunkBlockData cached = CHUNK_CACHE.get(key, k -> new ChunkBlockData());

    // carga async y merge en la cache
    return CompletableFuture.supplyAsync(() -> {
      ChunkBlockData loaded = loadChunkSync(world, chunk);
      cached.mergeFrom(loaded);
      return cached; // devolvemos la instancia de la cache
    }, UtilsFile.IO_CONTEXT.getExecutor());
  }

  private static ChunkBlockData loadChunkSync(World world, Chunk chunk) {
    File worldDir = new File(storageDir, getSanitizedWorldName(world));
    if (!worldDir.exists()) worldDir.mkdirs();
    File file = new File(worldDir, "chunk_" + chunk.getPos().x + "_" + chunk.getPos().z + ".dat");
    ChunkBlockData data = new ChunkBlockData();
    if (!file.exists()) return data;

    try (DataInputStream in = new DataInputStream(new GZIPInputStream(new FileInputStream(file)))) {
      int size = in.readInt();
      for (int i = 0; i < size; i++) data.add(in.readLong());
      data.clearDirty();
    } catch (IOException e) {
      CobbleUtils.LOGGER.warn("Failed to load chunk file: " + file.getName());
      e.printStackTrace();
    }
    return data;
  }

  public static CompletableFuture<Void> saveChunkAsync(String key, ChunkBlockData data) {
    return CompletableFuture.runAsync(() -> saveChunkSync(key, data), UtilsFile.IO_CONTEXT.getExecutor());
  }

  private static void saveChunkSync(String key, ChunkBlockData data) {
    if (!data.isDirty()) return;
    try {
      int last = key.lastIndexOf('_');
      int mid = key.lastIndexOf('_', last - 1);
      String world = key.substring(0, mid);
      int x = Integer.parseInt(key.substring(mid + 1, last));
      int z = Integer.parseInt(key.substring(last + 1));
      File worldDir = new File(storageDir, world);
      if (!worldDir.exists()) worldDir.mkdirs();
      File file = new File(worldDir, "chunk_" + x + "_" + z + ".dat");
      try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(file)))) {
        LongOpenHashSet blocks = data.getBlocks();
        out.writeInt(blocks.size());
        for (long b : blocks) out.writeLong(b);
      }
      data.clearDirty();
    } catch (Exception e) {
      CobbleUtils.LOGGER.warn("Failed to save chunk: " + key);
      e.printStackTrace();
    }
  }

  public static CompletableFuture<Void> shutdownAsync() {
    return CompletableFuture.allOf(
      CHUNK_CACHE.asMap().entrySet().stream()
        .map(e -> saveChunkAsync(e.getKey(), e.getValue()))
        .toArray(CompletableFuture[]::new)
    );
  }
}