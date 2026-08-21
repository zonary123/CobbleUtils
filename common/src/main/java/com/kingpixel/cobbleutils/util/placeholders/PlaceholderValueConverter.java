package com.kingpixel.cobbleutils.util.placeholders;

import com.kingpixel.cobbleutils.util.AdventureTranslator;
import eu.pb4.placeholders.api.PlaceholderResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
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
      if (value instanceof Text t) {
        return t;
      }
      if (value instanceof Component c) {
        return AdventureTranslator.toNative(c, player);
      }
      if (value instanceof String s) {
        return AdventureTranslator.toNative(s, null, player);
      }
      if (value instanceof PlaceholderResult pr) {
        return pr.isValid() ? pr.text() : null;
      }
      return AdventureTranslator.toNative(String.valueOf(value), null, player);
    } catch (Throwable e) {
      return Text.literal(String.valueOf(value));
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
          String json = Text.Serialization.toJsonString(t, net.minecraft.registry.DynamicRegistryManager.EMPTY);
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
  public static PlaceholderResult toPB4Result(@Nullable Object value, @Nullable ServerPlayerEntity player) {
    if (value == null) {
      return PlaceholderResult.invalid();
    }
    try {
      if (value instanceof PlaceholderResult pr) {
        return pr;
      }
      if (value instanceof Text t) {
        return PlaceholderResult.value(t);
      }
      if (value instanceof Component c) {
        Text nativeText = AdventureTranslator.toNative(c, player);
        return nativeText != null ? PlaceholderResult.value(nativeText) : PlaceholderResult.invalid();
      }
      if (value instanceof String s) {
        Text nativeText = AdventureTranslator.toNative(s, null, player);
        return nativeText != null ? PlaceholderResult.value(nativeText) : PlaceholderResult.value(s);
      }
      if (value instanceof Number || value instanceof Boolean) {
        return PlaceholderResult.value(String.valueOf(value));
      }
      Text text = toNativeText(value, player);
      return text != null ? PlaceholderResult.value(text) : PlaceholderResult.value(String.valueOf(value));
    } catch (Throwable e) {
      return PlaceholderResult.value(String.valueOf(value));
    }
  }

  /**
   * Safely converts an arbitrary object to a plain String.
   */
  public static @Nullable String toStringValue(@Nullable Object value) {
    if (value == null) return null;
    try {
      if (value instanceof String s) return s;
      if (value instanceof Text t) return t.getString();
      if (value instanceof Component c) return AdventureTranslator.miniMessage.serialize(c);
      return String.valueOf(value);
    } catch (Throwable e) {
      return String.valueOf(value);
    }
  }
}
