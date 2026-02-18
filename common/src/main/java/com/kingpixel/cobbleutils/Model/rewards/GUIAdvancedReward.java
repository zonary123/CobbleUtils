package com.kingpixel.cobbleutils.Model.rewards;

import ca.landonjw.gooeylibs2.api.button.ButtonAction;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

@Data
@Builder
public class GUIAdvancedReward {
  @NonNull
  private ServerPlayerEntity player;
  @NonNull
  private Consumer<ButtonAction> closeAction;
  @Nullable
  private Consumer<ChestTemplate> template;
}
