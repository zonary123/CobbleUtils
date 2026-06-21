package com.kingpixel.cobbleutils.events;

import com.kingpixel.cobbleutils.events.models.*;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Set;

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
  private static final Set<String> CRAFTING_BLACKLIST = Set.of(
      " minecraft:diamond_block",
      "minecraft:emerald_block",
      "minecraft:gold_block",
      "minecraft:iron_block",
      "minecraft:netherite_block",
      "minecraft:redstone_block",
      "minecraft:lapis_block",
      "minecraft:coal_block",
      "minecraft:bone_block",
      "minecraft:slime_block",
      "minecraft:dried_kelp_block",
      "minecraft:hay_block",
      "minecraft:quartz_block",
      "minecraft:copper_block",
      "minecraft:raw_copper_block",
      "minecraft:raw_iron_block",
      "minecraft:raw_gold_block");

  public static void register() {
    PlayerEvent.CRAFT_ITEM.register((player, itemStack, inventory) -> {
      if (CRAFTING_EVENT.isEmpty())
        return;
      CRAFTING_EVENT.emit(EventItemStack.builder()
          .itemStack(itemStack)
          .player((ServerPlayerEntity) player)
          .build());
    });
  }
}
