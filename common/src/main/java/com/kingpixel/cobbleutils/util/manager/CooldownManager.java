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

  // Clave compuesta: jugador + menú
  private record MenuKey(UUID playerId, String menu) {
  }

  // Valor: tiempo de expiración (timestamp en ms)
  private record CooldownEntry(long expiryTime) {
  }

  // Cache con expiración dinámica por entrada
  private static final Cache<MenuKey, CooldownEntry> cooldownMenus = Caffeine.newBuilder()
    .expireAfter(new Expiry<MenuKey, CooldownEntry>() {
      @Override
      public long expireAfterCreate(@NotNull MenuKey key, @NotNull CooldownEntry value, long currentTime) {
        return value.expiryTime() - System.currentTimeMillis();
      }

      @Override
      public long expireAfterUpdate(@NotNull MenuKey key, @NotNull CooldownEntry value,
                                    long currentTime, long currentDuration) {
        return value.expiryTime() - System.currentTimeMillis();
      }

      @Override
      public long expireAfterRead(@NotNull MenuKey key, @NotNull CooldownEntry value,
                                  long currentTime, long currentDuration) {
        return currentDuration;
      }
    })
    .removalListener((MenuKey key, CooldownEntry value, RemovalCause cause) -> {
      if (cause == RemovalCause.EXPIRED && CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER.info("[CobbleUtils] ⏰ Cooldown expirado: " +
          key.playerId() + " | menú=" + key.menu());
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
  public static boolean isCooldownMenu(ServerPlayerEntity player, String menu, DurationValue durationValue) {
    if (player == null) return false;
    if (durationValue == null || durationValue.toMillis() <= 0) return false;

    MenuKey key = new MenuKey(player.getUuid(), menu);
    long currentTime = System.currentTimeMillis();
    CooldownEntry entry = cooldownMenus.getIfPresent(key);

    if (entry == null || currentTime >= entry.expiryTime()) {
      cooldownMenus.put(key, new CooldownEntry(currentTime + durationValue.toMillis()));
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
}

