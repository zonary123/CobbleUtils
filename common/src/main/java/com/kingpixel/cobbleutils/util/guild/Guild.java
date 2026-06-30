package com.kingpixel.cobbleutils.util.guild;

import java.util.List;
import java.util.UUID;

/**
 *
 * @author Carlos Varas Alonso - 26/06/2026 16:38
 */
public abstract class Guild {
  /**
   * Get the leader of the guild that the player is in.
   *
   * @param playerUUID The UUID of the player.
   *
   * @return The UUID of the guild leader, or null if the player is not in a guild.
   */
  public abstract UUID getLeader(UUID playerUUID);

  /**
   * Get the members of the guild that the player is in.
   *
   * @param playerUUID The UUID of the player.
   *
   * @return A list of UUIDs of the guild members, or an empty list if the player is not in a guild.
   */
  public abstract List<UUID> getMembers(UUID playerUUID);
}
