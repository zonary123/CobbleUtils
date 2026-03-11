package com.kingpixel.cobbleutils.model.conditions;

import com.kingpixel.cobbleutils.CobbleUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class MolangCondition extends Condition {
  public static final String TYPE = "MOLANG";
  private List<String> expression = List.of(
    ""
  );

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    CobbleUtils.LOGGER.info("Not Implemented: MolangCondition");
    return true;
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "";
  }

}
