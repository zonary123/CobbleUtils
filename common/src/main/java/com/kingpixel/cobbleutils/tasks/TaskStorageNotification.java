package com.kingpixel.cobbleutils.tasks;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.database.DataBaseFactory;
import com.kingpixel.cobbleutils.database.users.DataBaseUsers;
import com.kingpixel.cobbleutils.database.users.UserModel;
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
    CobbleUtils.ASYNC.scheduleAtFixedRate(() -> {
      try {
        if (CobbleUtils.server == null) return;
        var users = DataBaseUsers.USERS.asMap().values();
        for (UserModel user : users) {
          DataBaseFactory.dataBaseUsers.findUserStorage(user.getPlayerUUID())
            .whenComplete((storages, throwable) -> {
              if (storages == null) return;
              int size = storages.size();
              if (size > 0) {
                CobbleUtils.server.execute(() -> {
                  ServerPlayerEntity player = CobbleUtils.server.getPlayerManager().getPlayer(user.getPlayerUUID());
                  if (player == null) return;
                  PlayerUtils.sendMessage(
                    player,
                    CobbleUtils.language.getMessageStorageNotify()
                      .replace("%amount%", String.valueOf(size)),
                    CobbleUtils.config.getPrefix(),
                    TypeMessage.CHAT
                  );
                });
              }
            });
        }
      } catch (Exception ignored) {
      }
    }, 0, 1, TimeUnit.MINUTES);
  }
}
