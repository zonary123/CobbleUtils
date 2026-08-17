package com.kingpixel.cobbleutils.mixins.events;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import net.minecraft.entity.passive.CowEntity;
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
 * Mixin that intercepts cow milking interactions and emits a {@code MILKING_EVENT}.
 * <p>
 * A per-cow cooldown of 30 seconds is enforced using a Caffeine cache.
 * Entries expire automatically after 30 seconds, so dead or unloaded cows
 * do not remain in memory indefinitely.
 *
 * @author Carlos Varas Alonso - 27/12/2025 1:33
 */
@Mixin(CowEntity.class)
public abstract class MilkMixin {

  /**
   * Per-cow cooldown cache. Entries expire 30 seconds after write,
   * meaning each cow can only trigger the event once every 30 seconds.
   * Caffeine automatically evicts expired entries, keeping memory usage low
   * even when cows are unloaded or removed from the world.
   */
  @Unique
  private static final Cache<UUID, Boolean> cobbleUtils$COOLDOWN = Caffeine.newBuilder()
    .expireAfterWrite(30, TimeUnit.SECONDS)
    .maximumSize(500)
    .build();

  /**
   * Injected into {@link CowEntity#interactMob(PlayerEntity, Hand)} right before the milk bucket
   * is exchanged. Emits a {@code MILKING_EVENT} if:
   * <ul>
   *   <li>There are registered listeners for the event</li>
   *   <li>The interacting entity is a {@link ServerPlayerEntity}</li>
   *   <li>The cow is not on cooldown (30-second window)</li>
   * </ul>
   *
   * @param playerEntity the player interacting with the cow
   * @param hand         the hand used for the interaction
   * @param cir          mixin callback info
   */
  @Inject(
    method = "interactMob",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/item/ItemUsage;exchangeStack(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;"
    )
  )
  private void cobbleutils$onMilk(
    PlayerEntity playerEntity, Hand hand, CallbackInfoReturnable<ActionResult> cir
  ) {
    try {
      if (CobbleUtilsEvents.MILKING_EVENT.isEmpty()) return;
      if (!(playerEntity instanceof ServerPlayerEntity player)) return;

      UUID cowId = ((CowEntity) (Object) this).getUuid();
      if (cobbleUtils$COOLDOWN.getIfPresent(cowId) != null) return;
      cobbleUtils$COOLDOWN.put(cowId, Boolean.TRUE);

      CobbleUtilsEvents.MILKING_EVENT.emit(player);
    } catch (Throwable e) {
      CobbleUtils.LOGGER_RAW.error("Error in MilkMixin#cobbleutils$onMilk", e);
    }
  }
}