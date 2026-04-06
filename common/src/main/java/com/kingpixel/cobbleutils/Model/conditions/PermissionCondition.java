package com.kingpixel.cobbleutils.Model.conditions;

import com.kingpixel.cobbleutils.api.PermissionApi;
import lombok.*;
import net.minecraft.server.network.ServerPlayerEntity;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Builder
@Data
public class PermissionCondition extends Condition {
  public static final String TYPE = "PERMISSION";
  @Builder.Default
  private String permission = "";

  @Override
  public String getType() {
    return TYPE;
  }


  @Override
  public boolean check(ServerPlayerEntity player) {
    return PermissionApi.hasPermission(player, permission, 2);
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "You need the permission: " + permission;
  }


}
