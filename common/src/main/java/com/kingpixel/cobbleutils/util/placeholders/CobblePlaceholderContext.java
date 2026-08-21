package com.kingpixel.cobbleutils.util.placeholders;

import com.kingpixel.cobbleutils.CobbleUtils;
import lombok.Getter;
import lombok.ToString;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Unified mutual context for placeholder evaluations across PlaceholderAPI, MiniPlaceholders,
 * and internal replacements.
 */
@Getter
@ToString
public class CobblePlaceholderContext {
  private final @Nullable ServerPlayerEntity player;
  private final @Nullable Audience audience;
  private final @Nullable ServerPlayerEntity targetPlayer;
  private final @Nullable Audience targetAudience;
  private final @Nullable MinecraftServer server;
  private final @Nullable ServerWorld world;
  private final @Nullable Object target;
  private final @Nullable String argument;
  private final String[] args;
  private final @Nullable ArgumentQueue argumentQueue;
  private final @Nullable Context miniMessageContext;

  public CobblePlaceholderContext(
    @Nullable ServerPlayerEntity player,
    @Nullable Audience audience,
    @Nullable ServerPlayerEntity targetPlayer,
    @Nullable Audience targetAudience,
    @Nullable MinecraftServer server,
    @Nullable ServerWorld world,
    @Nullable Object target,
    @Nullable String argument,
    @Nullable ArgumentQueue argumentQueue,
    @Nullable Context miniMessageContext
  ) {
    this.player = player;
    this.audience = audience;
    this.targetPlayer = targetPlayer;
    this.targetAudience = targetAudience;
    this.server = server != null ? server : (player != null ? player.getServer() : getSafeServer());
    this.world = world != null ? world : (player != null ? player.getServerWorld() : null);
    this.target = target;
    this.argument = argument;
    this.args = parseArgs(argument);
    this.argumentQueue = argumentQueue;
    this.miniMessageContext = miniMessageContext;
  }

