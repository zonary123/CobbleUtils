package com.kingpixel.cobbleutils.Model.messages;

/**
 * @author Carlos Varas Alonso - 21/09/2025 22:04
 */

public enum MessageType {
  CHAT("chat"),
  CHAT_BROADCAST("chat_broadcast"),
  BROADCAST("broadcast"),
  ACTIONBAR("actionbar"),
  ACTIONBAR_BROADCAST("actionbar_broadcast"),
  BOSSBAR("bossBar"),
  BOSSBAR_BROADCAST("bossbarbroadcast"),
  TITLE_SUBTITLE("titlesubtitle"),
  TITLE_SUBTITLE_BROADCAST("titlesubtitlebroadcast");

  MessageType(String type) {

  }

  /**
   * Convierte un string a enum (ignora mayúsculas/minúsculas y guiones).
   */
  public static MessageType fromString(String raw) {
    if (raw == null) return null;
    return switch (raw.trim().toLowerCase()) {
      case "chat", "c" -> CHAT;
      case "chat_broadcast", "cb" -> CHAT_BROADCAST;
      case "broadcast", "bc" -> BROADCAST;
      case "actionbar", "ab" -> ACTIONBAR;
      case "actionbar_broadcast", "abb" -> ACTIONBAR_BROADCAST;
      case "bossbar", "bb" -> BOSSBAR;
      case "bossbar_broadcast", "bbb" -> BOSSBAR_BROADCAST;
      case "titlesubtitle", "ts" -> TITLE_SUBTITLE;
      case "titlesubtitle_broadcast", "tsb" -> TITLE_SUBTITLE_BROADCAST;
      default -> null;
    };
  }

  private static String defaults;

  public static String defaults() {
    if (defaults == null) {
      StringBuilder sb = new StringBuilder();
      for (MessageType type : values()) {
        if (!sb.isEmpty()) sb.append(", ");
        sb.append(type.name());
      }
      defaults = sb.toString();
    }
    return defaults;
  }
}

