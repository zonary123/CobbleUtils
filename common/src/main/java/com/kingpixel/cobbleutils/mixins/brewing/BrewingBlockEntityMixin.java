package com.kingpixel.cobbleutils.mixins.brewing;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.events.models.EventBrewing;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingBlockEntityMixin {
  @Inject(method = "craft", at = @At("HEAD"))
  private static void cobbleUtils$craft(World world, BlockPos pos, DefaultedList<ItemStack> slots, CallbackInfo ci) {
    BlockEntity blockEntity = world.getBlockEntity(pos);
    if (blockEntity == null) {
      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER.info(CobbleUtils.MOD_ID,
          "No block entity found in brewing stand at position: " + pos.toString());
      }
      return;
    }
    var componentMap = blockEntity.getComponents();
    var customData = componentMap.get(DataComponentTypes.CUSTOM_DATA);
    if (customData == null) {
      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER.info(CobbleUtils.MOD_ID,
          "No customData component found in brewing stand at position: " + pos.toString());
      }
      return;
    }
    var nbt = customData.getNbt();
    if (nbt == null || !nbt.contains("lastPlayer")) {
      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER.info(CobbleUtils.MOD_ID,
          "No lastPlayer data found in brewing stand at position: " + pos.toString());
      }
      return;
    }
    UUID playerUUID = nbt.getUuid("lastPlayer");
    if (playerUUID == null) {
      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER.info(CobbleUtils.MOD_ID,
          "Invalid lastPlayer UUID in brewing stand at position: " + pos.toString());
      }
      return;
    }
    ServerPlayerEntity player = CobbleUtils.server.getPlayerManager().getPlayer(playerUUID);
    if (player == null) {
      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER.info(CobbleUtils.MOD_ID,
          "No player found with UUID: " + playerUUID);
      }
      return;
    }
    CompletableFuture.runAsync(() -> {
      CobbleUtilsEvents.BREWING_EVENT.emit(new
        EventBrewing(player, world, pos, slots));
    }, CobbleUtils.EXECUTOR_COBBLEUTILS);
  }
}