package com.kingpixel.cobbleutils.Model.messages;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.*;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.RedisManager;
import lombok.Data;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.*;
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

  public HiperMessage(String message, @Nullable MessageType defaultType) {
    this.rawMessage = message;
    this.type = defaultType;
  }

  /**
   * Sends the message to the specified player.
   *
   * @param player the player to send the message to
   * @param prefix Prefix to add before the message
   * @param cache  whether to cache the message or not
   */
  public void sendMessage(ServerPlayerEntity player, String prefix, boolean cache) {
    sendMessage(player == null ? null : player.getUuid(), prefix, cache, false, null, null);
  }

  /**
   * Sends the message to the specified list of players.
   *
   * @param players List of UUIDs of players to send the message to
   * @param prefix  Prefix to add before the message
   * @param cache   whether to cache the message or not
   */
  public void sendMessage(List<UUID> players, String prefix, boolean cache) {
    for (UUID player : players) {
      sendMessage(player, prefix, cache, false, null, null);
    }
  }

  /**
   * Sends the message to the specified list of players.
   *
   * @param players         List of UUIDs of players to send the message to
   * @param prefix          Prefix to add before the message
   * @param cache           whether to cache the message or not
   * @param modifiedContent Modified content to send instead of the original one (after placeholder replacement)
   */
  public void sendMessage(List<UUID> players, String prefix, boolean cache, String modifiedContent) {
    for (UUID player : players) {
      sendMessage(player, prefix, cache, false, null, modifiedContent);
    }
  }

  /**
   * Sends the message to the specified player.
   *
   * @param player       the player to send the message to
   * @param prefix       Prefix to add before the message
   * @param cache        whether to cache the message or not
   * @param placeholders Map of placeholders to replace in the message (Save the Map in a variable to avoid creating a new one each time)
   */
  public void sendMessage(ServerPlayerEntity player, String prefix, boolean cache, Map<String, String> placeholders) {
    sendMessage(player == null ? null : player.getUuid(), prefix, cache, false, placeholders, null);
  }

  /**
   * Sends the message to the specified player.
   *
   * @param player          the player to send the message to
   * @param modifiedContent Modified content to send instead of the original one (after placeholder replacement)
   * @param prefix          Prefix to add before the message
   * @param cache           whether to cache the message or not
   */
  public void sendMessage(ServerPlayerEntity player, String modifiedContent, String prefix, boolean cache) {
    sendMessage(player == null ? null : player.getUuid(), prefix, cache, false, null, modifiedContent);
  }

  /**
   * Sends the message to the specified player.
   *
   * @param playerUUID      the UUID of the player to send the message to
   * @param modifiedContent Modified content to send instead of the original one (after placeholder replacement)
   * @param prefix          Prefix to add before the message
   * @param cache           whether to cache the message or not
   */
  public void sendMessage(UUID playerUUID, String modifiedContent, String prefix, boolean cache) {
    sendMessage(playerUUID, prefix, cache, false, null, modifiedContent);
  }

  /**
   * Sends the message to the specified player.
   *
   * @param playerUUID the UUID of the player to send the message to
   * @param prefix     Prefix to add before the message
   * @param cache      whether to cache the message or not
   */
  public void sendMessage(UUID playerUUID, String prefix, boolean cache) {
    sendMessage(playerUUID, prefix, cache, false, null, null);
  }

  /**
   * Sends the message to the specified player.
   *
   * @param playerUUID   the UUID of the player to send the message to
   * @param prefix       Prefix to add before the message
   * @param cache        whether to cache the message or not
   * @param placeholders Map of placeholders to replace in the message (Save the Map in a variable to avoid creating a new one each time)
   */
  public void sendMessage(UUID playerUUID, String prefix, boolean cache, Map<String, String> placeholders) {
    sendMessage(playerUUID, prefix, cache, false, placeholders, null);
  }

  /**
   * Replaces placeholders in the content string with their corresponding values from the placeholders map.
   *
   * @param content      the original content string containing placeholders
   * @param placeholders a map of placeholders and their corresponding replacement values
   *
   * @return the content string with all placeholders replaced by their values
   */
  private String replacePlaceholders(String content, Map<String, String> placeholders) {
    if (content == null || content.isEmpty() || placeholders == null || placeholders.isEmpty()) {
      return content;
    }
    StringBuilder sb = new StringBuilder(content);

    var entries = placeholders.entrySet();
    for (var entry : entries) {
      String key = entry.getKey();
      String value = entry.getValue() != null ? entry.getValue() : "";
      int index = sb.indexOf(key);

      while (index != -1) {
        sb.replace(index, index + key.length(), value);
        index = sb.indexOf(key, index + value.length());
      }
    }

    return sb.toString();
  }


  transient
  private String modifiedContent;

  /**
   * Sends the message to the specified player.
   *
   * @param playerUUID        the UUID of the player to send the message to
   * @param cache             whether to cache the message or not
   * @param receivedFromRedis whether the message was received from Redis or not
   */
  public void sendMessage(UUID playerUUID, String prefix, boolean cache, boolean receivedFromRedis,
                          Map<String, String> placeholders, String modifiedContent) {
    if (rawMessage == null || rawMessage.isEmpty()) return;
    this.modifiedContent = Objects.requireNonNullElseGet(modifiedContent, () -> rawMessage);
    CobbleUtils.runAsync(() -> {
      if (content == null || content.isEmpty() || modifiedContent != null || type == null) {
        String[] parts;
        if (modifiedContent != null) {
          parts = modifiedContent.split(":", 2);
        } else {
          parts = rawMessage.split(":", 2);
        }
        if (parts.length < 2) return;

        type = MessageType.fromString(parts[0]);
        content = parts[1];
        if (type == null) {
          type = MessageType.CHAT;
          CobbleUtils.LOGGER.info("[HiperMessage] Unknown message type in rawMessage: " + rawMessage + ". Defaulting " +
            "to CHAT." +
            " Valid types are: " + MessageType.defaults());
        }
      }

      String finalContent = placeholders != null && !placeholders.isEmpty()
        ? replacePlaceholders(content, placeholders)
        : content;

      switch (type) {
        case CHAT -> sendChat(playerUUID, finalContent, prefix, cache, receivedFromRedis);
        case CHAT_BROADCAST, BROADCAST -> sendBroadcast(finalContent, prefix, cache, receivedFromRedis);
        case ACTIONBAR -> sendActionBar(playerUUID, finalContent, prefix, cache, receivedFromRedis);
        case ACTIONBAR_BROADCAST -> sendActionBarBroadcast(finalContent, prefix, cache, receivedFromRedis);
        case BOSSBAR -> sendBossBar(playerUUID, finalContent, prefix, cache, receivedFromRedis);
        case BOSSBAR_BROADCAST -> sendBossBarBroadcast(finalContent, prefix, cache, receivedFromRedis);
        case TITLE_SUBTITLE -> sendTitleSubtitle(playerUUID, finalContent, prefix, cache, receivedFromRedis);
        case TITLE_SUBTITLE_BROADCAST -> sendTitleSubtitleBroadcast(finalContent, prefix, cache, receivedFromRedis);
        default -> CobbleUtils.LOGGER.warn("Unknown message type: " + type);
      }
      this.modifiedContent = null;
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
    if (playerUUID == null) return null;
    return CobbleUtils.server.getPlayerManager().getPlayer(playerUUID);
  }

  //==========================================================================//
  //                                   CHAT
  //==========================================================================//

  /**
   * Send a chat message to a player.
   *
   * @param playerUUID        the UUID of the player to send the message to
   * @param content           the content of the message
   * @param prefix
   * @param cache             whether to cache the message or not
   * @param receivedFromRedis whether the message was received from Redis or not
   */
  private void sendChat(UUID playerUUID, String content, String prefix, boolean cache, boolean receivedFromRedis) {
    ServerPlayerEntity player = getPlayer(playerUUID);
    if (player == null && !receivedFromRedis && CobbleUtils.config.isRedisMessaging()) {
      sendToRedis(MessageType.CHAT.name(), this.modifiedContent, prefix, playerUUID, null);
      return;
    }
    if (player == null) return;
    player.sendMessage(AdventureTranslator.toNative(playThings(player, content), prefix, player), false);
  }

  /**
   * Broadcast a chat message to all players.
   *
   * @param content           the content of the message
   * @param prefix
   * @param cache             whether to cache the message or not
   * @param receivedFromRedis whether the message was received from Redis or not
   */
  private void sendBroadcast(String content, String prefix, boolean cache, boolean receivedFromRedis) {
    // Si no fue recibido de Redis y Redis está habilitado, enviar por Redis
    if (!receivedFromRedis && CobbleUtils.config.isRedisMessaging()) {
      sendToRedis("BROADCAST", this.modifiedContent, prefix, null, null);
      return;
    }

    // Enviar localmente
    var players = CobbleUtils.server.getPlayerManager().getPlayerList();
    String s = playThings(null, content);
    for (ServerPlayerEntity player : players) {
      player.sendMessage(AdventureTranslator.toNative(s, prefix, player), false);
    }
  }

  //==========================================================================//
  //                                 ACTION BAR
  //==========================================================================//

  /**
   * Send an action bar message to a player.
   *
   * @param playerUUID        the UUID of the playerUUID to send the message to
   * @param content           the content of the message
   * @param prefix
   * @param cache             whether to cache the message or not
   * @param receivedFromRedis whether the message was received from Redis or not
   */
  private void sendActionBar(UUID playerUUID, String content, String prefix, boolean cache, boolean receivedFromRedis) {
    ServerPlayerEntity player = getPlayer(playerUUID);
    if (player == null && !receivedFromRedis && CobbleUtils.config.isRedisMessaging()) {
      sendToRedis(MessageType.ACTIONBAR.name(), this.modifiedContent, prefix, playerUUID, null);
      return;
    }
    if (player == null) return;
    player.sendMessage(AdventureTranslator.toNative(playThings(player, content), prefix, player), true);
  }

  /**
   * Broadcast an action bar message to all players.
   *
   * @param content           the content of the message
   * @param prefix
   * @param cache             whether to cache the message or not
   * @param receivedFromRedis whether the message was received from Redis or not
   */
  private void sendActionBarBroadcast(String content, String prefix, boolean cache, boolean receivedFromRedis) {
    // Si no fue recibido de Redis y Redis está habilitado, enviar por Redis
    if (!receivedFromRedis && CobbleUtils.config.isRedisMessaging()) {
      sendToRedis("ACTIONBAR_BROADCAST", this.modifiedContent, prefix, null, null);
      return;
    }

    // Enviar localmente
    var players = CobbleUtils.server.getPlayerManager().getPlayerList();
    for (ServerPlayerEntity player : players) {
      player.sendMessage(AdventureTranslator.toNative(playThings(player, content), prefix, player), true);
    }
  }

  //==========================================================================//
  //                                   BOSS BAR
  //==========================================================================//

  /**
   * Send a boss bar message to a player.
   *
   * @param player            UUID
   * @param content           the content of the message
   * @param prefix
   * @param cache             whether to cache the message or not
   * @param receivedFromRedis whether the message was received from Redis or not
   */
  private void sendBossBar(UUID player, String content, String prefix, boolean cache, boolean receivedFromRedis) {
    CobbleUtils.LOGGER.info("Boss bar message not implemented yet.");
  }

  /**
   * Broadcast a boss bar message to all players.
   *
   * @param content           the content of the message
   * @param prefix
   * @param cache             whether to cache the message or not
   * @param receivedFromRedis whether the message was received from Redis or not
   */
  private void sendBossBarBroadcast(String content, String prefix, boolean cache, boolean receivedFromRedis) {
    // Si no fue recibido de Redis y Redis está habilitado, enviar por Redis
    if (!receivedFromRedis && CobbleUtils.config.isRedisMessaging()) {
      sendToRedis("BOSSBAR_BROADCAST", this.modifiedContent, prefix, null, null);
      return;
    }

    CobbleUtils.LOGGER.info("Boss bar broadcast not implemented yet.");
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
    public TitleS2CPacket getTitlePacker(ServerPlayerEntity player, String prefix) {

      return titlePacketCache.get(title, t -> new TitleS2CPacket(AdventureTranslator.toNative(t, prefix, player)));
    }

    /**
     * Gets the SubtitleS2CPacket for the subtitle, using the cache if available.
     *
     * @return the SubtitleS2CPacket instance
     */
    public SubtitleS2CPacket getSubtitlePacker(ServerPlayerEntity player, String prefix) {
      return subtitlePacketCache.get(subtitle, s -> new SubtitleS2CPacket(AdventureTranslator.toNative(s, prefix,
        player)));
    }

    /**
     * Sends the title and subtitle to the specified player.
     *
     * @param player the player to send the title and subtitle to
     */
    public void sendTo(ServerPlayerEntity player, String prefix) {
      if (player == null) return;
      if (title != null && !title.isEmpty()) {
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(0, 40, 20));
        player.networkHandler.sendPacket(getTitlePacker(player, prefix));
      }
      if (subtitle != null && !subtitle.isEmpty()) {
        player.networkHandler.sendPacket(getSubtitlePacker(player, prefix));
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
   * @param playerUUID        UUID
   * @param content           the content of the message
   * @param prefix            Prefix
   * @param cache             whether to cache the message or not
   * @param receivedFromRedis whether the message was received from Redis or not
   */
  private void sendTitleSubtitle(UUID playerUUID, String content, String prefix, boolean cache, boolean receivedFromRedis) {
    ServerPlayerEntity player = getPlayer(playerUUID);
    if (player == null && !receivedFromRedis && CobbleUtils.config.isRedisMessaging()) {
      sendToRedis(MessageType.TITLE_SUBTITLE.name(), this.modifiedContent, prefix, playerUUID, null);
      return;
    }

    TitleSubtitle titleSubtitle = parseTitleSubtitle(playThings(player, content));
    titleSubtitle.sendTo(player, prefix);
  }

  /**
   * Broadcast a title and subtitle message to all players.
   *
   * @param content           the content of the message
   * @param prefix
   * @param cache             whether to cache the message or not
   * @param receivedFromRedis whether the message was received from Redis or not
   */
  private void sendTitleSubtitleBroadcast(String content, String prefix, boolean cache, boolean receivedFromRedis) {
    // Si no fue recibido de Redis y Redis está habilitado, enviar por Redis
    if (!receivedFromRedis && CobbleUtils.config.isRedisMessaging()) {
      sendToRedis("TITLE_SUBTITLE_BROADCAST", this.modifiedContent, prefix, null, null);
      return;
    }

    // Enviar localmente
    TitleSubtitle titleSubtitle = parseTitleSubtitle(playThings(null, content));
    var players = CobbleUtils.server.getPlayerManager().getPlayerList();
    for (ServerPlayerEntity player : players) {
      titleSubtitle.sendTo(player, prefix);
    }
  }

  //==========================================================================//
  //                               SOUND
  //==========================================================================//

  private static final Cache<String, SoundEvent> soundCache = Caffeine.newBuilder()
    .maximumSize(1_000)
    .expireAfterAccess(10, TimeUnit.MINUTES)
    .build();

  private static final Pattern EFFECT_PATTERN = Pattern.compile(
    "(?:" +
      "sound:(?<sound>\\S+)" +
      "(?:\\s*volume:(?<volume>\\d+(?:\\.\\d+)?))?" +
      "(?:\\s*pitch:(?<pitch>\\d+(?:\\.\\d+)?))?" +
      "|" +
      "potion:(?<potion>\\S+)" +
      "(?:\\s*duration:(?<duration>\\d+))?" +
      "(?:\\s*amplifier:(?<amplifier>\\d+))?" +
      "|" +
      "particle:(?<particle>\\S+)" +
      "(?:\\s*count:(?<count>\\d+))?" +
      ")",
    Pattern.CASE_INSENSITIVE
  );

  public String playThings(ServerPlayerEntity player, String content) {
    if (content == null || content.isEmpty()) return content;

    Matcher matcher = EFFECT_PATTERN.matcher(content);
    StringBuilder cleaned = new StringBuilder();

    while (matcher.find()) {
      try {
        // ---------------- SONIDOS ----------------
        if (matcher.group("sound") != null) {
          SoundEvent soundEvent;
          String soundName = matcher.group("sound");
          Identifier id = Identifier.tryParse(soundName);

          // Skip if the identifier is invalid
          if (!soundName.contains("cobblemon:")) {
            if (id == null) {
              CobbleUtils.LOGGER.warn("Invalid sound identifier: " + soundName);
              matcher.appendReplacement(cleaned, "");
              continue;
            }

            // Check if the sound actually exists in the registry
            if (!Registries.SOUND_EVENT.containsId(id)) {
              CobbleUtils.LOGGER.warn("Sound not found in registry:" + id);
              matcher.appendReplacement(cleaned, "");
              continue;
            }

            soundEvent = Registries.SOUND_EVENT.get(id);
            if (soundEvent == null) {
              CobbleUtils.LOGGER.warn("Null SoundEvent for identifier: " + id);
              matcher.appendReplacement(cleaned, "");
              continue;
            }
          } else {
            soundEvent = SoundEvent.of(id);
          }

          // Parse volume and pitch (with default values)
          float volume = parseOrDefault(matcher.group("volume"), 1.0f);
          float pitch = parseOrDefault(matcher.group("pitch"), 1.0f);

          // Clamp values to prevent out-of-range behavior
          float finalVolume = Math.clamp(volume, 0.0f, 4.0f);
          float finalPitch = Math.clamp(pitch, 0.5f, 2.0f);

          // Play sound to all players or a specific player
          if (player == null) {
            for (ServerPlayerEntity p : CobbleUtils.server.getPlayerManager().getPlayerList()) {
              if (p == null) continue;
              CobbleUtils.server.execute(() ->
                p.playSoundToPlayer(soundEvent, SoundCategory.PLAYERS, finalVolume, finalPitch)
              );
            }
          } else {
            CobbleUtils.server.execute(() ->
              player.playSoundToPlayer(soundEvent, SoundCategory.PLAYERS, finalVolume, finalPitch)
            );
          }
        }


        // ---------------- POCIONES ----------------
        else if (matcher.group("potion") != null) {
          StatusEffect effect = Registries.STATUS_EFFECT.get(Identifier.of(matcher.group("potion")));
          if (effect != null) {
            int duration = (int) parseOrDefault(matcher.group("duration"), 200f);
            int amplifier = (int) parseOrDefault(matcher.group("amplifier"), 0f);
            CobbleUtils.server.execute(() -> {
              var status = new StatusEffectInstance(RegistryEntry.of(effect), duration, amplifier);
              if (player == null) {
                for (ServerPlayerEntity p : CobbleUtils.server.getPlayerManager().getPlayerList()) {
                  if (p == null) continue;
                  p.addStatusEffect(status);
                }
                return;
              }
              player.addStatusEffect(status);
            });
          }
        }

        // ---------------- PARTÍCULAS ----------------
        else if (matcher.group("particle") != null) {
          ParticleEffect particle = (ParticleEffect) Registries.PARTICLE_TYPE
            .get(Identifier.tryParse(matcher.group("particle")));

          if (particle != null) {
            int count = (int) parseOrDefault(matcher.group("count"), 20f);
            CobbleUtils.server.execute(() -> {
              ParticleS2CPacket packet;
              if (player == null) {
                for (ServerPlayerEntity p : CobbleUtils.server.getPlayerManager().getPlayerList()) {
                  if (p == null) continue;
                  packet = new ParticleS2CPacket(particle, true,
                    (float) p.getX(), (float) p.getY() + 1, (float) p.getZ(),
                    0.5f, 0.5f, 0.5f, 0.1f, count);
                  p.networkHandler.sendPacket(packet);
                }
                return;
              }
              packet = new ParticleS2CPacket(particle, true,
                (float) player.getX(), (float) player.getY() + 1, (float) player.getZ(),
                0.5f, 0.5f, 0.5f, 0.1f, count);
              player.networkHandler.sendPacket(packet);
            });
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }

      matcher.appendReplacement(cleaned, "");
    }

    matcher.appendTail(cleaned);
    return cleaned.toString().trim();
  }

  private float parseOrDefault(String input, float def) {
    if (input == null) return def;
    try {
      return Float.parseFloat(input);
    } catch (NumberFormatException e) {
      return def;
    }
  }


  /**
   * Sends a HiperMessage through Redis for cross-server broadcasting.
   *
   * @param messageType  the type of message (BROADCAST, ACTIONBAR_BROADCAST, etc.)
   * @param content      the message content
   * @param prefix       the message prefix
   * @param playerUUID   the player UUID (null for broadcasts)
   * @param placeholders the placeholders map
   */
  private void sendToRedis(String messageType, String content, String prefix, UUID playerUUID, Map<String, String> placeholders) {
    try {
      CompletableFuture.runAsync(() -> {
          JsonObject redisMessage = new JsonObject();
          redisMessage.addProperty("type", "hipermessage");
          redisMessage.addProperty("messageType", messageType);
          redisMessage.addProperty("rawMessage", content);
          redisMessage.addProperty("content", content);

          if (prefix != null && !prefix.isEmpty()) {
            redisMessage.addProperty("prefix", prefix);
          }


          if (playerUUID != null) {
            redisMessage.addProperty("playerUUID", playerUUID.toString());
          }

          if (placeholders != null && !placeholders.isEmpty()) {
            JsonObject placeholdersJson = new JsonObject();
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
              placeholdersJson.addProperty(entry.getKey(), entry.getValue());
            }
            redisMessage.add("placeholders", placeholdersJson);
          }
          try {
            RedisManager.publish(CobbleUtils.config.getRedis().getChannel(), redisMessage.toString());
          } catch (Exception e) {
            CobbleUtils.LOGGER.error("Failed to send HiperMessage through Redis: " + e.getMessage());
          }
        }, RedisManager.EXECUTOR_REDIS)
        .exceptionally(e -> {
          e.printStackTrace();
          return null;
        });

    } catch (Exception e) {
      CobbleUtils.LOGGER.error("Error creating Redis message for HiperMessage: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Handles incoming Redis messages for HiperMessage.
   *
   * @param redisMessage the JSON message received from Redis
   */
  public static void handleRedisMessage(JsonObject redisMessage) {
    try {
      // Safely extract required fields
      String messageType = getAsString(redisMessage, "messageType");
      String rawMessage = getAsString(redisMessage, "rawMessage");
      String content = getAsString(redisMessage, "content");
      String prefix = redisMessage.has("prefix") ? redisMessage.get("prefix").getAsString() : "";

      // Process player UUID (if present and valid)
      UUID playerUUID = null;
      if (redisMessage.has("playerUUID")) {
        try {
          playerUUID = UUID.fromString(redisMessage.get("playerUUID").getAsString());
        } catch (IllegalArgumentException ex) {
          CobbleUtils.LOGGER.warn("Invalid playerUUID in Redis HiperMessage: " + redisMessage.get("playerUUID").getAsString());
        }
      }

      // Process placeholders (if present)
      Map<String, String> placeholders = null;
      if (redisMessage.has("placeholders")) {
        placeholders = new HashMap<>();
        JsonObject placeholdersJson = redisMessage.getAsJsonObject("placeholders");
        for (Map.Entry<String, JsonElement> entry : placeholdersJson.entrySet()) {
          placeholders.put(entry.getKey(), entry.getValue().getAsString());
        }
      }

      // Create and send the HiperMessage
      HiperMessage hiperMessage = new HiperMessage(rawMessage, null);
      hiperMessage.sendMessage(playerUUID, prefix, false, true, placeholders, content);

    } catch (Exception e) {
      CobbleUtils.LOGGER.error("Error handling Redis HiperMessage");
    }
  }

  /**
   * Helper method to safely extract strings from a JsonObject
   * without risking NullPointerExceptions.
   */
  private static String getAsString(JsonObject json, String key) {
    return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
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
