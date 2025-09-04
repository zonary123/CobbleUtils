package com.kingpixel.cobbleutils.util;

/*
 * This file is part of Impactor, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2018-2022 NickImpact
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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

public class AdventureTranslator {
  public static final MiniMessage miniMessage = MiniMessage.miniMessage();
  public static LegacyComponentSerializer legacyComponentSerializer = LegacyComponentSerializer.builder()
    .character('§')
    .character('&')
    .hexColors()
    .build();

  private static RegistryWrapper.WrapperLookup getWrapper() {
    if (CobbleUtils.server != null) {
      return CobbleUtils.server.getRegistryManager();
    } else {
      return null;
    }
  }

  // Memoization of MiniMessage deserialization con Caffeine
  private static final Cache<String, Text> cache = Caffeine.newBuilder()
    .maximumSize(50000) // Max 50,000 entries
    .expireAfterAccess(15, TimeUnit.MINUTES) // Expire after 15 minute of inactivity
    .removalListener((String key, Text value, RemovalCause cause) -> {
      if (CobbleUtils.config.isDebug()) CobbleUtils.LOGGER.info("Removed key from cache: " + key + ", cause: " + cause);
    })
    .build();

  /**
   * Internal method to handle prefix and player placeholders
   *
   * @param text   The text to convert
   * @param prefix Optional prefix
   * @param player Optional player
   *
   * @return The converted text
   */
  private static Text toNativeInternal(String text, @Nullable String prefix, @Nullable ServerPlayerEntity player) {
    String replaced = text.replace("%prefix%", prefix == null ? "" : prefix);

    if (player == null) {
      return cache.get(replaced, e -> {
        Component component = miniMessage.deserialize(replaceNative(e));
        return toNative(component, null);
      });
    }

    return toNative(miniMessage.deserialize(replaceNative(replaced)), player);
  }

  // Sobrecargas públicas
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


  public static Text toNative(Component component, @Nullable ServerPlayerEntity player) {
    component = component.decoration(TextDecoration.ITALIC, false);
    RegistryWrapper.WrapperLookup wrapper = getWrapper();
    MutableText text;
    if (wrapper != null) {
      text = Text.Serialization.fromJson(GsonComponentSerializer.gson().serialize(component),
        wrapper);
    } else {
      text = Text.literal(GsonComponentSerializer.gson().serialize(component));
    }
    if (player != null && text != null) {
      if (Utils.isPlaceholder()) {
        //text = Placeholders.parseText(text, PlaceholderContext.of(player)).copy();
        return Placeholders.parseText(text, PlaceholderContext.of(player));
      }
    }
    return text;
  }


  public static List<Text> toNativeL(List<String> lore) {
    int size = lore.size();
    List<Text> loreString = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      String loreLine = lore.get(i);
      if (loreLine == null || loreLine.isEmpty()) continue;
      loreString.add(toNativeInternal(loreLine, null, null));
    }
    return loreString;
  }

  public static List<Text> toNativeL(List<String> lore, @Nullable ServerPlayerEntity player) {
    int size = lore.size();
    List<Text> loreString = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      String loreLine = lore.get(i);
      if (loreLine == null || loreLine.isEmpty()) continue;
      loreString.add(toNativeInternal(loreLine, null, player));
    }
    return loreString;
  }

  private static final Map<Character, String> COLOR_CODES = Map.ofEntries(
    Map.entry('0', "<black>"),
    Map.entry('1', "<dark_blue>"),
    Map.entry('2', "<dark_green>"),
    Map.entry('3', "<dark_aqua>"),
    Map.entry('4', "<dark_red>"),
    Map.entry('5', "<dark_purple>"),
    Map.entry('6', "<gold>"),
    Map.entry('7', "<gray>"),
    Map.entry('8', "<dark_gray>"),
    Map.entry('9', "<blue>"),
    Map.entry('a', "<green>"),
    Map.entry('b', "<aqua>"),
    Map.entry('c', "<red>"),
    Map.entry('d', "<light_purple>"),
    Map.entry('e', "<yellow>"),
    Map.entry('f', "<white>"),
    Map.entry('k', "<obfuscated>"),
    Map.entry('l', "<bold>"),
    Map.entry('m', "<strikethrough>"),
    Map.entry('n', "<underline>"),
    Map.entry('o', "<italic>"),
    Map.entry('r', "<reset>")
  );

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
    return Text.empty().append(AdventureTranslator.toNative(messageContent));
  }
}