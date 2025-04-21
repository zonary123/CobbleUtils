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

import com.kingpixel.cobbleutils.CobbleUtils;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class AdventureTranslator {
  public static final MiniMessage miniMessage = MiniMessage.miniMessage();
  public static LegacyComponentSerializer legacyComponentSerializer = LegacyComponentSerializer.builder()
    .character('§')
    .hexColors()
    .build();

  private static RegistryWrapper.WrapperLookup wrapperLookup;

  private static RegistryWrapper.WrapperLookup getWrapper() {
    try {
      if (wrapperLookup == null) {
        wrapperLookup = BuiltinRegistries.createWrapperLookup();
        if (wrapperLookup == null) {
          wrapperLookup = new RegistryWrapper.WrapperLookup() {
            @Override public Stream<RegistryKey<? extends Registry<?>>> streamAllRegistryKeys() {
              return Stream.empty();
            }

            @Override
            public <T> Optional<RegistryWrapper.Impl<T>> getOptionalWrapper(RegistryKey<? extends Registry<? extends T>> registryRef) {
              return Optional.empty();
            }
          };
        }
      }
      return wrapperLookup;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static Text toNativeWithOutPrefix(String text) {
    return toNative(miniMessage.deserialize(replaceNative(text)
      .replace("%prefix%", "")
      .replace("%partyprefix%", "")), null);
  }

  public static Text toNativeWithOutPrefix(String text, @Nullable ServerPlayerEntity player) {
    return toNative(miniMessage.deserialize(replaceNative(text)
      .replace("%prefix%", "")
      .replace("%partyprefix%", "")), player);
  }

  public static Text toNative(String text) {
    return toNative(miniMessage.deserialize(replaceNative(text
      .replace("%prefix%", CobbleUtils.config.getPrefix()))), null);
  }

  public static Text toNative(String text, @Nullable String prefix) {
    return toNative(miniMessage.deserialize(replaceNative(text
      .replace("%prefix%", prefix == null ? "" : prefix))), null);
  }

  public static Text toNative(String text, @Nullable String prefix, @Nullable ServerPlayerEntity player) {
    return toNative(miniMessage.deserialize(replaceNative(text
      .replace("%prefix%", prefix == null ? "" : prefix))), player);
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
      if (isPlaceholder()) {
        text = Placeholders.parseText(text, PlaceholderContext.of(player)).copy();
      }
    }
    return text;
  }


  private static boolean isPlaceholder() {
    try {
      Class.forName("eu.pb4.placeholders.api.Placeholders");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  public static List<Text> toNativeL(List<String> lore) {
    List<Text> loreString = new ArrayList<>(lore.size());
    for (String loreLine : lore) {
      loreString.add(toNative(miniMessage.deserialize(replaceNative(loreLine)), null));
    }

    return loreString;
  }

  public static List<Text> toNativeL(List<String> lore, @Nullable ServerPlayerEntity player) {
    List<Text> loreString = new ArrayList<>(lore.size());
    for (String loreLine : lore) {
      loreString.add(toNative(miniMessage.deserialize(replaceNative(loreLine)), player));
    }
    return loreString;
  }

  private static String replaceNative(String text) {
    if (text == null || text.isEmpty()) return "";

    if (text.contains("§") || text.contains("&")) {
      text = text
        .replace("&", "§")
        .replace("§0", "<black>")
        .replace("§1", "<dark_blue>")
        .replace("§2", "<dark_green>")
        .replace("§3", "<dark_aqua>")
        .replace("§4", "<dark_red>")
        .replace("§5", "<dark_purple>")
        .replace("§6", "<gold>")
        .replace("§7", "<gray>")
        .replace("§8", "<dark_gray>")
        .replace("§9", "<blue>")
        .replace("§a", "<green>")
        .replace("§b", "<aqua>")
        .replace("§c", "<red>")
        .replace("§d", "<light_purple>")
        .replace("§e", "<yellow>")
        .replace("§f", "<white>")
        .replace("§k", "<obfuscated>")
        .replace("§l", "<bold>")
        .replace("§m", "<strikethrough>")
        .replace("§n", "<underline>")
        .replace("§o", "<italic>")
        .replace("§r", "<reset>");
    }


    return text;
  }

  public static MutableText toNativeComponent(String messageContent) {
    return Text.empty().append(AdventureTranslator.toNative(messageContent));
  }
}