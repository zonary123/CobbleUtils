package com.kingpixel.cobbleutils.mixins.events;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.events.models.EventEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Carlos Varas Alonso - 06/08/2025 21:39
 */
@Mixin(PlayerEntity.class)
public abstract class InteractEntityMixin {
  @Unique
  private final Cache<UUID, Long> cobbleQuests$cooldown = Caffeine.newBuilder()
    .expireAfterWrite(5, TimeUnit.SECONDS)
    .build();

  @Inject(method = "interact", at = @At("HEAD"))
  private void cobbleutils$interact(Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
    try {
      if (CobbleUtilsEvents.INTERACT_ENTITY_EVENT.isEmpty()) return;
      PlayerEntity playerEntity = (PlayerEntity) (Object) this;
      if (!(playerEntity instanceof ServerPlayerEntity player)) return;
      long now = System.currentTimeMillis();
      long last = cobbleQuests$cooldown.get(player.getUuid(), k -> 0L);

      if (now - last < 1000) return;

      cobbleQuests$cooldown.put(player.getUuid(), now);

      CobbleUtilsEvents.INTERACT_ENTITY_EVENT.emit(EventEntity.builder()
        .player(player)
        .entity(entity)
        .build());
    } catch (Throwable e) {
      CobbleUtils.LOGGER_RAW.error("Error in InteractEntityMixin#cobbleutils$interact", e);
    }
  }
}
