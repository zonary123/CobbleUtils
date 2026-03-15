package com.kingpixel.cobbleutils.tasks;

import com.kingpixel.cobbleutils.CobbleUtils;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author Carlos Varas Alonso - 30/12/2025 4:28
 */
public class TaskServers {
  public static void register() {
    CobbleUtils.SCHEDULER_COBBLEUTILS.schedule(() -> {
      try {
        var list = CobbleUtils.server.getPlayerManager().getPlayerList();
        if (list.isEmpty()) return;
        ServerPlayerEntity player = list.getFirst();
        if (player == null) return;
        ServerPlayNetworking.send(player, new ProxyPacket("GetServers"));
      } catch (Exception ignored) {

      }
    }, 1, TimeUnit.MINUTES); // cada 5 minutos (ticks)

  }
}
