package com.kingpixel.cobbleutils.database.blocks;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import lombok.Getter;

/**
 * Stores block positions (as long keys) for a single chunk.
 * Uses a dirty flag to avoid unnecessary disk writes.
 */
@Getter
public class ChunkBlockData {

  /**
   * Set of block positions stored as BlockPos.asLong()
   * Initial capacity avoids frequent rehashing.
   */
  private final LongOpenHashSet blocks = new LongOpenHashSet(256);

  /**
   * Indicates whether the data has changed since last save.
   */
  private volatile boolean dirty = false;

  /**
   * Adds a block to the set.
   *
   * @return true if the block was not already present
   */
  public boolean add(long blockKey) {
    boolean added = blocks.add(blockKey);
    if (added) dirty = true;
    return added;
  }

  /**
   * Removes a block from the set.
   *
   * @return true if the block was present
   */
  public boolean remove(long blockKey) {
    boolean removed = blocks.remove(blockKey);
    if (removed) dirty = true;
    return removed;
  }

  /**
   * Checks if the block exists in the set.
   * This method does NOT mutate state.
   */
  public boolean contains(long blockKey) {
    return blocks.contains(blockKey);
  }

  /**
   * Clears the dirty flag after saving.
   */
  public void clearDirty() {
    dirty = false;
  }

  /**
   * Merges data loaded asynchronously from disk.
   */
  public void mergeFrom(ChunkBlockData other) {
    if (other == null || other.blocks.isEmpty()) return;
    blocks.addAll(other.blocks);
    dirty = true;
  }
}
