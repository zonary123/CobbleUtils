package com.kingpixel.cobbleutils.model.zones.zoneshapes;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class CompositeShape extends ZoneShape {
  public static final String TYPE = "COMPOSITE";
  private final List<ZoneShape> shapes;

  public CompositeShape() {
    super(TYPE);
    this.shapes = List.of(new CuboidShape(), new PolygonShape());
  }

  public CompositeShape(List<ZoneShape> shapes) {
    super(TYPE);
    this.shapes = shapes;
  }

  @Override
  public void fix() {
    for (ZoneShape shape : shapes) {
      shape.fix();
    }
  }

  @Override
  public boolean contains(BlockPos pos) {

    for (ZoneShape shape : shapes) {
      if (shape.contains(pos)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void spawnParticles(ServerWorld world, @Nullable ServerPlayerEntity player) {
    for (ZoneShape shape : shapes) {
      shape.spawnParticles(world, player);
    }
  }
}