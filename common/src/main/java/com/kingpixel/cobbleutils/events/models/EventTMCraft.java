package com.kingpixel.cobbleutils.events.models;

import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.tms.TechnicalMachine;
import com.cobblemon.mod.common.block.entity.TMMachineBlockEntity;
import lombok.Builder;
import lombok.Data;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

@Data
@Builder
public class EventTMCraft {
  private ServerPlayerEntity player;
  private ItemStack itemStack;
  private MoveTemplate move;
  private TechnicalMachine tm;
  private TMMachineBlockEntity blockEntity;
  private World world;
  private BlockPos pos;

  public List<ItemStack> getItemStacks() {
    if (itemStack == null) return List.of();
    return List.of(itemStack);
  }
}
