package com.kingpixel.cobbleutils.api;

import com.kingpixel.cobbleutils.util.placeholders.*;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

/**
 * Public high-level API to register and resolve placeholders seamlessly across
 * PlaceholderAPI (eu.pb4), MiniPlaceholders, and CobbleUtils internal engines.
 */
public class PlaceholderApi {

  /**
   * Registers a unified placeholder with full mutual context.
   *
   * @param namespace The mod or plugin namespace (e.g., "cobbleutils")
   * @param key       The placeholder key (e.g., "rank", "balance")
   * @param handler   The handler returning a Component, Text, String, Number, Boolean, or Object
   */
  public static void register(
    @NotNull String namespace,
    @NotNull String key,
    @NotNull UnifiedPlaceholderHandler handler
  ) {
    PlaceholdersUtils.register(namespace, key, handler);
  }

  /**
   * Registers a unified placeholder with explicit audience and relational options.
   */
  public static void register(
    @NotNull String namespace,
    @NotNull String key,
    @NotNull UnifiedPlaceholderHandler handler,
    boolean isAudience,
    boolean isRelational
  ) {
    PlaceholdersUtils.register(namespace, key, handler, isAudience, isRelational);
  }

  /**
   * Registers a simple player-scoped placeholder: (player, argument) -> value.
   */
  public static void registerPlayer(
    @NotNull String namespace,
    @NotNull String key,
    @NotNull SimplePlayerPlaceholder handler
  ) {
    PlaceholdersUtils.registerPlayer(namespace, key, handler);
  }

  /**
   * Registers a simple global placeholder: (argument) -> value.
   */
  public static void registerGlobal(
    @NotNull String namespace,
    @NotNull String key,
    @NotNull SimpleGlobalPlaceholder handler
  ) {
    PlaceholdersUtils.registerGlobal(namespace, key, handler);
  }

  /**
   * Registers a relational placeholder comparing two players: (player1, player2, argument) -> value.
   */
  public static void registerRelational(
    @NotNull String namespace,
    @NotNull String key,
    @NotNull RelationalPlaceholderHandler handler
  ) {
    PlaceholdersUtils.registerRelational(namespace, key, handler);
  }

  /**
   * Registers a placeholder for a specific mutual context object (e.g. Pokemon, ItemStack, Party).
   *
   * @param namespace   The namespace
   * @param key         The placeholder key
   * @param targetClass The class type of the target object
   * @param handler     The handler receiving the target instance and argument string
   */
  public static <T> void registerObject(
    @NotNull String namespace,
    @NotNull String key,
    @NotNull Class<T> targetClass,
    @NotNull BiFunction<T, String, Object> handler
  ) {
    PlaceholdersUtils.registerObject(namespace, key, targetClass, handler);
  }

  /**
   * Unregisters a placeholder across all engines.
   */
  public static void unregister(@NotNull String namespace, @NotNull String key) {
    PlaceholdersUtils.unregister(namespace, key);
  }

  /**
   * Unregisters all placeholders in a namespace across all engines.
   */
  public static void unregisterNamespace(@NotNull String namespace) {
    PlaceholdersUtils.unregisterNamespace(namespace);
  }

  /**
   * Evaluates placeholders within a plain String.
   */
  public static String parseString(@Nullable String message, @Nullable ServerPlayerEntity player) {
    return PlaceholdersUtils.parseString(message, player);
  }

  /**
   * Evaluates placeholders within a native Minecraft Text object.
   */
  public static Text parseText(@NotNull Text text, @Nullable ServerPlayerEntity player) {
    return PlaceholdersUtils.parseText(text, player);
  }

  /**
   * Parses MiniMessage text with MiniPlaceholders support for a player.
   */
  public static Component parseMiniMessage(@NotNull String miniMessageText, @Nullable ServerPlayerEntity player) {
    return PlaceholdersUtils.parseMiniMessage(miniMessageText, player);
  }

  /**
   * Parses MiniMessage text with MiniPlaceholders support for an audience.
   */
  public static Component parseMiniMessage(@NotNull String miniMessageText, @Nullable Audience audience) {
    return PlaceholdersUtils.parseMiniMessage(miniMessageText, audience);
  }

  /**
   * Verifies if PB4 PlaceholderAPI is loaded and active.
   */
  public static boolean isPB4Available() {
    return PlaceholdersUtils.isPB4Available();
  }

  /**
   * Verifies if MiniPlaceholders is loaded and active.
   */
  public static boolean isMiniPlaceholdersAvailable() {
    return PlaceholdersUtils.isMiniPlaceholdersAvailable();
  }
}
