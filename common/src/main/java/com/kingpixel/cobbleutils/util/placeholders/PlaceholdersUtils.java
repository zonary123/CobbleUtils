package com.kingpixel.cobbleutils.util.placeholders;

import com.kingpixel.cobbleutils.util.AdventureTranslator;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import io.github.miniplaceholders.api.MiniPlaceholders;
import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;

/**
 * Central orchestrator and utility manager for unified placeholder
 * registrations.
 */
public final class PlaceholdersUtils {
  private static final Logger LOGGER = LogManager.getLogger("CobbleUtils-Placeholders");

  @Getter
  private static final List<PlaceholderProvider> providers = new CopyOnWriteArrayList<>();
  @Getter
  private static final InternalPlaceholderProvider internalProvider = new InternalPlaceholderProvider();
  @Getter
  private static final PB4PlaceholderProvider pb4Provider = new PB4PlaceholderProvider();
  @Getter
  private static final MiniPlaceholdersProvider miniPlaceholdersProvider = new MiniPlaceholdersProvider();

  static {
    // Register defaults safely
    try {
      providers.add(internalProvider);
      if (pb4Provider.isAvailable()) {
        providers.add(pb4Provider);
        LOGGER.info("Registered PB4 PlaceholderAPI provider.");
      }
      if (miniPlaceholdersProvider.isAvailable()) {
        providers.add(miniPlaceholdersProvider);
        LOGGER.info("Registered MiniPlaceholders provider.");
      }
    } catch (Throwable e) {
      LOGGER.error("Error initializing placeholder providers", e);
    }
  }

  private PlaceholdersUtils() {
  }

  /**
   * Registers a custom placeholder provider.
   */
  public static void registerProvider(@NotNull PlaceholderProvider provider) {
    try {
      if (!providers.contains(provider)) {
        providers.add(provider);
      }
    } catch (Throwable e) {
      LOGGER.error("Failed to register custom placeholder provider: " + provider.getId(), e);
    }
  }

  /**
   * Registers a unified placeholder across all available providers.
   *
   * @param namespace The namespace (e.g. "cobbleutils")
   * @param key       The placeholder key (e.g. "balance")
   * @param handler   The callback handler
   */
  public static void register(
    @NotNull String namespace,
    @NotNull String key,
    @NotNull UnifiedPlaceholderHandler handler) {
    register(namespace, key, handler, true, false);
  }

  /**
   * Registers a unified placeholder with custom audience/relational flags.
   */
  public static void register(
    @NotNull String namespace,
    @NotNull String key,
    @NotNull UnifiedPlaceholderHandler handler,
    boolean isAudience,
    boolean isRelational) {
    for (PlaceholderProvider provider : providers) {
      try {
        if (provider.isAvailable()) {
          provider.register(namespace, key, handler, isAudience, isRelational);
        }
      } catch (Throwable e) {
        LOGGER.error("Error registering placeholder on provider " + provider.getId(), e);
      }
    }
  }

  /**
   * Convenience method to register a player-scoped placeholder.
   */
  public static void registerPlayer(
    @NotNull String namespace,
    @NotNull String key,
    @NotNull SimplePlayerPlaceholder handler) {
    register(namespace, key, ctx -> handler.handle(ctx.getPlayer(), ctx.getArgument()), true, false);
  }

  /**
   * Convenience method to register a global placeholder.
   */
  public static void registerGlobal(
    @NotNull String namespace,
    @NotNull String key,
    @NotNull SimpleGlobalPlaceholder handler) {
    register(namespace, key, ctx -> handler.handle(ctx.getArgument()), false, false);
  }

  /**
   * Convenience method to register a relational placeholder between two players.
   */
  public static void registerRelational(
    @NotNull String namespace,
    @NotNull String key,
    @NotNull RelationalPlaceholderHandler handler) {
    register(namespace, key, ctx -> handler.handle(ctx.getPlayer(), ctx.getTargetPlayer(), ctx.getArgument()), true,
      true);
  }

