package com.kingpixel.cobbleutils.command.suggests;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.database.DataBaseFactory;
import com.kingpixel.cobbleutils.database.users.UserModel;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import lombok.Data;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Carlos Varas Alonso - 27/08/2025 15:05
 */
@Data
public class PlayerOfflineAndOnline {
  private long lastUpdate = 0;
  private final long updateInterval = TimeUnit.MINUTES.toMillis(5);
  private List<String> playerNames = new ArrayList<>();
  private List<UUID> playerUUIDs = new ArrayList<>();

  private List<String> getPlayerNames() {
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastUpdate > updateInterval) {
      lastUpdate = currentTime;
      load();
    }
    return playerNames;
  }

  private List<UUID> getPlayerUUIDs() {
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastUpdate > updateInterval) {
      lastUpdate = currentTime;
      load();
    }
    return playerUUIDs;
  }

  private void load() {
    playerNames.clear();
    playerUUIDs.clear();
    CompletableFuture.runAsync(() -> {
        var list = DataBaseFactory.dataBaseUsers.getUsersInactiveSince(CobbleUtils.config.getTimeSinceLastLoginToSuggest());
        int size = list.size();
        for (int i = 0; i < size; i++) {
          UserModel user = list.get(i);
          playerNames.add(user.getPlayerName());
          playerUUIDs.add(user.getPlayerUUID());
        }
      }, CobbleUtils.EXECUTOR_COBBLEUTILS)
      .exceptionally(ex -> {
        CobbleUtils.LOGGER.error("Error fetching player names: " + ex.getMessage());
        return null;
      });
  }

  public RequiredArgumentBuilder<ServerCommandSource, String> suggestPlayerName(String literal, List<String> permissions, int level) {
    return CommandManager.argument(literal, StringArgumentType.string())
      .requires(source -> PermissionApi.hasPermission(source, permissions, level))
      .suggests((context, builder) -> CommandSource.suggestMatching(getPlayerNames(), builder));
  }

  public RequiredArgumentBuilder<ServerCommandSource, UUID> suggestPlayerUUID(String literal, List<String> permissions,
                                                                              int level) {
    return CommandManager.argument(literal, UuidArgumentType.uuid())
      .requires(source -> PermissionApi.hasPermission(source, permissions, level))
      .suggests((context, builder) -> CommandSource.suggestMatching(getPlayerUUIDs().stream().map(UUID::toString).toList(), builder));
  }


  public void reset() {
    lastUpdate = 0;
    playerNames.clear();
    playerUUIDs.clear();
  }
}
