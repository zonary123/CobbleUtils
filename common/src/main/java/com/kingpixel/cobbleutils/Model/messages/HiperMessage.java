package com.kingpixel.cobbleutils.Model.messages;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.*;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import lombok.Data;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class HiperMessage implements JsonSerializer<HiperMessage>, JsonDeserializer<HiperMessage> {
  public static final HiperMessage EMPTY = new HiperMessage("", MessageType.CHAT);

  private String rawMessage;
  transient
  private String content;
  transient
  private MessageType type;
  transient
  private long hash;

  public HiperMessage(String message, MessageType defaultType) {
    this.rawMessage = message;
    this.type = defaultType;
    this.hash = 0;
  }

  public void sendMessage(ServerPlayerEntity player, boolean cache) {
    sendMessage(player.getUuid(), cache);
  }

  public void sendMessage(UUID playerUUID, boolean cache) {
    if (rawMessage == null || rawMessage.isEmpty()) return;
    CompletableFuture.runAsync(() -> {
        if (rawMessage.hashCode() != hash) {
          String[] parts = rawMessage.split(":", 2);
          if (parts.length < 2) return;

          type = MessageType.fromString(parts[0]);
          content = parts[1];
          hash = rawMessage.hashCode();
          if (type == null) {
            type = MessageType.CHAT;
            System.out.println("[HiperMessage] Unknown message type in rawMessage: " + rawMessage + ". Defaulting to CHAT." +
              " Valid types are: " + MessageType.defaults());
          }
        }
        switch (type) {
          case CHAT -> sendChat(playerUUID, content, cache);
          case BROADCAST -> sendBroadcast(content, cache);
          case ACTIONBAR -> sendActionBar(playerUUID, content, cache);
          case ACTIONBAR_BROADCAST -> sendActionBarBroadcast(content, cache);
          case BOSSBAR -> sendBossBar(playerUUID, content, cache);
          case BOSSBAR_BROADCAST -> sendBossBarBroadcast(content, cache);
          case TITLE_SUBTITLE -> sendTitleSubtitle(playerUUID, content, cache);
          case TITLE_SUBTITLE_BROADCAST -> sendTitleSubtitleBroadcast(content, cache);
        }
      }, CobbleUtils.EXECUTOR_COBBLEUTILS)
      .exceptionally(e -> {
        e.printStackTrace();
        return null;
      });
  }

  private ServerPlayerEntity getPlayer(UUID playerUUID) {
    return CobbleUtils.server.getPlayerManager().getPlayer(playerUUID);
  }

  //==========================================================================//
  //                                   CHAT
  //==========================================================================//

  /**
   * Send a chat message to a player.
   *
   * @param playerUUID the UUID of the player to send the message to
   * @param content    the content of the message
   * @param cache      whether to cache the message or not
   */
  private void sendChat(UUID playerUUID, String content, boolean cache) {
    ServerPlayerEntity player = getPlayer(playerUUID);
    if (player == null) return;
    Text message = AdventureTranslator.toNative(content);
    player.sendMessage(message, false);
  }

  private void sendBroadcast(String content, boolean cache) {
    Text message = AdventureTranslator.toNative(content);
    CobbleUtils.server.getPlayerManager().broadcast(message, false);
  }

  //==========================================================================//
  //                                 ACTION BAR
  //==========================================================================//
  private void sendActionBar(UUID player, String content, boolean cache) {
    ServerPlayerEntity p = getPlayer(player);
    if (p == null) return;
    Text message = AdventureTranslator.toNative(content);
    p.sendMessage(message, true);
  }

  private void sendActionBarBroadcast(String content, boolean cache) {
    CobbleUtils.server.getPlayerManager().broadcast(AdventureTranslator.toNative(content), true);
  }

  //==========================================================================//
  //                                   BOSS BAR
  //==========================================================================//

  private void sendBossBar(UUID player, String content, boolean cache) {

  }

  private void sendBossBarBroadcast(String content, boolean cache) {

  }

  //==========================================================================//
  //                                   TITLE & SUBTITLE
  //==========================================================================//

  private static final Pattern TITLE_SUBTITLE_PATTERN =
    Pattern.compile("(?:title:(?<title>.*?))?(?:\\s*subtitle:(?<subtitle>.*))?", Pattern.CASE_INSENSITIVE);

  private record TitleSubtitle(String title, String subtitle) {
    public static Cache<String, TitleS2CPacket> titlePacketCache = Caffeine.newBuilder()
      .maximumSize(1_000)
      .expireAfterAccess(1, TimeUnit.MINUTES)
      .build();
    public static Cache<String, SubtitleS2CPacket> subtitlePacketCache = Caffeine.newBuilder()
      .maximumSize(1_000)
      .expireAfterAccess(1, TimeUnit.MINUTES)
      .build();

    public TitleS2CPacket getTitlePacker() {
      return titlePacketCache.get(title, t -> new TitleS2CPacket(AdventureTranslator.toNative(t)));
    }

    public SubtitleS2CPacket getSubtitlePacker() {
      return subtitlePacketCache.get(subtitle, s -> new SubtitleS2CPacket(AdventureTranslator.toNative(s)));
    }

    public void sendTo(ServerPlayerEntity player) {
      if (title != null && !title.isEmpty()) {
        player.networkHandler.sendPacket(getTitlePacker());
      }
      if (subtitle != null && !subtitle.isEmpty()) {
        player.networkHandler.sendPacket(getSubtitlePacker());
      }
    }

  }

  private TitleSubtitle parseTitleSubtitle(String content) {
    String title = "";
    String subtitle = "";

    Matcher matcher = TITLE_SUBTITLE_PATTERN.matcher(content);
    if (matcher.matches()) {
      if (matcher.group("title") != null) {
        title = matcher.group("title").trim();
      }
      if (matcher.group("subtitle") != null) {
        subtitle = matcher.group("subtitle").trim();
      }
    }
    return new TitleSubtitle(title, subtitle);
  }

  private void sendTitleSubtitle(UUID player, String content, boolean cache) {
    TitleSubtitle titleSubtitle = parseTitleSubtitle(content);
    ServerPlayerEntity p = getPlayer(player);
    if (p == null) return;
    titleSubtitle.sendTo(p);
  }

  private void sendTitleSubtitleBroadcast(String content, boolean cache) {
    TitleSubtitle titleSubtitle = parseTitleSubtitle(content);
    var players = CobbleUtils.server.getPlayerManager().getPlayerList();
    for (ServerPlayerEntity player : players) {
      titleSubtitle.sendTo(player);
    }
  }

  //==========================================================================//
  //                           GSON SERIALIZER/ DESERIALIZER
  //==========================================================================//

  @Override
  public HiperMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
      return new HiperMessage(json.getAsString(), null);
    }
    throw new JsonParseException("Expected string for HiperMessage but got: " + json);
  }

  @Override
  public JsonElement serialize(HiperMessage src, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(src.getRawMessage());
  }
}
