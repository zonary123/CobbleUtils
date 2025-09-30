package com.kingpixel.cobbleutils.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.kingpixel.cobbleutils.CobbleUtils;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Utility class to convert Adventure Components and MiniMessage text into Minecraft-native Text objects.
 * Optimized for multithreading using ThreadLocal for GsonComponentSerializer instances.
 */
public class AdventureTranslator {


  // MiniMessage instance for parsing MiniMessage strings
  public static final MiniMessage miniMessage = MiniMessage.miniMessage();

  // Legacy serializer supporting '&' and '§' color codes
  public static final LegacyComponentSerializer legacyComponentSerializer = LegacyComponentSerializer.builder()
    .character('§')
    .character('&')
    .hexColors()
    .build();

  // Thread-local GsonComponentSerializer to avoid contention in multi-threaded environments
  private static final ThreadLocal<GsonComponentSerializer> threadLocalGson = ThreadLocal.withInitial(GsonComponentSerializer::gson);

  // Caffeine cache for repeated text conversions
  private static final Cache<String, Text> cache = Caffeine.newBuilder()
    .maximumSize(250_000)
    .expireAfterAccess(1, TimeUnit.MINUTES)
    .removalListener((String key, Text value, RemovalCause cause) -> {
      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER.info("Removed key from cache: " + key + ", cause: " + cause);
      }
    })
    .build();

  // Mapping Minecraft color codes to MiniMessage tags
  private static final Map<Character, String> COLOR_CODES = Map.ofEntries(
    Map.entry('0', "<black>"), Map.entry('1', "<dark_blue>"), Map.entry('2', "<dark_green>"),
    Map.entry('3', "<dark_aqua>"), Map.entry('4', "<dark_red>"), Map.entry('5', "<dark_purple>"),
    Map.entry('6', "<gold>"), Map.entry('7', "<gray>"), Map.entry('8', "<dark_gray>"),
    Map.entry('9', "<blue>"), Map.entry('a', "<green>"), Map.entry('b', "<aqua>"),
    Map.entry('c', "<red>"), Map.entry('d', "<light_purple>"), Map.entry('e', "<yellow>"),
    Map.entry('f', "<white>"), Map.entry('k', "<obfuscated>"), Map.entry('l', "<bold>"),
    Map.entry('m', "<strikethrough>"), Map.entry('n', "<underline>"), Map.entry('o', "<italic>"),
    Map.entry('r', "<reset>")
  );

  private static final Text EMPTY = Text.literal(" ");

  /**
   * Converts MiniMessage or legacy-formatted text to native Minecraft Text object.
   * Handles optional prefixes and player-specific placeholders.
   */
  private static Text toNativeInternal(String text, @Nullable String prefix, @Nullable ServerPlayerEntity player) {
    if (text == null || text.isBlank()) return EMPTY;
    String replaced = text.replace("%prefix%", prefix == null ? "" : prefix);

    // If no player is provided, cache the conversion
    if (player == null) {
      return cache.get(replaced, e -> {
        Component component = miniMessage.deserialize(replaceNative(e));
        return toNative(component, null);
      });
    }

    // Parse placeholders for specific player
    return toNative(miniMessage.deserialize(replaceNative(replaced)), player);
  }

  // ===========================
  // Public convenience methods
  // ===========================
  public static Text toNativeWithOutPrefix(String text) {
    return toNativeInternal(text, null, null);
  }

  public static Text toNativeWithOutPrefix(String text, @Nullable ServerPlayerEntity player) {
    return toNativeInternal(text, null, player);
  }

  public static Text toNative(String text) {
    return toNativeInternal(text, null, null);
  }

  public static Text toNative(String text, @Nullable String prefix) {
    return toNativeInternal(text, prefix, null);
  }

  public static Text toNative(String text, @Nullable String prefix, @Nullable ServerPlayerEntity player) {
    return toNativeInternal(text, prefix, player);
  }

  /**
   * Converts Adventure Component into Minecraft Text, optionally parsing placeholders for a player.
   */
  public static Text toNative(Component component, @Nullable ServerPlayerEntity player) {
    component = component.decoration(TextDecoration.ITALIC, false);
    RegistryWrapper.WrapperLookup wrapper = getWrapper();
    MutableText text;

    // Use thread-local GsonComponentSerializer to avoid multi-thread contention
    GsonComponentSerializer gsonSerializer = threadLocalGson.get();

    if (wrapper != null) {
      text = Text.Serialization.fromJson(gsonSerializer.serialize(component), wrapper);
    } else {
      text = Text.literal(gsonSerializer.serialize(component));
    }

    if (player != null && text != null && Utils.isPlaceholder()) {
      return Placeholders.parseText(text, PlaceholderContext.of(player));
    }

    return text;
  }

  /**
   * Converts a list of strings to a list of Minecraft Text objects.
   */
  public static List<Text> toNativeL(List<String> lore) {
    List<Text> loreString = new ArrayList<>(lore.size());
    for (String loreLine : lore) {
      loreString.add(toNativeInternal(loreLine, null, null));
    }
    return loreString;
  }

  public static List<Text> toNativeL(List<String> lore, @Nullable ServerPlayerEntity player) {
    List<Text> loreString = new ArrayList<>(lore.size());
    for (String loreLine : lore) {
      loreString.add(toNativeInternal(loreLine, null, player));
    }
    return loreString;
  }

  /**
   * Converts legacy color codes (&/§) to MiniMessage tags
   */
  private static String replaceNative(String text) {
    if (text == null || text.isEmpty()) return "";
    StringBuilder builder = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if ((c == '&' || c == '§') && i + 1 < text.length()) {
        char code = text.charAt(i + 1);
        String tag = COLOR_CODES.get(code);
        if (tag != null) {
          builder.append(tag);
          i++;
          continue;
        }
      }
      builder.append(c);
    }
    return builder.toString();
  }

  public static MutableText toNativeComponent(String messageContent) {
    return Text.empty().append(toNative(messageContent));
  }

  // Helper to get server registry wrapper
  private static RegistryWrapper.WrapperLookup getWrapper() {
    if (CobbleUtils.server != null) {
      return CobbleUtils.server.getRegistryManager();
    } else {
      return null;
    }
  }
}
