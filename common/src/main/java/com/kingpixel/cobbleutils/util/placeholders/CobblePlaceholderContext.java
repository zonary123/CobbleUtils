package com.kingpixel.cobbleutils.util.placeholders;

import com.kingpixel.cobbleutils.CobbleUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Unified mutual context for placeholder evaluations across PlaceholderAPI, MiniPlaceholders,
 * and internal replacements.
 */
@Getter
@ToString
@Builder(toBuilder = true)
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

  CobblePlaceholderContext(
    @Nullable ServerPlayerEntity player,
    @Nullable Audience audience,
    @Nullable ServerPlayerEntity targetPlayer,
    @Nullable Audience targetAudience,
    @Nullable MinecraftServer server,
    @Nullable ServerWorld world,
    @Nullable Object target,
    @Nullable String argument,
    @Nullable String[] args,
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
    this.args = args != null ? args : parseArgs(argument);
    this.argumentQueue = argumentQueue;
    this.miniMessageContext = miniMessageContext;
  }

  /**
   * Creates a derived copy of this context with a new argument string.
   */
  public CobblePlaceholderContext withArgument(@Nullable String newArgument) {
    return toBuilder()
      .argument(newArgument)
      .args(parseArgs(newArgument))
      .build();
  }

  private static final Pattern SPLIT_PATTERN = Pattern.compile("[_\\s]+");
  private static final String[] EMPTY_ARGS = new String[0];

  private static @Nullable MinecraftServer getSafeServer() {
    try {
      return CobbleUtils.server;
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static String[] parseArgs(@Nullable String argument) {
    if (argument == null || argument.trim().isEmpty()) {
      return EMPTY_ARGS;
    }
    return SPLIT_PATTERN.split(argument);
  }

  // ==========================================
  // Factory Methods
  // ==========================================

  public static CobblePlaceholderContext of(@Nullable ServerPlayerEntity player) {
    return builder().player(player).build();
  }

  public static CobblePlaceholderContext of(@Nullable ServerPlayerEntity player, @Nullable String argument) {
    return builder().player(player).argument(argument).build();
  }

  public static CobblePlaceholderContext of(@Nullable ServerPlayerEntity player, @Nullable Object target, @Nullable String argument) {
    return builder().player(player).target(target).argument(argument).build();
  }

  public static CobblePlaceholderContext ofRelational(
    @Nullable ServerPlayerEntity player,
    @Nullable ServerPlayerEntity targetPlayer,
    @Nullable String argument
  ) {
    return builder().player(player).targetPlayer(targetPlayer).argument(argument).build();
  }

  public static CobblePlaceholderContext global(@Nullable String argument) {
    return builder().argument(argument).build();
  }

  public static CobblePlaceholderContext ofMiniPlaceholders(
    @Nullable Audience audience,
    @Nullable ArgumentQueue queue,
    @Nullable Context ctx
  ) {
    ServerPlayerEntity resolvedPlayer = resolvePlayerFromAudience(audience);
    String extractedArg = extractArgFromQueue(queue);
    return builder()
      .player(resolvedPlayer)
      .audience(audience)
      .argument(extractedArg)
      .argumentQueue(queue)
      .miniMessageContext(ctx)
      .build();
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
    return builder()
      .player(player1)
      .audience(audience)
      .targetPlayer(player2)
      .targetAudience(otherAudience)
      .argument(extractedArg)
      .argumentQueue(queue)
      .miniMessageContext(ctx)
      .build();
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

  /**
   * Safely resolves a Minecraft ServerPlayerEntity from an Adventure Audience,
   * fully supporting direct instances, identity inspection, and proxy/forwarding wrappers.
   */
  public static @Nullable ServerPlayerEntity resolvePlayerFromAudience(@Nullable Audience audience) {
    if (audience == null) return null;
    try {
      ServerPlayerEntity direct = resolveDirectPlayer(audience);
      if (direct != null) return direct;

      ServerPlayerEntity forwarded = resolveForwardingPlayer(audience);
      if (forwarded != null) return forwarded;

      return resolvePlayerFromIdentity(audience);
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static @Nullable ServerPlayerEntity resolveDirectPlayer(@NotNull Audience audience) {
    if (audience instanceof ServerPlayerEntity p) {
      return p;
    }
    return null;
  }

  private static @Nullable ServerPlayerEntity resolveForwardingPlayer(@NotNull Audience audience) {
    if (audience instanceof ForwardingAudience.Single single) {
      return resolvePlayerFromAudience(single.audience());
    }
    if (audience instanceof ForwardingAudience forwarding) {
      for (Audience child : forwarding.audiences()) {
        ServerPlayerEntity unwrapped = resolvePlayerFromAudience(child);
        if (unwrapped != null) return unwrapped;
      }
    }
    return null;
  }

  private static @Nullable ServerPlayerEntity resolvePlayerFromIdentity(@NotNull Audience audience) {
    MinecraftServer server = getSafeServer();
    if (server == null || server.getPlayerManager() == null) {
      return null;
    }
    Optional<UUID> uuidOpt = audience.get(Identity.UUID);
    if (uuidOpt.isPresent()) {
      ServerPlayerEntity p = server.getPlayerManager().getPlayer(uuidOpt.get());
      if (p != null) return p;
    }
    Optional<String> nameOpt = audience.get(Identity.NAME);
    return nameOpt.map(s -> server.getPlayerManager().getPlayer(s)).orElse(null);
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
