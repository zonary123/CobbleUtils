package com.kingpixel.cobbleutils.mixins.brewing;

import net.minecraft.block.BlockState;
import net.minecraft.block.BrewingStandBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrewingStandBlock.class)

public abstract class BrewingBlockMixin {
  @Inject(method = "onUse", at = @At("HEAD"))
  private void cobbleQuests$onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
    if (cir.getReturnValue() != null && cir.getReturnValue().equals(ActionResult.FAIL)) return;
    BlockEntity blockEntity = world.getBlockEntity(pos);
    if (blockEntity == null) return;
    NbtCompound nbtCompound = new NbtCompound();
    nbtCompound.putUuid("lastPlayer", player.getUuid());
    ComponentMap base = blockEntity.getComponents();
    ComponentMap overrides = ComponentMap.builder()
      .add(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbtCompound))
      .build();

    blockEntity.setComponents(ComponentMap.of(base, overrides));
    blockEntity.markDirty();
  }
}