  private static @Nullable MinecraftServer getSafeServer() {
    try {
      return CobbleUtils.server;
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static String[] parseArgs(@Nullable String argument) {
    if (argument == null || argument.trim().isEmpty()) {
      return new String[0];
    }
    return argument.split("[_\\s]+");
  }

  // ==========================================
  // Factory Methods
  // ==========================================

  public static CobblePlaceholderContext of(@Nullable ServerPlayerEntity player) {
    return new CobblePlaceholderContext(player, null, null, null, null, null, null, null, null, null);
  }

  public static CobblePlaceholderContext of(@Nullable ServerPlayerEntity player, @Nullable String argument) {
    return new CobblePlaceholderContext(player, null, null, null, null, null, null, argument, null, null);
  }

  public static CobblePlaceholderContext of(@Nullable ServerPlayerEntity player, @Nullable Object target, @Nullable String argument) {
    return new CobblePlaceholderContext(player, null, null, null, null, null, target, argument, null, null);
  }

  public static CobblePlaceholderContext ofRelational(
    @Nullable ServerPlayerEntity player,
    @Nullable ServerPlayerEntity targetPlayer,
    @Nullable String argument
  ) {
    return new CobblePlaceholderContext(player, null, targetPlayer, null, null, null, null, argument, null, null);
  }

  public static CobblePlaceholderContext global(@Nullable String argument) {
    return new CobblePlaceholderContext(null, null, null, null, null, null, null, argument, null, null);
  }

  public static CobblePlaceholderContext ofMiniPlaceholders(
    @Nullable Audience audience,
    @Nullable ArgumentQueue queue,
    @Nullable Context ctx
  ) {
    ServerPlayerEntity resolvedPlayer = resolvePlayerFromAudience(audience);
    String extractedArg = extractArgFromQueue(queue);
    return new CobblePlaceholderContext(resolvedPlayer, audience, null, null, null, null, null, extractedArg, queue, ctx);
  }

  public static CobblePlaceholderContext ofMiniPlaceholdersRelational(
    @Nullable Audience audience,
    @Nullable Audience otherAudience,
    @Nullable ArgumentQueue queue,
    @Nullable Context ctx
  ) {
    ServerPlayerEntity player1 = resolvePlayerFromAudience(audience);
    ServerPlayerEntity player2 = resolvePlayerFromAudience(otherAudience);
    String extractedArg = extractArgFromQueue(queue);
    return new CobblePlaceholderContext(player1, audience, player2, otherAudience, null, null, null, extractedArg, queue, ctx);
  }

  private static @Nullable String extractArgFromQueue(@Nullable ArgumentQueue queue) {
    if (queue == null) return null;
    try {
      List<String> raw = new ArrayList<>();
      while (queue.hasNext()) {
        raw.add(queue.pop().value());
      }
      return raw.isEmpty() ? null : String.join("_", raw);
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static @Nullable ServerPlayerEntity resolvePlayerFromAudience(@Nullable Audience audience) {
    if (audience == null) return null;
    try {
      if (audience instanceof ServerPlayerEntity p) {
        return p;
      }
      MinecraftServer s = getSafeServer();
      if (s != null && s.getPlayerManager() != null) {
        Optional<UUID> uuidOpt = audience.get(Identity.UUID);
        if (uuidOpt.isPresent()) {
          ServerPlayerEntity p = s.getPlayerManager().getPlayer(uuidOpt.get());
          if (p != null) return p;
        }
        Optional<String> nameOpt = audience.get(Identity.NAME);
        if (nameOpt.isPresent()) {
          return s.getPlayerManager().getPlayer(nameOpt.get());
        }
      }
    } catch (Throwable ignored) {
    }
    return null;
  }

  // ==========================================
  // Helper Methods
  // ==========================================

  public boolean hasPlayer() {
    return player != null;
  }

  public boolean hasTargetPlayer() {
    return targetPlayer != null;
  }

  public boolean hasTarget() {
    return target != null;
  }

  public boolean hasServer() {
    return server != null;
  }

  public boolean hasWorld() {
    return world != null;
  }

  public boolean hasArg() {
    return argument != null && !argument.trim().isEmpty();
  }

  public int numArgs() {
    return args.length;
  }

  public @Nullable UUID getPlayerUuid() {
    return player != null ? player.getUuid() : null;
  }

  public @Nullable String getPlayerName() {
    return player != null ? player.getNameForScoreboard() : null;
  }

  /**
   * Safely retrieves the target object cast to the expected type with full try-catch safety.
   */
  @SuppressWarnings("unchecked")
  public <T> Optional<T> targetAs(@NotNull Class<T> clazz) {
    if (target == null) return Optional.empty();
    try {
      if (clazz.isInstance(target)) {
        return Optional.of((T) target);
      }
    } catch (Throwable ignored) {
    }
    return Optional.empty();
  }

  public <T> T targetAs(@NotNull Class<T> clazz, T defaultValue) {
    return targetAs(clazz).orElse(defaultValue);
  }

  public String getArg(int index, String defaultValue) {
    try {
      if (index >= 0 && index < args.length) {
        return args[index];
      }
    } catch (Throwable ignored) {
    }
    return defaultValue;
  }

  public int getArgInt(int index, int defaultValue) {
    try {
      String val = getArg(index, null);
      if (val != null) {
        return Integer.parseInt(val.trim());
      }
    } catch (Throwable ignored) {
    }
    return defaultValue;
  }

  public long getArgLong(int index, long defaultValue) {
    try {
      String val = getArg(index, null);
      if (val != null) {
        return Long.parseLong(val.trim());
      }
    } catch (Throwable ignored) {
    }
    return defaultValue;
  }

  public double getArgDouble(int index, double defaultValue) {
    try {
      String val = getArg(index, null);
      if (val != null) {
        return Double.parseDouble(val.trim());
      }
    } catch (Throwable ignored) {
    }
    return defaultValue;
  }

  public boolean getArgBool(int index, boolean defaultValue) {
    try {
      String val = getArg(index, null);
      if (val != null) {
        String trimmed = val.trim().toLowerCase();
        if (trimmed.equals("true") || trimmed.equals("yes") || trimmed.equals("1") || trimmed.equals("si")) {
          return true;
        }
        if (trimmed.equals("false") || trimmed.equals("no") || trimmed.equals("0")) {
          return false;
        }
      }
    } catch (Throwable ignored) {
    }
    return defaultValue;
  }
}
