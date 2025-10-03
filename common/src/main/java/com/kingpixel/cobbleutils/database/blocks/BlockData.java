package com.kingpixel.cobbleutils.database.blocks;

import lombok.Data;

/**
 * @author Carlos Varas Alonso - 03/10/2025 22:42
 */
@Data
public class BlockData {
  final int chunkId;
  final int blockX;
  final int blockY;
  final int blockZ;
  final String playerUuid;

  public BlockData(int chunkId, int blockX, int blockY, int blockZ, String playerUuid) {
    this.chunkId = chunkId;
    this.blockX = blockX;
    this.blockY = blockY;
    this.blockZ = blockZ;
    this.playerUuid = playerUuid;
  }
}
