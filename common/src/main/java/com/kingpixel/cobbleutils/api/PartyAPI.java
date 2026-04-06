package com.kingpixel.cobbleutils.api;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.party.Party;
import lombok.NonNull;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.UUID;

public class PartyAPI {
  private static Party PARTY;

  public static void registerPartyImplementation(Party partyImpl) {
    try {
      partyImpl.getLeader(UUID.randomUUID());
      PARTY = partyImpl;
      CobbleUtils.LOGGER_RAW.info(
        "Registered party implementation: " + partyImpl.getClass().getName()
      );
    } catch (NoClassDefFoundError | NoSuchMethodError | Exception e) {
      e.printStackTrace();
      CobbleUtils.LOGGER_RAW.info(
        "Failed to register party implementation: " + partyImpl.getClass().getName()
      );
    }
  }

  /**
   * Check if a party plugin is registered.
   *
   * @return True if a party plugin is registered, false otherwise.
   */
  public static boolean isPartyPluginRegistered() {
    return PARTY != null;
  }

  /**
   * Get the leader of the party that the player is in.
   *
   * @param player The PlayerRef of the player.
   * @return The UUID of the party leader, or the player's UUID if the player is not in a party.
   */
  public static UUID getLeader(ServerPlayerEntity player) {
    return getLeader(player.getUuid());
  }

  /**
   * Get the leader of the party that the player is in.
   *
   * @param playerUUID The UUID of the player.
   * @return The UUID of the party leader, or the player's UUID if the player is not in a party.
   */
  public static UUID getLeader(UUID playerUUID) {
    if (PARTY == null) return playerUUID;
    UUID leader = PARTY.getLeader(playerUUID);
    if (leader != null) return leader;
    return playerUUID;
  }

  @NonNull
  public static List<UUID> getMembers(ServerPlayerEntity player) {
    return getMembers(player.getUuid());
  }

  @NonNull
  public static List<UUID> getMembers(UUID playerUUID) {
    if (PARTY == null) return List.of(playerUUID);
    List<UUID> members = PARTY.getMembers(playerUUID);
    if (members != null) return members;
    return List.of(playerUUID);
  }

  public static void registerPartyProviders() {
  }
}
