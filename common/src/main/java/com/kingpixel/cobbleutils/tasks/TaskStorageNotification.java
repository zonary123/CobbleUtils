package com.kingpixel.cobbleutils.tasks;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.database.DataBaseFactory;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author Carlos Varas Alonso - 30/12/2025 4:28
 */
public class TaskStorageNotification {
  public static void register() {
    CobbleUtils.SCHEDULER_COBBLEUTILS.scheduleAtFixedRate(() -> {
      try {
        if (CobbleUtils.server == null) return;
        var players = CobbleUtils.server.getPlayerManager().getPlayerList();
        for (ServerPlayerEntity player : players) {
          if (player == null) {
            CobbleUtils.LOGGER_RAW.error("Player is null in scheduled task");
            continue;
          }
          var user = DataBaseFactory.dataBaseUsers.findUser(player.getUuid());
          if (user == null) continue;
          var storageList = user.getStorageList();
          if (storageList == null) continue;
          int size = storageList.size();
          if (size > 0) {
            PlayerUtils.sendMessage(
              player,
              CobbleUtils.language.getMessageStorageNotify()
                .replace("%amount%", String.valueOf(size)),
              CobbleUtils.config.getPrefix(),
              TypeMessage.CHAT
            );
          }
        }
      } catch (Exception ignored) {
      }
    }, 0, 1, TimeUnit.MINUTES);
  }
}
