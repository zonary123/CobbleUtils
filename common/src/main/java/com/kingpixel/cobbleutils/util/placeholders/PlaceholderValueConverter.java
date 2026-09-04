package com.kingpixel.cobbleutils.util.placeholders;

import com.kingpixel.cobbleutils.util.AdventureTranslator;
import eu.pb4.placeholders.api.PlaceholderResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * Robust converter utility that safely transforms arbitrary placeholder return values
 * (Component, Text, String, Tag, PlaceholderResult, Number, etc.) across engines.
 */
public final class PlaceholderValueConverter {

  private PlaceholderValueConverter() {
  }

  /**
   * Safely converts an arbitrary object into a Minecraft Text object.
   */
  public static @Nullable Text toNativeText(@Nullable Object value, @Nullable ServerPlayerEntity player) {
    if (value == null) return null;
    try {
      return switch (value) {
        case Text t -> t;
        case Component c -> AdventureTranslator.toNative(c, player);
        case String s -> AdventureTranslator.toNative(s, null, player);
        case PlaceholderResult pr -> pr.isValid() ? pr.text() : null;
        default -> AdventureTranslator.toNative(String.valueOf(value), null, player);
      };
    } catch (Throwable e) {
      return AdventureTranslator.toNative(String.valueOf(value));
    }
  }

  /**
   * Safely converts an arbitrary object into an Adventure Component.
   */
  public static @Nullable Component toAdventureComponent(@Nullable Object value) {
    if (value == null) return null;
    try {
      if (value instanceof Component c) {
        return c;
      }
      if (value instanceof Text t) {
        try {
          String json = Text.Serialization.toJsonString(t, DynamicRegistryManager.EMPTY);
          return GsonComponentSerializer.gson().deserialize(json);
        } catch (Throwable fallback) {
          return Component.text(t.getString());
        }
      }
      if (value instanceof String s) {
        try {
          return AdventureTranslator.miniMessage.deserialize(s);
        } catch (Throwable fallback) {
          return Component.text(s);
        }
      }
      if (value instanceof PlaceholderResult pr) {
        if (!pr.isValid()) return null;
        if (pr.text() != null) {
          return toAdventureComponent(pr.text());
        }
        return pr.string() != null ? Component.text(pr.string()) : null;
      }
      return Component.text(String.valueOf(value));
    } catch (Throwable e) {
      return Component.text(String.valueOf(value));
    }
  }

  /**
   * Safely converts an arbitrary object into a MiniPlaceholders Tag.
   */
  public static @Nullable Tag toMiniPlaceholdersTag(@Nullable Object value) {
    if (value == null) return null;
    try {
      if (value instanceof Tag t) {
        return t;
      }
      if (value instanceof Component c) {
        return Tag.selfClosingInserting(c);
      }
      if (value instanceof Text t) {
        Component comp = toAdventureComponent(t);
        return comp != null ? Tag.selfClosingInserting(comp) : null;
      }
      if (value instanceof String s) {
        try {
          Component comp = AdventureTranslator.miniMessage.deserialize(s);
          return Tag.selfClosingInserting(comp);
        } catch (Throwable fallback) {
          return Tag.selfClosingInserting(Component.text(s));
        }
      }
      if (value instanceof Number || value instanceof Boolean) {
        return Tag.selfClosingInserting(Component.text(String.valueOf(value)));
      }
      Component comp = toAdventureComponent(value);
      return comp != null ? Tag.selfClosingInserting(comp) : Tag.selfClosingInserting(Component.text(String.valueOf(value)));
    } catch (Throwable e) {
      return Tag.selfClosingInserting(Component.text(String.valueOf(value)));
    }
  }

  /**
   * Safely converts an arbitrary object into a PB4 PlaceholderResult.
   */
  public static PlaceholderResult toPB4Result(Object value, ServerPlayerEntity player) {
    switch (value) {
      case null -> {
        return PlaceholderResult.invalid();
      }
      case Text text -> {
        return PlaceholderResult.value(text);
      }
      case PlaceholderResult result -> {
        return result;
      }
      default -> {
      }
    }
    return PlaceholderResult.value(AdventureTranslator.toNative(String.valueOf(value), ""));
  }

  /**
   * Safely converts an arbitrary object to a plain String.
   */
  public static @Nullable String toStringValue(@Nullable Object value) {
    if (value == null) return null;
    try {
      return switch (value) {
        case String s -> s;
        case Text t -> t.getString();
        case Component c -> AdventureTranslator.miniMessage.serialize(c);
        default -> String.valueOf(value);
      };
    } catch (Throwable e) {
      return String.valueOf(value);
    }
  }
}
