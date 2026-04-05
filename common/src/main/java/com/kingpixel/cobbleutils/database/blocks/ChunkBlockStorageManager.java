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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ChunkBlockStorageManager {

  private static File storageDir;

  private static final Cache<String, ChunkBlockData> CHUNK_CACHE = Caffeine.newBuilder()
    .expireAfterAccess(5, TimeUnit.MINUTES)
    .maximumSize(10_000)
    .removalListener((String key, ChunkBlockData value, RemovalCause cause) -> {
      if (key == null || value == null || !value.isDirty()) return;
      try {
        // Si el servidor está parando, guardar sincrónicamente para no perder datos
        if (CobbleUtils.server != null && (CobbleUtils.server.isStopping() || CobbleUtils.server.isStopped())) {
          saveChunkSync(key, value);
        } else {
          saveChunkAsync(key, value);
        }
      } catch (Throwable t) {
        CobbleUtils.LOGGER_RAW.warn("Failed to save chunk on eviction: " + key, t);
      }
    })
    .build();

  // Usar String como clave para no retener referencias al objeto World
  private static final ConcurrentHashMap<String, String> WORLD_NAME_CACHE = new ConcurrentHashMap<>();

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

  // Usar get() atómico de Caffeine para evitar race conditions
  private static ChunkBlockData getChunkData(World world, Chunk chunk) {
    String key = getKey(world, chunk);
    return CHUNK_CACHE.get(key, k -> {
      ChunkBlockData initial = new ChunkBlockData();
      CompletableFuture.runAsync(() -> {
        try {
          ChunkBlockData loaded = loadChunkSync(world, chunk);
          initial.mergeFrom(loaded);
        } catch (Throwable t) {
          CobbleUtils.LOGGER_RAW.warn("Failed to async load chunk: " + k, t);
        }
      }, UtilsFile.IO_EXECUTOR);
      return initial;
    });
  }

  private static String getKey(World world, Chunk chunk) {
    return getSanitizedWorldName(world) + "_" + chunk.getPos().x + "_" + chunk.getPos().z;
  }

  // Cachear por registry key string para no retener el objeto World
  private static String getSanitizedWorldName(World world) {
    String registryId = world.getRegistryKey().getValue().toString();
    return WORLD_NAME_CACHE.computeIfAbsent(registryId, id -> id.replaceAll("[^a-zA-Z0-9-_]", "_"));
  }

  private static ChunkBlockData loadChunkSync(World world, Chunk chunk) {
    File worldDir = new File(storageDir, getSanitizedWorldName(world));
    File file = new File(worldDir, "chunk_" + chunk.getPos().x + "_" + chunk.getPos().z + ".dat");
    ChunkBlockData data = new ChunkBlockData();
    if (!file.exists()) return data;

    try (DataInputStream in = new DataInputStream(new GZIPInputStream(new FileInputStream(file)))) {
      int size = in.readInt();
      for (int i = 0; i < size; i++) data.add(in.readLong());
      data.clearDirty();
    } catch (IOException e) {
      CobbleUtils.LOGGER_RAW.warn("Failed to load chunk file: " + file.getName(), e);
    }
    return data;
  }

  public static CompletableFuture<Void> saveChunkAsync(String key, ChunkBlockData data) {
    return CompletableFuture.runAsync(() -> saveChunkSync(key, data), UtilsFile.IO_EXECUTOR);
  }

  // Escritura atómica: escribir en archivo temporal y renombrar
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
      File tempFile = new File(worldDir, "chunk_" + x + "_" + z + ".dat.tmp");

      try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(tempFile)))) {
        LongOpenHashSet blocks = data.getBlocks();
        out.writeInt(blocks.size());
        for (long b : blocks) out.writeLong(b);
      }

      // Renombrar atómicamente para evitar corrupción
      Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      data.clearDirty();
    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.warn("Failed to save chunk: " + key, e);
    }
  }

  public static CompletableFuture<Void> shutdownAsync() {
    return CompletableFuture.allOf(
      CHUNK_CACHE.asMap().entrySet().stream()
        .filter(e -> e.getValue().isDirty())
        .map(e -> saveChunkAsync(e.getKey(), e.getValue()))
        .toArray(CompletableFuture[]::new)
    );
  }
}

