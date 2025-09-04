package com.kingpixel.cobbleutils.database.users;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DurationValue;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.database.DataBaseFactory;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import lombok.Data;
import net.minecraft.server.network.ServerPlayerEntity;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * User model for CobbleUtils
 * Handles player info, last login, and claimed rewards.
 */
@Data
public class UserModel {

  private UUID playerUUID;
  private String playerName;
  private Instant lastLogin;
  private String ip;

  // Map: ItemChance UUID -> RewardInfo
  private Map<String, RewardInfo> rewardsClaimed = new HashMap<>();

  public UserModel(ServerPlayerEntity player) {
    this.playerUUID = player.getUuid();
    updateData(player);
  }

  public void updateData(ServerPlayerEntity player) {
    this.playerName = player.getGameProfile().getName();
    this.lastLogin = Instant.now();
    this.ip = player.getIp();
    if (rewardsClaimed == null) rewardsClaimed = new HashMap<>();
  }

  /**
   * Returns a human-readable summary of the user.
   */
  public String getUserInfo() {
    DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)
      .withLocale(Locale.getDefault())
      .withZone(ZoneId.systemDefault());

    String formattedLastLogin = lastLogin != null
      ? formatter.format(lastLogin)
      : "never";

    return String.format("User Info:%n - Name: %s%n - UUID: %s%n - Last Login: %s%n - IP: %s",
      playerName, playerUUID, formattedLastLogin, ip);
  }

  /**
   * Checks if an ItemChance reward is available for the user.
   */
  public boolean isAvailableReward(ItemChance itemChance) {
    String identifier = itemChance.getIdentifier();
    if (identifier == null) return true; // Unlimited item
    int maxClaims = itemChance.getAmount();
    RewardInfo rewardInfo = rewardsClaimed.computeIfAbsent(identifier, k -> new RewardInfo());

    // Cooldown check
    if (rewardInfo.getTimesClaimed() >= maxClaims && itemChance.getCooldown() != null) {
      if (rewardInfo.isOnCooldown(itemChance)) {
        CobbleUtils.LOGGER.info("Item on cooldown. Remaining time: " +
          PlayerUtils.getCooldown(rewardInfo.getRemainingCooldown(itemChance)) + " seconds"
        );
        return false;
      } else {
        rewardInfo.reset();
      }
    }

    rewardInfo.addTimesClaimed();

    // Set cooldown if max reached
    if (rewardInfo.getTimesClaimed() == maxClaims && itemChance.getCooldown() != null) {
      rewardInfo.setLastClaimed(Instant.now());
    }

    DataBaseFactory.dataBaseUsers.saveOrUpdateUser(this);
    return true;
  }

  /**
   * Stores information about claimed rewards for a user.
   */
  @Data
  private static class RewardInfo {
    private int timesClaimed;
    private Instant lastClaimed;

    public RewardInfo() {
      this.timesClaimed = 0;
      this.lastClaimed = null; // Start null to avoid premature cooldown
    }

    public void addTimesClaimed() {
      timesClaimed++;
    }

    public void reset() {
      this.timesClaimed = 0;
      this.lastClaimed = null;
    }

    public boolean isOnCooldown(ItemChance itemChance) {
      DurationValue cooldown = itemChance.getCooldown();
      // Si cooldown es null o negativo, consideramos que es infinito
      if (lastClaimed == null || cooldown == null) return false;
      if (cooldown.toMillis() <= 0) return true; // Cooldown infinito
      Instant nextAvailable = lastClaimed.plus(cooldown.toMillis(), ChronoUnit.MILLIS);
      return Instant.now().isBefore(nextAvailable);
    }

    public long getRemainingCooldown(ItemChance itemChance) {
      DurationValue cooldown = itemChance.getCooldown();
      if (lastClaimed == null || cooldown == null) return 0;
      if (cooldown.toMillis() <= 0) return Long.MAX_VALUE; // Representa cooldown infinito
      Instant nextAvailable = lastClaimed.plus(cooldown.toMillis(), ChronoUnit.MILLIS);
      long remaining = nextAvailable.getEpochSecond() - Instant.now().getEpochSecond();
      return Math.max(0, remaining);
    }

  }
}
