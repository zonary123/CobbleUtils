package com.kingpixel.cobbleutils.model.conditions;

import com.kingpixel.cobbleutils.api.PermissionAPI;
import net.minecraft.server.network.ServerPlayerEntity;

public class PermissionCondition extends Condition {
  public static final String TYPE = "PERMISSION";
  private final String permission = "";

  @Override
  public String getType() {
    return TYPE;
  }


  @Override
  public boolean check(ServerPlayerEntity player) {
    return PermissionAPI.hasPermission(player, permission, 2);
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "You need the permission: " + permission;
  }


}
