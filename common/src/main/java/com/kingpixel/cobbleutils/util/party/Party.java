package com.kingpixel.cobbleutils.util.party;

import java.util.List;
import java.util.UUID;

public abstract class Party {
  /**
   * Get the leader of the party that the player is in.
   *
   * @param playerUUID The UUID of the player.
   * @return The UUID of the party leader, or null if the player is not in a party.
   */
  public abstract UUID getLeader(UUID playerUUID);

  /**
   * Get the members of the party that the player is in.
   *
   * @param playerUUID The UUID of the player.
   * @return A list of UUIDs of the party members, or an empty list if the player is not in a party.
   */
  public abstract List<UUID> getMembers(UUID playerUUID);


}
