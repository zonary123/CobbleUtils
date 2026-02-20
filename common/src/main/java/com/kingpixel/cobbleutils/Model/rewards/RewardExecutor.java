package com.kingpixel.cobbleutils.Model.rewards;

import lombok.NonNull;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.concurrent.CompletableFuture;

public interface RewardExecutor {
  CompletableFuture<Boolean> execute(@NonNull ServerPlayerEntity player, @NonNull Reward reward, @NonNull String data);
}
