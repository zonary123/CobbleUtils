package com.kingpixel.cobbleutils.database.blocks.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ChunkBlockStorageManager {

  private static File storageDir;

  // Cache chunks: key = world_sanitized + "_" + chunkX + "_" + chunkZ
  private static final Cache<String, ChunkBlockData> CHUNK_CACHE = Caffeine.newBuilder()
    .expireAfterAccess(1, TimeUnit.MINUTES)
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .removalListener((key, value, cause) -> {
      if (CobbleUtils.server.isStopped() || CobbleUtils.server.isStopping()) return;
      switch (cause) {
        case EXPLICIT -> {
          // No hacer nada si se elimina explícitamente
        }
        default -> {
          // Guardar en disco si se expulsa por expiración o tamaño
          if (key != null && value != null) {
            saveChunkByKey((String) key, (ChunkBlockData) value);
          }
        }
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
  public static void markPlaced(World world, Chunk chunk, BlockPos pos, BlockState state) {
    BlockPos target = pos;

    // If the block is a FallingBlock, find the last replaceable position below
    if (state.getBlock() instanceof FallingBlock) {
      int steps = 0;
      int maxSteps = 256; // maximum distance to avoid infinite loops
      BlockPos down = target.down();
      while (world.getBlockState(down).isReplaceable() && steps < maxSteps) {
        target = down;
        down = target.down();
        steps++;
      }
    }
    if (CobbleUtils.config.isDebug()) {
      CobbleUtils.LOGGER.info("Marking block as placed by player at " + target.getX() + ", " + target.getY() + ", " + target.getZ()
        + " in world " + getSanitizedWorldName(world));
    }
    getChunkData(world, chunk).add(target.asLong());
  }

  public static boolean removePlaced(World world, Chunk chunk, BlockPos pos, BlockState state) {
    boolean removed = getChunkData(world, chunk).remove(pos.asLong());

    BlockPos current = pos.up(); // block directly above
    int steps = 0;
    int maxSteps = 256; // maximum iterations to avoid infinite loops

    while (steps < maxSteps) {
      BlockState aboveState = world.getBlockState(current);

      // Stop if the block above is not a FallingBlock
      if (!(aboveState.getBlock() instanceof FallingBlock)) break;

      Chunk chunkAbove = world.getChunk(current);
      if (isPlacedByPlayer(world, chunkAbove, current)) {
        // Move the block above down by one position
        markPlaced(world, chunkAbove, current.down(), aboveState);
        removePlaced(world, chunkAbove, current, aboveState); // optional: clear the old position
      }

      current = current.up(); // next block in the stack
      steps++;
    }

    return removed;
  }


  public static boolean isPlacedByPlayer(World world, Chunk chunk, BlockPos pos) {
    return getChunkData(world, chunk).contains(pos.asLong());
  }


  // ===========================
  // CACHE Y CARGA/SALVADO
  // ===========================

  private static ChunkBlockData getChunkData(World world, Chunk chunk) {
    String key = getKey(world, chunk);
    ChunkBlockData data = CHUNK_CACHE.getIfPresent(key);
    if (data != null) return data;
    try {
      data = loadChunk(world, chunk).get();
    } catch (Exception e) {
      e.printStackTrace();
      data = new ChunkBlockData();
    }
    CHUNK_CACHE.put(key, data);
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
    return Utils.IO_EXECUTOR.submit(() -> {
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
            chunkBlockData.add(dis.readLong());
          }

        } catch (EOFException e) {
          CobbleUtils.LOGGER.error("Chunk file is corrupt or incomplete: " + chunkFile.getName() + ". Skipping it.");
          // Opcional: eliminar archivo corrupto para regenerarlo
          try {
            if (!chunkFile.delete()) {
              CobbleUtils.LOGGER.warn("Failed to delete corrupted chunk file: " + chunkFile.getName());
            }
          } catch (Exception ex) {
            CobbleUtils.LOGGER.error("Error deleting corrupted chunk file: " + chunkFile.getName());
          }
        } catch (IOException e) {
          CobbleUtils.LOGGER.error("Error reading chunk file: " + chunkFile.getName());
        }
      }

      return chunkBlockData;
    });
  }


  public static void saveChunk(World world, Chunk chunk) {
    String key = getKey(world, chunk);
    ChunkBlockData data = CHUNK_CACHE.getIfPresent(key);
    if (data != null) saveChunkByKey(key, data);
  }

  private static void saveChunkByKey(String key, ChunkBlockData data) {
    Utils.IO_EXECUTOR.execute(() -> saveChunkByKeySync(key, data));
  }

  private static void saveChunkByKeySync(String key, ChunkBlockData data) {
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
  }

  // ===========================
  // SHUTDOWN
  // ===========================
  public static void shutdown() {
    CobbleUtils.LOGGER.info("ChunkBlockStorageManager: Saving all cached chunk data to disk on shutdown...");
    CHUNK_CACHE.asMap().forEach(ChunkBlockStorageManager::saveChunkByKeySync);
  }
}
