package com.kingpixel.cobbleutils.database.users;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.database.users.models.Storage;
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
 * User model for CobbleUtils.
 * <p>
 * Uses a dirty-tracking pattern: mutating methods mark the model as dirty.
 * Call {@link #isDirty()} before saving to skip unnecessary DB writes.
 * Call {@link #clearDirty()} after a successful save.
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
  private transient AtomicBoolean dirty = new AtomicBoolean(false);

  public AtomicBoolean getDirty() {
    if (dirty == null) dirty = new AtomicBoolean(false);
    return dirty;
  }

  public UserModel(ServerPlayerEntity player) {
    this.playerUUID = player.getUuid();
    connect(player);
  }

  public UserModel(UUID uuid, String playerName) {
    this.playerUUID = uuid;
    this.playerName = playerName;
    this.lastLogin = null;
    this.ip = null;
    init();
  }

  public UserModel(@NotNull UUID uuid) {
    this.playerUUID = uuid;
    this.playerName = null;
    this.lastLogin = null;
    this.ip = null;
    this.online = false;
    this.disconnectTime = null;
    init();
    getDirty().set(true);
  }

  /**
   * Ensures all collections are initialized and non-null.
   * Called from constructors that don't go through {@link #connect(ServerPlayerEntity)}.
   */
  private void init() {
    if (rewardsClaimed == null) rewardsClaimed = new HashMap<>();
    if (storageList == null) storageList = new HashSet<>();
  }

  public void markDirty() {
    getDirty().set(true);
  }

  /**
   * Atomically clears the dirty flag.
   *
   * @return true if the flag was dirty (and is now cleared), false if it was already clean.
   */
  public boolean clearDirty() {
    return getDirty().compareAndSet(true, false);
  }

  public boolean isDirty() {
    return getDirty().get();
  }

  public void connect(ServerPlayerEntity player) {
    this.playerName = player.getGameProfile().getName();
    this.lastLogin = Instant.now();
    this.ip = player.getIp();
    this.online = true;
    this.disconnectTime = null;
    init();
    getDirty().set(true);
  }

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
   * Pure CHECK — does NOT mutate state. Safe to call from filters/menus.
   */
  public boolean isAvailableReward(ItemChance itemChance) {
    String identifier = itemChance.getIdentifier();
    if (identifier == null) return true;
    int maxClaims = itemChance.getAmount() != null ? itemChance.getAmount() : 1;
    RewardInfo rewardInfo = rewardsClaimed.get(identifier);
    if (rewardInfo == null) return true;

    if (rewardInfo.getTimesClaimed() >= maxClaims) {
      if (itemChance.getCooldown() != null && !rewardInfo.isOnCooldown()) {
        return true;
      }
      if (itemChance.getCooldown() == null) {
        return false;
      }
      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER_RAW.info("Item '{}' on cooldown. Remaining: {}ms",
          identifier, rewardInfo.getRemainingCooldown());
      }
      return false;
    }

    return true;
  }

  /**
   * MUTATION — marks a reward as claimed. Marks dirty. Does NOT save to DB.
   */
  public void claimReward(ItemChance itemChance) {
    String identifier = itemChance.getIdentifier();
    if (identifier == null) return;
    int maxClaims = itemChance.getAmount() != null ? itemChance.getAmount() : 1;
    RewardInfo rewardInfo = rewardsClaimed.computeIfAbsent(identifier, k -> new RewardInfo());

    if (rewardInfo.getTimesClaimed() >= maxClaims && itemChance.getCooldown() != null && !rewardInfo.isOnCooldown()) {
      rewardInfo.reset();
    }

    rewardInfo.addTimesClaimed();

    if (rewardInfo.getTimesClaimed() >= maxClaims && itemChance.getCooldown() != null) {
      rewardInfo.setFinishCooldown(Instant.now().plus(itemChance.getCooldown().toMillis(), ChronoUnit.MILLIS));
    }
    getDirty().set(true);
  }

  /**
   * Repairs null/corrupt data after deserialization. Returns true if anything was fixed.
   */
  public boolean fix() {
    boolean changed = false;

    if (rewardsClaimed == null) {
      rewardsClaimed = new HashMap<>();
      changed = true;
    }
    if (storageList == null) {
      storageList = new HashSet<>();
      changed = true;
    }

    // Resolve playerName if missing (e.g. created via UserModel(UUID) for offline cross-server)
    if ((playerName == null || playerName.isBlank()) && playerUUID != null && CobbleUtils.server != null) {
      var userCache = CobbleUtils.server.getUserCache();
      if (userCache != null) {
        var profile = userCache.getByUuid(playerUUID);
        if (profile.isPresent()) {
          playerName = profile.get().getName();
          changed = true;
        }
      }
    }

    // Remove null entries and entries with null id (would cause NPE in removeStorage)
    if (storageList.removeIf(s -> s == null || s.getId() == null)) {
      changed = true;
    }

    Iterator<Map.Entry<String, RewardInfo>> it = rewardsClaimed.entrySet().iterator();
    Instant now = Instant.now();

    while (it.hasNext()) {
      Map.Entry<String, RewardInfo> entry = it.next();
      String key = entry.getKey();
      RewardInfo info = entry.getValue();

      // Remove entries with blank keys, null values, or negative timesClaimed
      if (key == null || key.isBlank() || info == null || info.getTimesClaimed() < 0) {
        it.remove();
        changed = true;
        continue;
      }

      // Remove expired cooldowns — no longer relevant
      if (info.getFinishCooldown() != null && now.isAfter(info.getFinishCooldown())) {
        it.remove();
        changed = true;
      }
    }

    if (changed) getDirty().set(true);
    return changed;
  }

  public void addStorage(Storage storage) {
    storageList.add(storage);
    getDirty().set(true);
  }

  public Storage removeStorage(UUID storageId) {
    Storage toRemove = null;
    for (Storage storage : storageList) {
      if (storage.getId().equals(storageId)) {
        toRemove = storage;
        break;
      }
    }
    if (toRemove != null) {
      storageList.remove(toRemove);
      getDirty().set(true);
    }
    return toRemove;
  }

  public void disconnect() {
    this.online = false;
    this.disconnectTime = Instant.now();
    getDirty().set(true);
  }

  public void addStorage(List<Storage> storage) {
    if (storage != null && !storage.isEmpty()) {
      storageList.addAll(storage);
      getDirty().set(true);
    }
  }

  @Data
  static class RewardInfo {
    private int timesClaimed;
    private Instant finishCooldown;

    public RewardInfo() {
      this.timesClaimed = 0;
      this.finishCooldown = null;
    }

    public void addTimesClaimed() {
      timesClaimed++;
    }

    public void reset() {
      this.timesClaimed = 0;
      this.finishCooldown = null;
    }

    public boolean isOnCooldown() {
      if (finishCooldown == null) return false;
      return Instant.now().isBefore(finishCooldown);
    }

    public long getRemainingCooldown() {
      if (finishCooldown == null) return 0;
      long remaining = Duration.between(Instant.now(), finishCooldown).toMillis();
      return Math.max(0, remaining);
    }

  }
}
