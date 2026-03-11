package com.kingpixel.cobbleutils.database.users;

import lombok.Data;
import lombok.ToString;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
@ToString(callSuper = true)
public abstract class UserAbstract {

  public static final String KEY_PLAYER_UUID = "playerUUID";
  public static final String KEY_USERNAME = "username";

  protected UUID playerUUID;
  protected String username;

  protected transient AtomicBoolean dirty = new AtomicBoolean(false);

  public UserAbstract(ServerPlayerEntity player) {
    var profile = player.getGameProfile();
    this.username = profile.getName();
    this.playerUUID = profile.getId();
    markDirty();
  }

  private AtomicBoolean dirty() {
    if (dirty == null) {
      dirty = new AtomicBoolean(false);
    }
    return dirty;
  }

  public boolean isDirty() {
    return dirty().get();
  }

  public void setDirty(boolean value) {
    dirty().set(value);
  }

  public void markDirty() {
    dirty().set(true);
  }

  public void clearDirty() {
    dirty().set(false);
  }

  public void fix(ServerPlayerEntity player) {
    var profile = player.getGameProfile();

    if (playerUUID == null) {
      playerUUID = profile.getId();
      markDirty();
    }

    String name = profile.getName();
    if (username == null || !username.equals(name)) {
      username = name;
      markDirty();
    }
  }
}