package com.kingpixel.cobbleutils.mixins.events;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.events.models.EventItemStack;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(SheepEntity.class)
public abstract class SheepShearMixin {

  @Unique
  private ServerPlayerEntity cobbleutils$lastShearer;

  @Shadow
  @Final
  private static Map<DyeColor, ItemConvertible> DROPS;

  @Shadow
  public abstract DyeColor getColor();

  @Inject(method = "interactMob", at = @At("HEAD"))
  private void cobblejobs$capturePlayer(
    PlayerEntity player,
    Hand hand,
    CallbackInfoReturnable<ActionResult> cir
  ) {
    try {
      if (player instanceof ServerPlayerEntity sp) {
        this.cobbleutils$lastShearer = sp;
      }
    } catch (Throwable e) {
      CobbleUtils.LOGGER_RAW.error("Error in SheepShearMixin#cobblejobs$capturePlayer", e);
    }
  }

  @Inject(
    method = "sheared",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/entity/passive/SheepEntity;dropItem(Lnet/minecraft/item/ItemConvertible;I)Lnet/minecraft/entity/ItemEntity;",
      shift = At.Shift.AFTER
    )
  )
  private void cobbleUtils$onSheared(SoundCategory shearedSoundCategory, CallbackInfo ci) {
    try {
      if (CobbleUtilsEvents.SHEEP_SHEAR_EVENT.isEmpty()) return;
      if (this.cobbleutils$lastShearer != null) {
        CobbleUtilsEvents.SHEEP_SHEAR_EVENT.emit(EventItemStack.builder()
          .player(cobbleutils$lastShearer)
          .itemStack(new ItemStack(DROPS.get(this.getColor())))
          .build());
        this.cobbleutils$lastShearer = null;
      }
    } catch (Throwable e) {
      CobbleUtils.LOGGER_RAW.error("Error in SheepShearMixin#cobbleUtils$onSheared", e);
    }
  }

}
