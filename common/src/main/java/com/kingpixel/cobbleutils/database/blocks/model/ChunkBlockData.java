package com.kingpixel.cobbleutils.database.blocks.model;

/**
 * @author Carlos Varas Alonso - 08/10/2025 22:11
 */

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import lombok.Data;

@Data
public class ChunkBlockData {
  private final LongOpenHashSet blocks = new LongOpenHashSet();
  private long lastAccess = System.currentTimeMillis();

  public void add(long blockKey) {
    blocks.add(blockKey);
    touch();
  }

  public boolean remove(long blockKey) {
    var result = blocks.remove(blockKey);
    touch();
    return result;
  }

  public boolean contains(long blockKey) {
    touch();
    return blocks.contains(blockKey);
  }

  public void touch() {
    lastAccess = System.currentTimeMillis();
  }

}

