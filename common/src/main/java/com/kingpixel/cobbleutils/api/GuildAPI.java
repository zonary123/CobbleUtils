package com.kingpixel.cobbleutils.api;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.guild.Guild;
import lombok.NonNull;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.UUID;

/**
 *
 * @author Carlos Varas Alonso - 26/06/2026 16:38
 */
public class GuildAPI {
  private static Guild GUILD;

  public static void registerGuildImplementation(Guild guildImpl) {
    try {
      guildImpl.getLeader(UUID.randomUUID());
      GUILD = guildImpl;
      CobbleUtils.LOGGER_RAW.info(
        "Registered guild implementation: " + guildImpl.getClass().getName()
      );
    } catch (NoClassDefFoundError | NoSuchMethodError | Exception e) {
      e.printStackTrace();
      CobbleUtils.LOGGER_RAW.info(
        "Failed to register guild implementation: " + guildImpl.getClass().getName()
      );
    }
  }

  /**
   * Check if a guild plugin is registered.
   *
   * @return True if a guild plugin is registered, false otherwise.
   */
  public static boolean isGuildPluginRegistered() {
    return GUILD != null;
  }

  /**
   * Get the leader of the guild that the player is in.
   *
   * @param player The PlayerRef of the player.
   *
   * @return The UUID of the guild leader, or the player's UUID if the player is not in a guild.
   */
  public static UUID getLeader(ServerPlayerEntity player) {
    return getLeader(player.getUuid());
  }

  /**
   * Get the leader of the guild that the player is in.
   *
   * @param playerUUID The UUID of the player.
   *
   * @return The UUID of the guild leader, or the player's UUID if the player is not in a guild.
   */
  public static UUID getLeader(UUID playerUUID) {
    if (GUILD == null) return playerUUID;
    UUID leader = GUILD.getLeader(playerUUID);
    if (leader != null) return leader;
    return playerUUID;
  }

  @NonNull
  public static List<UUID> getMembers(ServerPlayerEntity player) {
    return getMembers(player.getUuid());
  }

  @NonNull
  public static List<UUID> getMembers(UUID playerUUID) {
    if (GUILD == null) return List.of(playerUUID);
    List<UUID> members = GUILD.getMembers(playerUUID);
    if (members != null) return members;
    return List.of(playerUUID);
  }

  public static void registerGuildProviders() {
  }
}
