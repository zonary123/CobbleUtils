package com.kingpixel.cobbleutils.Model.conditions;

import lombok.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;

import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Builder
@Data
public class GameModeCondition extends Condition {
  public static final String TYPE = "GAME_MODE";
  @Builder.Default
  private Set<String> gameModes = Set.of("survival");

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean check(ServerPlayerEntity player) {
    GameMode current = player.interactionManager.getGameMode();
    return gameModes.stream()
      .anyMatch(gm -> current.getName().equalsIgnoreCase(gm));
  }

  @Override
  public String getReason(ServerPlayerEntity player) {
    return "You need to be in one of the following game modes: " + String.join(", ", gameModes);
  }
}

