package com.kingpixel.cobbleutils.tasks;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.Location;
import com.kingpixel.cobbleutils.util.RedisManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TaskLocation {

  public static void register() {

    CobbleUtils.ASYNC.scheduleAtFixedRate(() -> {
      try {
        if (CobbleUtils.server == null) return;

        List<UUID> toProcess = new ArrayList<>(RedisManager.LOCATION_CACHE.asMap().keySet());

        if (toProcess.isEmpty()) return;

        CobbleUtils.server.execute(() -> {
          try {
            for (UUID playerUUID : toProcess) {

              Location location = RedisManager.LOCATION_CACHE.getIfPresent(playerUUID);
              if (location == null) continue;

              ServerPlayerEntity player =
                CobbleUtils.server.getPlayerManager().getPlayer(playerUUID);

              if (player == null) continue;

              if (location.teleportToNoCrossServer(player)) {
                RedisManager.LOCATION_CACHE.invalidate(playerUUID);
              }
            }
          } catch (Exception e) {
            CobbleUtils.LOGGER.error("Error processing location task");
            e.printStackTrace();
          }
        });
      } catch (Exception e) {
        CobbleUtils.LOGGER.error("Error processing location task");
        e.printStackTrace();
      }
    }, 0, 1, TimeUnit.SECONDS);
  }
}