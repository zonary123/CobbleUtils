package com.kingpixel.cobbleutils.command.suggests;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.database.DataBaseFactory;
import com.kingpixel.cobbleutils.database.users.UserModel;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import lombok.Data;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * @author Carlos Varas Alonso
 */
@Data
public class PlayerOfflineAndOnline {

  private long lastUpdate = 0;
  private final long updateInterval = TimeUnit.MINUTES.toMillis(5);
  private final List<String> playerNames = new CopyOnWriteArrayList<>();
  private final List<UUID> playerUUIDs = new CopyOnWriteArrayList<>();
  private CompletableFuture<Void> currentLoad = CompletableFuture.completedFuture(null);

  /**
   * Obtiene los nombres cacheados de los jugadores.
   */
  private List<String> getPlayerNames() {
    refreshIfNeeded();
    return playerNames;
  }

  /**
   * Obtiene los UUID cacheados de los jugadores.
   */
  private List<UUID> getPlayerUUIDs() {
    refreshIfNeeded();
    return playerUUIDs;
  }

  /**
   * Verifica si hace falta refrescar la lista de jugadores.
   */
  private void refreshIfNeeded() {
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastUpdate > updateInterval) {
      lastUpdate = currentTime;
      loadAsync();
    }
  }

  /**
   * Carga los jugadores inactivos de forma asíncrona.
   */
  private void loadAsync() {
    currentLoad = CompletableFuture.runAsync(() -> {
      try {
        var list = DataBaseFactory.dataBaseUsers.getUsersInactiveSince(
          CobbleUtils.config.getTimeSinceLastLoginToSuggest().toMillis()
        );

        List<String> names = new ArrayList<>();
        List<UUID> uuids = new ArrayList<>();

        for (UserModel user : list) {
          names.add(user.getPlayerName());
          uuids.add(user.getPlayerUUID());
        }

        playerNames.clear();
        playerNames.addAll(names);

        playerUUIDs.clear();
        playerUUIDs.addAll(uuids);

      } catch (Exception ex) {
        CobbleUtils.LOGGER.error("Error fetching player data: ");
      }
    }, CobbleUtils.EXECUTOR_COBBLEUTILS);
  }

  /**
   * Sugiere nombres de jugadores.
   */
  public RequiredArgumentBuilder<ServerCommandSource, String> suggestPlayerName(
    String literal, List<String> permissions, int level) {
    return CommandManager.argument(literal, StringArgumentType.string())
      .requires(source -> PermissionApi.hasPermission(source, permissions, level))
      .suggests((context, builder) -> CommandSource.suggestMatching(getPlayerNames(), builder));
  }

  /**
   * Sugiere UUIDs de jugadores.
   */
  public RequiredArgumentBuilder<ServerCommandSource, UUID> suggestPlayerUUID(
    String literal, List<String> permissions, int level) {
    return CommandManager.argument(literal, UuidArgumentType.uuid())
      .requires(source -> PermissionApi.hasPermission(source, permissions, level))
      .suggests((context, builder) -> CommandSource.suggestMatching(
        getPlayerUUIDs().stream().map(UUID::toString).toList(),
        builder
      ));
  }

  /**
   * Resetea la caché.
   */
  public void reset() {
    lastUpdate = 0;
    playerNames.clear();
    playerUUIDs.clear();
  }

  /**
   * Obtiene un jugador (online u offline) a partir de un nombre desde el contexto.
   */
  public Optional<UserModel> getUserFromContext(CommandContext<ServerCommandSource> context, String argumentName) {
    String playerName = StringArgumentType.getString(context, argumentName);
    return getUserFromName(playerName);
  }

  /**
   * Obtiene un jugador offline desde su nombre (de caché o DB).
   */
  public Optional<UserModel> getUserFromName(String playerName) {
    // Primero buscamos en la caché
    int index = playerNames.indexOf(playerName);
    if (index >= 0 && index < playerUUIDs.size()) {
      return Optional.of(new UserModel(playerUUIDs.get(index), playerName));
    }

    // Si no está en caché, lo pedimos directamente a la DB
    try {
      UserModel user = DataBaseFactory.dataBaseUsers.findUserByName(playerName);
      return Optional.ofNullable(user);
    } catch (Exception ex) {
      CobbleUtils.LOGGER.error("Error fetching user by name: " + playerName);

      return Optional.empty();
    }
  }

  public Optional<DataResultPlayer> getPlayerFromContext(CommandContext<ServerCommandSource> context, String argumentName) {
    String playerName = StringArgumentType.getString(context, argumentName);
    return getPlayer(playerName);
  }

  /**
   * Obtiene un jugador online directamente desde un String.
   */
  public Optional<DataResultPlayer> getPlayer(String playerName) {
    var online = CobbleUtils.server.getPlayerManager().getPlayer(playerName);
    var userModel = DataBaseFactory.dataBaseUsers.findUserByName(playerName);
    var offlineOrOnlineUser = DataBaseFactory.dataBaseUsers.getPlayerOfflineOrOnline(playerName);
    return Optional.ofNullable(offlineOrOnlineUser == null ? null : new DataResultPlayer(
        online != null,
        userModel,
        offlineOrOnlineUser
      )
    );
  }


  public record DataResultPlayer(boolean isOnline, UserModel user, ServerPlayerEntity player) {
  }
}
