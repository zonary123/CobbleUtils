package com.kingpixel.cobbleutils.mixins;

import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.tms.TechnicalMachine;
import com.cobblemon.mod.common.api.tms.TechnicalMachines;
import com.cobblemon.mod.common.block.entity.TMMachineBlockEntity;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.events.models.EventTMCraft;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 06/09/2026 10:42
 */
@Mixin(TMMachineBlockEntity.class)
public abstract class TMMachineBlockEntityMixin extends BlockEntity {

  protected TMMachineBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  @Shadow(remap = false)
  public abstract String getActiveMove();

  @Unique
  private UUID cobbleutils$lastPlayerUuid;

  @Inject(method = "createScreenHandler", at = @At("HEAD"))
  private void cobbleutils$onCreateScreenHandler(int syncId, PlayerInventory playerInventory, CallbackInfoReturnable<ScreenHandler> cir) {
    if (playerInventory.player instanceof ServerPlayerEntity serverPlayer) {
      this.cobbleutils$lastPlayerUuid = serverPlayer.getUuid();
    }
  }

  @Inject(
    method = "craftTM",
    at = @At(
      value = "INVOKE",
      target = "Lcom/cobblemon/mod/common/block/entity/TMMachineBlockEntity;playSound(Lnet/minecraft/sound/SoundEvent;FF)V"
    ),
    remap = false
  )
  private void cobbleutils$onCraftTM(CallbackInfo ci) {
    try {
      if (CobbleUtilsEvents.TM_CRAFT_EVENT.isEmpty()) return;
      if (world == null || world.isClient()) return;

      String activeMove = getActiveMove();
      if (activeMove == null || activeMove.isEmpty()) return;

      MoveTemplate move = Moves.getByName(activeMove);
      if (move == null) return;

      TechnicalMachine tm = TechnicalMachines.INSTANCE.getMoveToTM().get(move);
      if (tm == null) return;

      ItemStack craftedStack = tm.createItemStack();

      ServerPlayerEntity player = null;
      if (world instanceof ServerWorld serverWorld) {
        if (cobbleutils$lastPlayerUuid != null) {
          player = serverWorld.getServer().getPlayerManager().getPlayer(cobbleutils$lastPlayerUuid);
        }
        if (player == null) {
          player = (ServerPlayerEntity) serverWorld.getClosestPlayer(
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12.0, false
          );
        }
      }

      TMMachineBlockEntity tmMachine = (TMMachineBlockEntity) (Object) this;

      CobbleUtilsEvents.TM_CRAFT_EVENT.emit(EventTMCraft.builder()
        .player(player)
        .itemStack(craftedStack)
        .move(move)
        .tm(tm)
        .blockEntity(tmMachine)
        .world(world)
        .pos(pos)
        .build());
    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.error("Error in TMMachineBlockEntityMixin#cobbleutils$onCraftTM", e);
    }
  }
}
