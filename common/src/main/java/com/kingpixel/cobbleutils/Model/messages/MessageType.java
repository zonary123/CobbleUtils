package com.kingpixel.cobbleutils.Model.messages;

/**
 * @author Carlos Varas Alonso - 21/09/2025 22:04
 */

public enum MessageType {
  CHAT("chat"),
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
      case "chat" -> CHAT;
      case "broadcast" -> BROADCAST;
      case "actionbar" -> ACTIONBAR;
      case "actionbar_broadcast" -> ACTIONBAR_BROADCAST;
      case "bossBar" -> BOSSBAR;
      case "bossbarbroadcast" -> BOSSBAR_BROADCAST;
      case "titlesubtitle" -> TITLE_SUBTITLE;
      case "titlesubtitlebroadcast" -> TITLE_SUBTITLE_BROADCAST;
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

