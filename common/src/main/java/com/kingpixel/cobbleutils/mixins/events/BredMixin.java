package com.kingpixel.cobbleutils.mixins.events;

import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.events.models.EventEntity;
import net.minecraft.advancement.criterion.BredAnimalsCriterion;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Carlos Varas Alonso - 06/08/2025 21:39
 */
@Mixin(BredAnimalsCriterion.class)
public abstract class BredMixin {
  @Inject(method = "trigger", at = @At("HEAD"))
  private void Bred(ServerPlayerEntity player, AnimalEntity parent, AnimalEntity partner, PassiveEntity child,
                    CallbackInfo ci) {
    if (CobbleUtilsEvents.BRED_EVENT.isEmpty()) return;
    if (parent == null || partner == null || child == null) return;
    if (player == null) return;

    CobbleUtilsEvents.BRED_EVENT.emit(EventEntity.builder()
      .player(player)
      .entity(child)
      .build());
  }
}