  /**
   * Convenience method to register a placeholder expecting a specific target
   * object.
   */
  public static <T> void registerObject(
    @NotNull String namespace,
    @NotNull String key,
    @NotNull Class<T> targetClass,
    @NotNull BiFunction<T, String, Object> handler) {
    register(namespace, key, ctx -> {
      T target = ctx.targetAs(targetClass).orElse(null);
      if (target == null)
        return null;
      return handler.apply(target, ctx.getArgument());
    }, true, false);
  }

  /**
   * Unregisters a specific placeholder from all providers.
   */
  public static void unregister(@NotNull String namespace, @NotNull String key) {
    for (PlaceholderProvider provider : providers) {
      try {
        if (provider.isAvailable()) {
          provider.unregister(namespace, key);
        }
      } catch (Throwable e) {
        LOGGER.error("Error unregistering placeholder on provider " + provider.getId(), e);
      }
    }
  }

  /**
   * Unregisters all placeholders in a namespace from all providers.
   */
  public static void unregisterNamespace(@NotNull String namespace) {
    for (PlaceholderProvider provider : providers) {
      try {
        if (provider.isAvailable()) {
          provider.unregisterNamespace(namespace);
        }
      } catch (Throwable e) {
        LOGGER.error("Error unregistering namespace on provider " + provider.getId(), e);
      }
    }
  }

  // ==========================================
  // Parsing & Resolution Utilities
  // ==========================================

  /**
   * Evaluates placeholders within a String message using available engines
   * (Internal, PB4, MiniPlaceholders).
   */
  public static String parseString(@Nullable String message, @Nullable ServerPlayerEntity player) {
    if (message == null || message.isEmpty())
      return "";
    try {
      CobblePlaceholderContext ctx = CobblePlaceholderContext.of(player);
      return internalProvider.replace(message, ctx);
    } catch (Throwable e) {
      LOGGER.error("Error parsing string placeholders", e);
      return message;
    }
  }

  /**
   * Evaluates placeholders within a native Minecraft Text object.
   */
  public static Text parseText(@NotNull Text text, @Nullable ServerPlayerEntity player) {
    try {
      if (player != null && isPB4Available()) {
        return Placeholders.parseText(text, PlaceholderContext.of(player));
      }
    } catch (Throwable e) {
      LOGGER.error("Error parsing text with PB4 PlaceholderAPI", e);
    }
    return text;
  }

  /**
   * Deserializes a MiniMessage string with MiniPlaceholders tag resolvers enabled
   * for a ServerPlayerEntity.
   */
  public static Component parseMiniMessage(@NotNull String miniMessageText, @Nullable ServerPlayerEntity player) {
    if (miniMessageText.isEmpty())
      return Component.empty();
    try {
      if (isMiniPlaceholdersAvailable()) {
        Audience audience = player instanceof Audience aud ? aud : null;
        TagResolver resolver = audience != null
          ? MiniPlaceholders.getAudienceGlobalPlaceholders(audience)
          : MiniPlaceholders.getGlobalPlaceholders();
        return AdventureTranslator.miniMessage.deserialize(miniMessageText, resolver);
      }
    } catch (Throwable e) {
      LOGGER.error("Error parsing MiniMessage with MiniPlaceholders", e);
    }
    return AdventureTranslator.miniMessage.deserialize(miniMessageText);
  }

  /**
   * Deserializes a MiniMessage string with MiniPlaceholders tag resolvers enabled
   * for an Audience.
   */
  public static Component parseMiniMessage(@NotNull String miniMessageText, @Nullable Audience audience) {
    if (miniMessageText.isEmpty())
      return Component.empty();
    try {
      if (isMiniPlaceholdersAvailable()) {
        TagResolver resolver = audience != null
          ? MiniPlaceholders.getAudienceGlobalPlaceholders(audience)
          : MiniPlaceholders.getGlobalPlaceholders();
        return AdventureTranslator.miniMessage.deserialize(miniMessageText, resolver);
      }
    } catch (Throwable e) {
      LOGGER.error("Error parsing MiniMessage with MiniPlaceholders", e);
    }
    return AdventureTranslator.miniMessage.deserialize(miniMessageText);
  }

  public static boolean isPB4Available() {
    return pb4Provider.isAvailable();
  }

  public static boolean isMiniPlaceholdersAvailable() {
    return miniPlaceholdersProvider.isAvailable();
  }
}
