package com.kingpixel.cobbleutils.Model.conditions;

import com.kingpixel.cobbleutils.CobbleUtils;
import lombok.*;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Builder
@Data
public class StructureCondition extends Condition {

  public static final String TYPE = "STRUCTURE";

  private Set<String> structures = new HashSet<>(Set.of(
    "minecraft:village",
    "minecraft:pillager_outpost",
    "minecraft:stronghold"
  ));


  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    CobbleUtils.LOGGER.info("Not implemented yet, returning true for now");
    return true;
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "You need to be in one of the following structures: "
      + String.join(", ", structures);
  }
}