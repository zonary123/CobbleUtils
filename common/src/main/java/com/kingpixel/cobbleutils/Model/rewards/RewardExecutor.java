package com.kingpixel.cobbleutils.Model.rewards;

import lombok.NonNull;
import net.minecraft.server.network.ServerPlayerEntity;

public interface RewardExecutor {
  void execute(@NonNull ServerPlayerEntity player, @NonNull Reward reward, @NonNull String data);
}
