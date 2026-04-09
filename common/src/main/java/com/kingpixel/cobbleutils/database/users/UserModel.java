package com.kingpixel.cobbleutils.database.users;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DurationValue;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.database.DataBaseFactory;
import com.kingpixel.cobbleutils.database.users.models.Storage;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import lombok.Data;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * User model for CobbleUtils
 * Handles player info, last login, and claimed rewards.
 */
@Data
public class UserModel {
  private boolean online;
  private UUID playerUUID;
  private String playerName;
  private Instant lastLogin;
  private Instant disconnectTime;
  private String ip;
  private Map<String, RewardInfo> rewardsClaimed = new HashMap<>();
  private Set<Storage> storageList = new HashSet<>();
  private transient boolean dirty = false;

  /**
   * Constructor for an online player.
   *
   * @param player The online player entity.
   */
  public UserModel(ServerPlayerEntity player) {
    this.playerUUID = player.getUuid();
    connect(player);
  }

  /**
   * Constructor for a user with basic info.
   *
   * @param uuid       The UUID of the player.
   * @param playerName The name of the player.
   */
  public UserModel(UUID uuid, String playerName) {
    this.playerUUID = uuid;
    this.playerName = playerName;
    this.lastLogin = null;
    this.ip = null;
  }

  /**
   * Constructor for a new user with default collections.
   *
   * @param uuid The UUID of the player.
   */
  public UserModel(@NotNull UUID uuid) {
    this.playerUUID = uuid;
    this.lastLogin = null;
    this.ip = null;
    this.online = false;
    this.disconnectTime = null;
    this.storageList = new HashSet<>();
    this.rewardsClaimed = new HashMap<>();
  }

  /**
   * Update user info upon connection.
   *
   * @param player The online player entity.
   */
  public void connect(ServerPlayerEntity player) {
    this.playerName = player.getGameProfile().getName();
    this.lastLogin = Instant.now();
    this.ip = player.getIp();
    this.online = true;
    this.disconnectTime = null;
    if (rewardsClaimed == null) rewardsClaimed = new HashMap<>();
    if (storageList == null) storageList = new HashSet<>();
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

    if (rewardInfo.getTimesClaimed() >= maxClaims && itemChance.getCooldown() != null) {
      if (rewardInfo.isOnCooldown(itemChance)) {
        CobbleUtils.LOGGER_RAW.info("Item on cooldown. Remaining time: " +
          PlayerUtils.getCooldown(rewardInfo.getRemainingCooldown(itemChance)) + " seconds"
        );
        return false;
      } else {
        rewardInfo.reset();
      }
    }

    rewardInfo.addTimesClaimed();

    if (rewardInfo.getTimesClaimed() == maxClaims && itemChance.getCooldown() != null) {
      rewardInfo.setFinishCooldown(Instant.now().plus(itemChance.getCooldown().toMillis(), ChronoUnit.MILLIS));
    }
    DataBaseFactory.dataBaseUsers.saveOrUpdateUser(this);
    return true;
  }

  /**
   * Sanitizes the user model by initializing null collections and removing invalid entries.
   *
   * @return true if changes were made.
   */
  public boolean fix() {
    AtomicBoolean changed = new AtomicBoolean(false);
    if (rewardsClaimed == null) {
      rewardsClaimed = new HashMap<>();
      changed.set(true);
      if (CobbleUtils.config.isDebug())
        CobbleUtils.LOGGER_RAW.info("Fixed null rewardsClaimed for user: " + playerName);
    }
    if (storageList == null) {
      storageList = new HashSet<>();
      changed.set(true);
      if (CobbleUtils.config.isDebug())
        CobbleUtils.LOGGER_RAW.info("Fixed null storageList for user: " + playerName);
    }
    storageList.stream().filter(Objects::isNull).toList().forEach(storage -> {
      storageList.remove(storage);
      changed.set(true);
      if (CobbleUtils.config.isDebug())
        CobbleUtils.LOGGER_RAW.info("Removed null storage entry for user: " + playerName);
    });
    Iterator<Map.Entry<String, RewardInfo>> it = rewardsClaimed.entrySet().iterator();

    Instant now = Instant.now();

    while (it.hasNext()) {
      Map.Entry<String, RewardInfo> entry = it.next();
      RewardInfo info = entry.getValue();
      if (info == null) {
        it.remove();
        changed.set(true);
        if (CobbleUtils.config.isDebug())
          CobbleUtils.LOGGER_RAW.info("Removed null RewardInfo for user: " + playerName);
        continue;
      }
      if (info.getFinishCooldown() != null && now.isAfter(info.getFinishCooldown())) {
        it.remove();
        changed.set(true);
      }
    }
    return changed.get();
  }

  /**
   * Add a storage to the user.
   *
   * @param storage The storage to add.
   */
  public void addStorage(Storage storage) {
    storageList.add(storage);
  }

  /**
   * Remove a storage by ID.
   *
   * @param storageId The ID of the storage to remove.
   * @return The removed storage or null if not found.
   */
  public Storage removeStorage(UUID storageId) {
    Storage toRemove = null;
    for (Storage storage : storageList) {
      if (storage.getId().equals(storageId)) {
        toRemove = storage;
        break;
      }
    }
    if (toRemove != null) storageList.remove(toRemove);
    return toRemove;
  }

  /**
   * Update the user state for disconnection.
   */
  public void disconnect() {
    this.online = false;
    this.disconnectTime = Instant.now();
  }

  /**
   * Add multiple storages to the user.
   *
   * @param storage The list of storages to add.
   */
  public void addStorage(List<Storage> storage) {
    storageList.addAll(storage);
  }


  /**
   * Stores information about claimed rewards for a user.
   */
  @Data
  private static class RewardInfo {
    private int timesClaimed;
    private Instant finishCooldown;

    public RewardInfo() {
      this.timesClaimed = 0;
      this.finishCooldown = null; // Start null to avoid premature cooldown
    }

    public void addTimesClaimed() {
      timesClaimed++;
    }

    public void reset() {
      this.timesClaimed = 0;
      this.finishCooldown = null;
    }

    public boolean isOnCooldown(ItemChance itemChance) {
      DurationValue cooldown = itemChance.getCooldown();
      if (finishCooldown == null || cooldown == null) return false;
      if (cooldown.toMillis() <= 0) return true;
      Instant nextAvailable = finishCooldown.plusMillis(cooldown.toMillis());
      return Instant.now().isBefore(nextAvailable);
    }

    public long getRemainingCooldown(ItemChance itemChance) {
      DurationValue cooldown = itemChance.getCooldown();
      if (finishCooldown == null || cooldown == null) return 0;
      if (cooldown.toMillis() <= 0) return Long.MAX_VALUE; // Cooldown infinito

      Instant nextAvailable = finishCooldown.plusMillis(cooldown.toMillis());
      long remaining = Duration.between(Instant.now(), nextAvailable).toMillis();

      return Math.max(0, remaining);
    }


  }
}
