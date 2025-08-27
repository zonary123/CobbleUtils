package com.kingpixel.cobbleutils.command.suggests;

/**
 * @author Carlos Varas Alonso - 27/08/2025 15:05
 */
public class CobbleUtilsSuggests {
  public static final PlayerOfflineAndOnline SUGGESTS_PLAYER_OFFLINE_AND_ONLINE = new PlayerOfflineAndOnline();

  /**
   * Method to reset all suggests, useful for testing purposes
   */
  public static void reset() {
    SUGGESTS_PLAYER_OFFLINE_AND_ONLINE.reset();
  }
}
