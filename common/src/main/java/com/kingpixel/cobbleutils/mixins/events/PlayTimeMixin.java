package com.kingpixel.cobbleutils.mixins.events;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Mixin(ServerPlayerEntity.class)
public abstract class PlayTimeMixin {

  @Unique
  private static final Cache<UUID, Integer> PLAY_TIME_CACHE = Caffeine.newBuilder()
    .expireAfterAccess(30, TimeUnit.SECONDS)
    .build();

  @Inject(method = "tick", at = @At("HEAD"))
  private void playTime(CallbackInfo ci) {
    try {
      if (CobbleUtilsEvents.PLAY_TIME_EVENT.isEmpty()) return;

      ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
      if (player == null) return;

      UUID uuid = player.getUuid();

      int ticks = player.age;

      int seconds = ticks / 20;

      Integer lastSentSecond = PLAY_TIME_CACHE.getIfPresent(uuid);

      if (lastSentSecond == null || lastSentSecond != seconds) {
        PLAY_TIME_CACHE.put(uuid, seconds);

        CobbleUtilsEvents.PLAY_TIME_EVENT.emit(player);
      }
    } catch (Throwable e) {
      CobbleUtils.LOGGER_RAW.error("Error in PlayTimeMixin#playTime", e);
    }
  }
}
