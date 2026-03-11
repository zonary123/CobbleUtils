package com.kingpixel.cobbleutils.model.conditions;

import com.kingpixel.cobbleutils.CobbleUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
public class ServerCondition extends Condition {
  public static final String TYPE = "SERVER";
  private final Set<String> servers = Set.of("lunarclient");

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    return CobbleUtils.getServerName() != null && servers.contains(CobbleUtils.getServerName());
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "You need to be on one of the following servers: " + String.join(", ", servers);
  }


}
