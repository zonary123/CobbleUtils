package com.kingpixel.cobbleutils.util.manager;

/**
 * @author Carlos Varas Alonso - 03/09/2025 17:30
 */

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DurationValue;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Manejo de cooldowns para menús usando Caffeine con expiración personalizada.
 */
public class CooldownManager {

  // Clave compuesta: playerId + value
  private record Key(UUID playerId, String value) {
  }

  // Valor: tiempo de expiración (timestamp en ms)
  private record CooldownEntry(long expiryTime) {
  }

  // Cache con expiración dinámica por entrada
  private static final Cache<Key, CooldownEntry> COOLDOWNS = Caffeine.newBuilder()
    .expireAfter(new Expiry<Key, CooldownEntry>() {
      @Override
      public long expireAfterCreate(@NotNull CooldownManager.Key key, @NotNull CooldownEntry value, long currentTime) {
        return value.expiryTime() - System.currentTimeMillis();
      }

      @Override
      public long expireAfterUpdate(@NotNull CooldownManager.Key key, @NotNull CooldownEntry value,
                                    long currentTime, long currentDuration) {
        return value.expiryTime() - System.currentTimeMillis();
      }

      @Override
      public long expireAfterRead(@NotNull CooldownManager.Key key, @NotNull CooldownEntry value,
                                  long currentTime, long currentDuration) {
        return currentDuration;
      }
    })
    .removalListener((Key key, CooldownEntry value, RemovalCause cause) -> {
      if (cause == RemovalCause.EXPIRED && CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER.info("[CobbleUtils] ⏰ Cooldown expirado: " +
          key.playerId() + " | menú=" + key.value());
      }
    })
    .build();

  /**
   * Verifica si el jugador está en cooldown para un menú.
   *
   * @param player        Jugador
   * @param menu          Identificador del menú
   * @param durationValue Duración del cooldown
   *
   * @return true si sigue en cooldown, false si puede usarlo
   */
  public static boolean hasCooldownMenu(ServerPlayerEntity player, String menu, DurationValue durationValue) {
    if (player == null) return false;
    if (durationValue == null || durationValue.toMillis() <= 0) return false;

    Key key = new Key(player.getUuid(), menu);
    long currentTime = System.currentTimeMillis();
    CooldownEntry entry = COOLDOWNS.getIfPresent(key);

    if (entry == null || currentTime >= entry.expiryTime()) {
      COOLDOWNS.put(key, new CooldownEntry(currentTime + durationValue.toMillis()));
      return false;
    }

    PlayerUtils.sendMessage(
      player,
      CobbleUtils.language.getMessageCooldownMenu(),
      null,
      TypeMessage.CHAT
    );
    return true;
  }

  public static boolean hasCooldownCommand(ServerPlayerEntity player, String command, DurationValue durationValue) {
    if (player == null) return false;
    if (durationValue == null || durationValue.toMillis() <= 0) return false;

    Key key = new Key(player.getUuid(), "cmd:" + command);
    long currentTime = System.currentTimeMillis();
    CooldownEntry entry = COOLDOWNS.getIfPresent(key);

    if (entry == null || currentTime >= entry.expiryTime()) {
      COOLDOWNS.put(key, new CooldownEntry(currentTime + durationValue.toMillis()));
      return false;
    }

    PlayerUtils.sendMessage(
      player,
      CobbleUtils.language.getMessageCooldownCommand(),
      null,
      TypeMessage.CHAT
    );
    return true;
  }
}

