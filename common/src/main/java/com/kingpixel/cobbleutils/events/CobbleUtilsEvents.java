package com.kingpixel.cobbleutils.events;

import com.kingpixel.cobbleutils.events.models.EventBlockBreak;
import com.kingpixel.cobbleutils.events.models.EventBlockPlaced;
import com.kingpixel.cobbleutils.events.models.EventBrewing;

/**
 * @author Carlos Varas Alonso - 26/08/2025 14:42
 */
public class CobbleUtilsEvents {
  public static final EventChannel<EventBrewing> BREWING_EVENT = new EventChannel<>();
  public static final EventChannel<EventBlockBreak> BLOCK_BREAK_EVENT = new EventChannel<>();
  public static final EventChannel<EventBlockPlaced> BLOCK_PLACED_EVENT = new EventChannel<>();

}
