package com.kingpixel.cobbleutils.database.blocks;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import lombok.Getter;

@Getter
public class ChunkBlockData {

  private final LongOpenHashSet blocks = new LongOpenHashSet(256);
  private final transient Object lock = new Object(); // lock interno para seguridad de hilos

  private volatile boolean dirty = false;
  private volatile boolean saving = false;

  /**
   * Añade un bloque, marca dirty si es nuevo.
   */
  public boolean add(long blockKey) {
    synchronized (lock) {
      boolean added = blocks.add(blockKey);
      if (added) dirty = true;
      return added;
    }
  }

  /**
   * Elimina un bloque, marca dirty si existía.
   */
  public boolean remove(long blockKey) {
    synchronized (lock) {
      boolean removed = blocks.remove(blockKey);
      if (removed) dirty = true;
      return removed;
    }
  }

  /**
   * Comprueba si un bloque existe en la colección.
   */
  public boolean contains(long blockKey) {
    synchronized (lock) {
      return blocks.contains(blockKey);
    }
  }

  /**
   * Marca que los cambios han sido guardados.
   */
  public void clearDirty() {
    dirty = false;
  }

  /**
   * Devuelve un snapshot de los bloques, thread-safe.
   */
  public LongOpenHashSet snapshot() {
    synchronized (lock) {
      return new LongOpenHashSet(blocks);
    }
  }

  /**
   * Fusiona datos de otro ChunkBlockData.
   * No se pierde ningún bloque, marca dirty si se agregan bloques nuevos.
   */
  public void mergeFrom(ChunkBlockData other) {
    if (other == null) return;
    synchronized (lock) {
      synchronized (other.lock) {
        if (!other.blocks.isEmpty()) {
          blocks.addAll(other.blocks);
          dirty = true;
        }
      }
    }
  }

  /**
   * Intenta marcar como "guardando" para evitar saves concurrentes.
   *
   * @return true si pudo marcar, false si ya estaba en proceso de guardado.
   */
  public boolean markSaving() {
    synchronized (lock) {
      if (saving) return false;
      saving = true;
      return true;
    }
  }

  /**
   * Marca que el guardado terminó.
   */
  public void finishSaving() {
    synchronized (lock) {
      saving = false;
    }
  }

  /**
   * Devuelve el número de bloques en este chunk.
   */
  public int size() {
    synchronized (lock) {
      return blocks.size();
    }
  }

  /**
   * Limpia todos los bloques del chunk.
   */
  public void clear() {
    synchronized (lock) {
      blocks.clear();
      dirty = true;
    }
  }
}