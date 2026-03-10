package com.kingpixel.cobbleutils.mixins.events;

import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.events.models.EventItemStack;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Carlos Varas Alonso - 06/08/2025 21:39
 */
@Mixin(LivingEntity.class)
public abstract class EatMixin {

  @Inject(method = "eatFood", at = @At("HEAD"))
  private void eatFood(World world, ItemStack stack, FoodComponent foodComponent, CallbackInfoReturnable<ItemStack> cir) {
    if (CobbleUtilsEvents.EATING_EVENT.isEmpty()) return;
    LivingEntity livingEntity = (LivingEntity) (Object) this;
    if (!(livingEntity instanceof ServerPlayerEntity)) return;
    ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
    ItemStack finalStack = stack.copy();

    CobbleUtilsEvents.EATING_EVENT.emit(EventItemStack.builder()
      .player(player)
      .itemStack(finalStack)
      .build());
  }
}
