package com.kingpixel.cobbleutils.Model.conditions;

import lombok.Data;
import net.minecraft.server.network.ServerPlayerEntity;

@Data
public abstract class Condition {

  public abstract String getType();


  public abstract boolean check(ServerPlayerEntity player);

  public abstract String getReason(ServerPlayerEntity player);
}
