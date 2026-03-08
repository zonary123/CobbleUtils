package com.kingpixel.cobbleutils.database.users;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DurationValue;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.Model.rewards.Reward;
import com.kingpixel.cobbleutils.database.DataBaseFactory;
import com.kingpixel.cobbleutils.database.users.models.Storage;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.UtilsFile;
import lombok.Data;
import net.minecraft.server.network.ServerPlayerEntity;
import org.bson.Document;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

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

  public UserModel(ServerPlayerEntity player) {
    var gameProfile = player.getGameProfile();
    this.playerName = gameProfile.getName();
    this.playerUUID = gameProfile.getId();
    connect(player);
  }

  public UserModel() {
  }

  public static @Nullable UserModel fromDocument(Document doc) {
    return UtilsFile.getGson().fromJson(doc.toJson(), UserModel.class);
  }

  public void connect(ServerPlayerEntity player) {
    var gameProfile = player.getGameProfile();
    this.playerUUID = gameProfile.getId();
    this.playerName = gameProfile.getName();
    this.lastLogin = Instant.now();
    this.ip = player.getIp();
    this.online = true;
    this.disconnectTime = null;
    if (rewardsClaimed == null) rewardsClaimed = new HashMap<>();
    if (storageList == null) storageList = new HashSet<>();
    if (dirty == null) dirty = new AtomicBoolean(false);
    markDirty();
  }

  public void markDirty() {
    if (dirty == null) dirty = new AtomicBoolean(false);
    dirty.set(true);
  }

  public CompletableFuture<Void> save() {
    if (dirty.getAndSet(false)) return DataBaseFactory.dataBaseUsers.saveUserModel(this);
    return CompletableFuture.completedFuture(null);
  }

  public String getUserInfo() {
    DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)
      .withLocale(Locale.getDefault())
      .withZone(ZoneId.systemDefault());
    String formattedLastLogin = lastLogin != null ? formatter.format(lastLogin) : "never";
    return String.format("User Info:%n - Name: %s%n - UUID: %s%n - Last Login: %s%n - IP: %s",
      playerName, playerUUID, formattedLastLogin, ip);
  }

  public boolean isAvailableReward(Reward reward) {
    if (reward == null) return false;
    if (Boolean.FALSE.equals(reward.getUnique()) || reward.getIdentifier() == null) return true;

    String identifier = reward.getIdentifier();
    int maxClaims = reward.getAmount();
    RewardInfo rewardInfo = rewardsClaimed.computeIfAbsent(identifier, k -> new RewardInfo());

    if (rewardInfo.getTimesClaimed() >= maxClaims && reward.getCooldown() != null) {
      if (rewardInfo.isOnCooldown(reward)) {
        CobbleUtils.LOGGER.info("cobbleutils", "Reward on cooldown. Remaining time: " +
          PlayerUtils.getCooldown(rewardInfo.getRemainingCooldown(reward)) + " seconds");
        return false;
      } else {
        rewardInfo.reset();
      }
    }

    rewardInfo.addTimesClaimed();

    if (rewardInfo.getTimesClaimed() == maxClaims && reward.getCooldown() != null) {
      rewardInfo.setFinishCooldown(Instant.now().plus(reward.getCooldown().toMillis(), ChronoUnit.MILLIS));
    }

    markDirty();
    return true;
  }

  public boolean isAvailableReward(ItemChance itemChance) {
    if (itemChance == null) return false;
    return isAvailableReward(itemChance.toReward());
  }

  public void fix(ServerPlayerEntity player) {
    var gameProfile = player.getGameProfile();
    if (playerUUID == null || !playerUUID.equals(gameProfile.getId())) {
      this.playerUUID = player.getUuid();
      markDirty();
    }
    if (playerName == null || !playerName.equals(gameProfile.getName())) {
      this.playerName = gameProfile.getName();
      markDirty();
    }
    if (rewardsClaimed == null) {
      rewardsClaimed = new HashMap<>();
      markDirty();
    }
    if (storageList == null) {
      storageList = new HashSet<>();
      markDirty();
    }
    storageList.removeIf(Objects::isNull);
    Instant now = Instant.now();
    rewardsClaimed.entrySet().removeIf(entry -> {
      RewardInfo info = entry.getValue();
      return info == null || (info.getFinishCooldown() != null && now.isAfter(info.getFinishCooldown()));
    });
  }

  public void addStorage(Storage storage) {
    storageList.add(storage);
    markDirty();
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
      markDirty();
    }
    return toRemove;
  }

  public void addStorage(List<Storage> storage) {
    storageList.addAll(storage);
    markDirty();
  }

  public void disconnect() {
    this.online = false;
    this.disconnectTime = Instant.now();
    markDirty();
  }

  public Document toDocument() {
    Document document = Document.parse(UtilsFile.getGson().toJson(this, UserModel.class));
    document.remove("storageList");
    document.remove("rewardsClaimed");
    return document;
  }

  @Data
  private static class RewardInfo {
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

    public boolean isOnCooldown(Reward reward) {
      DurationValue cooldown = reward.getCooldown();
      if (finishCooldown == null || cooldown == null) return false;
      if (cooldown.toMillis() <= 0) return true;
      Instant nextAvailable = finishCooldown.plusMillis(cooldown.toMillis());
      return Instant.now().isBefore(nextAvailable);
    }

    public long getRemainingCooldown(Reward reward) {
      DurationValue cooldown = reward.getCooldown();
      if (finishCooldown == null || cooldown == null) return 0;
      if (cooldown.toMillis() <= 0) return Long.MAX_VALUE;
      Instant nextAvailable = finishCooldown.plusMillis(cooldown.toMillis());
      return Math.max(0, Duration.between(Instant.now(), nextAvailable).toMillis());
    }
  }
}