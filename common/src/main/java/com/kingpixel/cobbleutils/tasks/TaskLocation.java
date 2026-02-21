package com.kingpixel.cobbleutils.tasks;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.Location;
import com.kingpixel.cobbleutils.util.RedisManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Carlos Varas Alonso - 30/12/2025 4:28
 */
public class TaskLocation {
  public static void register() {
    CobbleUtils.ASYNC.scheduleAtFixedRate(() -> {
      if (CobbleUtils.server == null) return;

      var entries = RedisManager.LOCATION_CACHE.asMap().entrySet();
      for (var entry : entries) {
        UUID playerUUID = entry.getKey();
        Location location = entry.getValue();
        CobbleUtils.server.execute(() -> {
          ServerPlayerEntity player = CobbleUtils.server.getPlayerManager().getPlayer(playerUUID);
          if (player == null) return;
          if (location.teleportToNoCrossServer(player)) {
            RedisManager.LOCATION_CACHE.invalidate(playerUUID);
          }
        });
      }
    }, 0, 1, TimeUnit.SECONDS);
  }
}
