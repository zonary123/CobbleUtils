package com.kingpixel.cobbleutils.events;

import com.kingpixel.cobbleutils.events.models.*;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Carlos Varas Alonso - 26/08/2025 14:42
 */
public class CobbleUtilsEvents {
  private CobbleUtilsEvents() {
    /* This utility class should not be instantiated */
  }

  public static final EventChannel<EventBrewing> BREWING_EVENT = new EventChannel<>();
  public static final EventChannel<EventBlockBreak> BLOCK_BREAK_EVENT = new EventChannel<>();
  public static final EventChannel<EventBlockPlaced> BLOCK_PLACED_EVENT = new EventChannel<>();
  public static final EventChannel<EventCollect> COLLECT_EVENT = new EventChannel<>();
  public static final EventChannel<ServerPlayerEntity> PLAY_TIME_EVENT = new EventChannel<>();
  public static final EventChannel<EventItemStack> ARCHEOLOGY_EVENT = new EventChannel<>();
  public static final EventChannel<EventEntity> BRED_EVENT = new EventChannel<>();
  public static final EventChannel<EventItemStack> CAMPFIRE_POT_EVENT = new EventChannel<>();
  public static final EventChannel<EventItemStack> EATING_EVENT = new EventChannel<>();
  public static final EventChannel<EventEntity> ENTITY_KILLED_EVENT = new EventChannel<>();
  public static final EventChannel<EventItemStack> FISHING_EVENT = new EventChannel<>();
  public static final EventChannel<ServerPlayerEntity> MILKING_EVENT = new EventChannel<>();
  public static final EventChannel<EventBlock> STRIPPED_LOG_EVENT = new EventChannel<>();
  public static final EventChannel<EventEntity> TAMING_EVENT = new EventChannel<>();
  public static final EventChannel<EventItemStack> TRADE_EVENT = new EventChannel<>();
  public static final EventChannel<EventTravel> TRAVEL_EVENT = new EventChannel<>();
  public static final EventChannel<EventEntity> INTERACT_ENTITY_EVENT = new EventChannel<>();
  public static final EventChannel<EventEnchant> ENCHANT_EVENT = new EventChannel<>();
  public static final EventChannel<EventItemStack> SHEEP_SHEAR_EVENT = new EventChannel<>();
  public static final EventChannel<EventItemStack> SMITHING_TABLE_EVENT = new EventChannel<>();

  // CRAFTING EVENT
  public static final EventChannel<EventItemStack> CRAFTING_EVENT = new EventChannel<>();
  // SMELTING EVENT
  public static final EventChannel<EventItemStack> SMELTING_EVENT = new EventChannel<>();

  public static void register() {
    // Registered via mixins to support correct quantities and shift-clicking
  }
}
