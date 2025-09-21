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

  /**
   * Sends the message to the specified player.
   *
   * @param player the player to send the message to
   * @param cache  whether to cache the message or not
   */
  public void sendMessage(ServerPlayerEntity player, boolean cache) {
    sendMessage(player.getUuid(), cache);
  }

  /**
   * Sends the message to the specified player.
   *
   * @param playerUUID the UUID of the player to send the message to
   * @param cache      whether to cache the message or not
   */
  public void sendMessage(UUID playerUUID, boolean cache) {
    if (rawMessage == null || rawMessage.isEmpty()) return;
    sendMessage(playerUUID, cache, false);
  }

  /**
   * Sends the message to the specified player.
   *
   * @param playerUUID       the UUID of the player to send the message to
   * @param cache            whether to cache the message or not
   * @param receivedForRedis whether the message was received from Redis or not
   */
  public void sendMessage(UUID playerUUID, boolean cache, boolean receivedForRedis) {
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

  /**
   * Gets the player entity from the server using the player's UUID.
   *
   * @param playerUUID the UUID of the player
   *
   * @return the player entity, or null if the player is not online
   */
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

  /**
   * Broadcast a chat message to all players.
   *
   * @param content the content of the message
   * @param cache   whether to cache the message or not
   */
  private void sendBroadcast(String content, boolean cache) {
    Text message = AdventureTranslator.toNative(content);
    CobbleUtils.server.getPlayerManager().broadcast(message, false);
  }

  //==========================================================================//
  //                                 ACTION BAR
  //==========================================================================//

  /**
   * Send an action bar message to a player.
   *
   * @param playerUUID the UUID of the playerUUID to send the message to
   * @param content    the content of the message
   * @param cache      whether to cache the message or not
   */
  private void sendActionBar(UUID playerUUID, String content, boolean cache) {
    ServerPlayerEntity p = getPlayer(playerUUID);
    if (p == null) return;
    Text message = AdventureTranslator.toNative(content);
    p.sendMessage(message, true);
  }

  /**
   * Broadcast an action bar message to all players.
   *
   * @param content the content of the message
   * @param cache   whether to cache the message or not
   */
  private void sendActionBarBroadcast(String content, boolean cache) {
    CobbleUtils.server.getPlayerManager().broadcast(AdventureTranslator.toNative(content), true);
  }

  //==========================================================================//
  //                                   BOSS BAR
  //==========================================================================//

  /**
   * Send a boss bar message to a player.
   *
   * @param player  UUID
   * @param content the content of the message
   * @param cache   whether to cache the message or not
   */
  private void sendBossBar(UUID player, String content, boolean cache) {

  }

  /**
   * Broadcast a boss bar message to all players.
   *
   * @param content the content of the message
   * @param cache   whether to cache the message or not
   */
  private void sendBossBarBroadcast(String content, boolean cache) {

  }

  //==========================================================================//
  //                                   TITLE & SUBTITLE
  //==========================================================================//

  // Pattern to match title and subtitle in the format "title:Your Title Here subtitle:Your Subtitle Here"
  private static final Pattern TITLE_SUBTITLE_PATTERN =
    Pattern.compile("(?:title:(?<title>.*?))?(?:\\s*subtitle:(?<subtitle>.*))?", Pattern.CASE_INSENSITIVE);

  /**
   * Record to hold title and subtitle strings, along with cached packets for efficiency.
   *
   * @param title    the title text
   * @param subtitle the subtitle text
   */
  private record TitleSubtitle(String title, String subtitle) {
    /**
     * Cache for storing TitleS2CPacket and SubtitleS2CPacket instances to avoid redundant packet creation.
     * The cache automatically expires entries after 1 minute of inactivity and has a maximum size of
     */
    public static Cache<String, TitleS2CPacket> titlePacketCache = Caffeine.newBuilder()
      .maximumSize(1_000)
      .expireAfterAccess(1, TimeUnit.MINUTES)
      .build();
    public static Cache<String, SubtitleS2CPacket> subtitlePacketCache = Caffeine.newBuilder()
      .maximumSize(1_000)
      .expireAfterAccess(1, TimeUnit.MINUTES)
      .build();

    /**
     * Gets the TitleS2CPacket for the title, using the cache if available.
     *
     * @return the TitleS2CPacket instance
     */
    public TitleS2CPacket getTitlePacker() {
      return titlePacketCache.get(title, t -> new TitleS2CPacket(AdventureTranslator.toNative(t)));
    }

    /**
     * Gets the SubtitleS2CPacket for the subtitle, using the cache if available.
     *
     * @return the SubtitleS2CPacket instance
     */
    public SubtitleS2CPacket getSubtitlePacker() {
      return subtitlePacketCache.get(subtitle, s -> new SubtitleS2CPacket(AdventureTranslator.toNative(s)));
    }

    /**
     * Sends the title and subtitle to the specified player.
     *
     * @param player the player to send the title and subtitle to
     */
    public void sendTo(ServerPlayerEntity player) {
      if (title != null && !title.isEmpty()) {
        player.networkHandler.sendPacket(getTitlePacker());
      }
      if (subtitle != null && !subtitle.isEmpty()) {
        player.networkHandler.sendPacket(getSubtitlePacker());
      }
    }

  }

  /**
   * Parses the content string to extract title and subtitle using regex.
   *
   * @param content the content string containing title and subtitle
   *
   * @return a TitleSubtitle object containing the parsed title and subtitle
   */
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

  /**
   * Send a title and subtitle message to a player.
   *
   * @param player  UUID
   * @param content the content of the message
   * @param cache   whether to cache the message or not
   */
  private void sendTitleSubtitle(UUID player, String content, boolean cache) {
    TitleSubtitle titleSubtitle = parseTitleSubtitle(content);
    ServerPlayerEntity p = getPlayer(player);
    if (p == null) return;
    titleSubtitle.sendTo(p);
  }

  /**
   * Broadcast a title and subtitle message to all players.
   *
   * @param content the content of the message
   * @param cache   whether to cache the message or not
   */
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

  /**
   * Custom deserializer for HiperMessage that expects a JSON string.
   * If the JSON element is not a string, it throws a JsonParseException.
   *
   * @param json    the JSON element to deserialize
   * @param typeOfT the type of the object to deserialize to
   * @param context the context for deserialization
   *
   * @return a HiperMessage object created from the JSON string
   *
   * @throws JsonParseException if the JSON element is not a string
   */
  @Override
  public HiperMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
      return new HiperMessage(json.getAsString(), null);
    }
    throw new JsonParseException("Expected string for HiperMessage but got: " + json);
  }

  /**
   * Custom serializer for HiperMessage that converts the object to a JSON string.
   *
   * @param src       the HiperMessage object to serialize
   * @param typeOfSrc the type of the source object
   * @param context   the context for serialization
   *
   * @return a JsonElement representing the HiperMessage as a JSON string
   */
  @Override
  public JsonElement serialize(HiperMessage src, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(src.getRawMessage());
  }
}
